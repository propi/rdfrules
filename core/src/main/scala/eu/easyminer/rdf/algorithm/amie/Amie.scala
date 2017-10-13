package eu.easyminer.rdf.algorithm.amie

import com.typesafe.scalalogging.Logger
import eu.easyminer.rdf.index.TripleHashIndex
import eu.easyminer.rdf.rule.ExtendedRule.{ClosedRule, DanglingRule}
import eu.easyminer.rdf.rule.RuleConstraint.{OnlyPredicates, WithoutPredicates}
import eu.easyminer.rdf.rule._
import eu.easyminer.rdf.utils.HowLong._
import eu.easyminer.rdf.utils.{Debugger, FilteredSetView, HashQueue}

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
class Amie private(thresholds: Threshold.Thresholds, rulePatterns: List[RulePattern], constraints: List[RuleConstraint])(implicit debugger: Debugger) {

  private val logger = Logger[Amie]

  lazy val minSupport: Int = thresholds[Threshold.MinSupport].value

  def addThreshold(threshold: Threshold): Amie = {
    thresholds += threshold
    this
  }

  def addConstraint(ruleConstraint: RuleConstraint): Amie = new Amie(thresholds, rulePatterns, ruleConstraint :: constraints)

  def addRulePattern(rulePattern: RulePattern): Amie = new Amie(thresholds, rulePattern :: rulePatterns, constraints)

  /**
    * Mine all closed rules from tripleIndex by defined thresholds (hc, support, rule length), optional rule pattern and constraints
    *
    * @param tripleIndex dataset from which rules are mined
    * @return Mined closed rules which satisfies all thresholds, patterns and constraints.
    *         Rules contains only these measures: head size, head coverage and support
    *         Rules are not ordered
    */
  def mine(tripleIndex: TripleHashIndex): List[ClosedRule] = {
    //if we have defined OnlyPredicates constraint we recreate lazy triple index with lazy filters which remove all predicates that are not defined
    val updatedTripleIndex = constraints.foldLeft(tripleIndex) { (tripleIndex, constraint) =>
      val itemFilter = constraint match {
        case OnlyPredicates(predicates) => Some(predicates.apply _)
        case WithoutPredicates(predicates) => Some((x: Int) => !predicates(x))
        case _ => None
      }
      itemFilter.map { itemFilter =>
        val filteredSetView = FilteredSetView(itemFilter) _
        new TripleHashIndex(
          tripleIndex.subjects.mapValues(tsi => new TripleHashIndex.TripleSubjectIndex(tsi.objects.mapValues(filteredSetView), tsi.predicates.filterKeys(itemFilter))),
          tripleIndex.predicates.filterKeys(itemFilter),
          tripleIndex.objects.mapValues(toi => new TripleHashIndex.TripleObjectIndex(toi.subjects.mapValues(filteredSetView), toi.predicates.filterKeys(itemFilter)))
        )
      }.getOrElse(tripleIndex)
    }
    //create amie process with debugger and final triple index
    val process = new AmieProcess(updatedTripleIndex)(if (logger.underlying.isDebugEnabled && !logger.underlying.isTraceEnabled) debugger else Debugger.EmptyDebugger)
    //get all possible heads and filter them by support threshold
    val heads = process.filterHeadsBySupport(process.getHeads).toList
    //parallel rule mining: for each head we search rules
    debugger.debug("Amie rules mining", heads.length) { ad =>
      heads.par.flatMap { head =>
        ad.result()(process.searchRules(head))
      }
    }.toList
  }

  private class AmieProcess(val tripleIndex: TripleHashIndex)(implicit val debugger: Debugger) extends RuleExpansion with AtomCounting {

    val isWithInstances: Boolean = constraints.contains(RuleConstraint.WithInstances)
    val minHeadCoverage: Double = thresholds[Threshold.MinHeadCoverage].value
    val maxRuleLength: Int = thresholds[Threshold.MaxRuleLength].value
    val bodyPatterns: Seq[IndexedSeq[AtomPattern]] = rulePatterns.map(_.antecedent)
    val withDuplicitPredicates: Boolean = !constraints.contains(RuleConstraint.WithoutDuplicitPredicates)

    /**
      * Get all possible heads from triple index
      *
      * @return possible head atoms
      */
    def getHeads: Iterator[Atom] = {
      //first get all variable atoms which satisfies an optional rule pattern in consequent
      //if there is no rule pattern then get all variable atoms from all predicates
      val atomIterator = if (rulePatterns.isEmpty) {
        tripleIndex.predicates.keysIterator.map(Atom(Atom.Variable(0), _, Atom.Variable(1)))
      } else {
        rulePatterns.iterator.flatMap { x =>
          x.consequent.predicate
            .map(Atom(x.consequent.subject, _, x.consequent.`object`))
            .map(Iterator(_))
            .getOrElse(tripleIndex.predicates.keysIterator.map(Atom(x.consequent.subject, _, x.consequent.`object`)))
        }
      }
      if (isWithInstances) {
        //if instantiated atoms are allowed then we specify variables by constants only if the atom has two variables
        //only one variable may be replaced by a constant
        //result is original variable atoms + instantied atoms with constants in subject + instantied atoms with constants in object
        atomIterator.flatMap { atom =>
          if (atom.subject.isInstanceOf[Atom.Variable] && atom.`object`.isInstanceOf[Atom.Variable]) {
            val it1 = tripleIndex.predicates(atom.predicate).subjects.keysIterator.map(subject => Atom(Atom.Constant(subject), atom.predicate, atom.`object`))
            val it2 = tripleIndex.predicates(atom.predicate).objects.keysIterator.map(`object` => Atom(atom.subject, atom.predicate, Atom.Constant(`object`)))
            Iterator(atom) ++ it1 ++ it2
          } else {
            Iterator(atom)
          }
        }
      } else {
        atomIterator
      }
    }

    /**
      * Filter atoms by minimal support
      *
      * @param atoms input atoms
      * @return atoms in dangling rule form - DanglingRule(no body, one head, one or two variables in head)
      */
    def filterHeadsBySupport(atoms: Iterator[Atom]): Iterator[DanglingRule] = atoms.flatMap { atom =>
      val tm = tripleIndex.predicates(atom.predicate)
      Some(atom.subject, atom.`object`).collect {
        case (v1: Atom.Variable, v2: Atom.Variable) => ExtendedRule.TwoDanglings(v1, v2, Nil) -> tm.size
        case (Atom.Constant(c), v1: Atom.Variable) => ExtendedRule.OneDangling(v1, Nil) -> tm.subjects.get(c).map(_.size).getOrElse(0)
        case (v1: Atom.Variable, Atom.Constant(c)) => ExtendedRule.OneDangling(v1, Nil) -> tm.objects.get(c).map(_.size).getOrElse(0)
      }.filter(_._2 >= minSupport).map(x => DanglingRule(Vector.empty, atom)(Measure.Measures(Measure.HeadSize(x._2), Measure.Support(x._2)), x._1, x._1.danglings.max, getAtomTriples(atom).toIndexedSeq))
    }

    /**
      * Search all expanded rules from a specific rule which satisfies all thresholds, constraints and patterns.
      *
      * @param initRule a rule to be expanded
      * @return expanded closed rules
      */
    def searchRules(initRule: ExtendedRule): List[ClosedRule] = {
      //queue for mined rules which can be also expanded
      //in this queue there can not be any duplicit rules (it speed up computation)
      val queue = new HashQueue[ExtendedRule].add(initRule)
      //list for mined closed rules
      val result = collection.mutable.ListBuffer.empty[ClosedRule]
      //if the queue is not empty, try expand rules
      while (!queue.isEmpty) {
        //get one rule from queue and remove it from that
        val rule = queue.poll
        //if the rule is closed we add it to the result set
        rule match {
          case closedRule: ClosedRule => result += closedRule
          case _ =>
        }
        //if rule length is lower than max rule length we can expand this rule with one atom
        if (rule.ruleLength < maxRuleLength) {
          //expand the rule and add all expanded variants into the queue
          howLong("Rule expansion", true)(for (rule <- rule.expand) queue.add(rule))
        }
      }
      result.toList
    }

  }

}

object Amie {

  def apply()(implicit debugger: Debugger): Amie = {
    val defaultThresholds = Threshold.Thresholds(
      Threshold.MinSupport -> Threshold.MinSupport(100),
      Threshold.MinHeadCoverage -> Threshold.MinHeadCoverage(0.01),
      Threshold.MaxRuleLength -> Threshold.MaxRuleLength(3)
    )
    new Amie(defaultThresholds, Nil, Nil)
  }

}