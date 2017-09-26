package eu.easyminer.rdf.algorithm.amie

import com.typesafe.scalalogging.Logger
import eu.easyminer.rdf.data.{HashQueue, TripleHashIndex}
import eu.easyminer.rdf.rule.ExtendedRule.{ClosedRule, DanglingRule}
import eu.easyminer.rdf.rule.RuleConstraint.OnlyPredicates
import eu.easyminer.rdf.rule._
import eu.easyminer.rdf.utils.BasicFunctions.Match
import eu.easyminer.rdf.utils.{Debugger, HowLong}

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
class Amie private(thresholds: Threshold.Thresholds, rulePattern: Option[RulePattern], constraints: List[RuleConstraint])(implicit debugger: Debugger) {

  private val logger = Logger[Amie]

  lazy val minSupport = thresholds(Threshold.MinSupport).asInstanceOf[Threshold.MinSupport].value

  def addThreshold(threshold: Threshold) = {
    thresholds += threshold
    this
  }

  def addConstraint(ruleConstraint: RuleConstraint) = new Amie(thresholds, rulePattern, ruleConstraint :: constraints)

  def setRulePattern(rulePattern: RulePattern) = new Amie(thresholds, Some(rulePattern), constraints)

  def mine(tripleIndex: TripleHashIndex) = {
    for (constraint <- constraints) Match(constraint) {
      case OnlyPredicates(predicates) => tripleIndex.predicates.keySet.iterator.filterNot(predicates.apply).foreach(tripleIndex.predicates -= _)
    }
    val rules = {
      val process = new AmieProcess(tripleIndex)(if (logger.underlying.isDebugEnabled && !logger.underlying.isTraceEnabled) debugger else Debugger.EmptyDebugger)
      val heads = process.filterHeadsBySupport(process.getHeads).toList
      debugger.debug("Amie rules mining", heads.length) { ad =>
        heads.par.flatMap { head =>
          ad.result()(process.searchRules(head))
        }
      }
    }
    val confidenceCounting = new AmieConfidenceCounting(tripleIndex)
    debugger.debug("Rules confidence counting", rules.size) { ad =>
      rules.filter { rule =>
        ad.result()(confidenceCounting.filterByConfidence(rule))
      }
    }.toList
  }

  private class AmieConfidenceCounting(val tripleIndex: TripleHashIndex) extends RuleCounting {
    val minConfidence = thresholds.getOrElse(Threshold.MinConfidence, Threshold.MinConfidence(0.0)).asInstanceOf[Threshold.MinConfidence].value

    def filterByConfidence(rule: ClosedRule) = {
      rule.withConfidence.measures(Measure.Confidence).asInstanceOf[Measure.Confidence].value >= minConfidence
    }

  }

  private class AmieProcess(val tripleIndex: TripleHashIndex)(implicit val debugger: Debugger) extends RuleExpansion with AtomCounting {

    val isWithInstances: Boolean = constraints.contains(RuleConstraint.WithInstances)
    val minHeadCoverage: Double = thresholds(Threshold.MinHeadCoverage).asInstanceOf[Threshold.MinHeadCoverage].value
    val maxRuleLength: Int = thresholds(Threshold.MaxRuleLength).asInstanceOf[Threshold.MaxRuleLength].value
    val bodyPattern: IndexedSeq[AtomPattern] = rulePattern.map(_.antecedent).getOrElse(IndexedSeq.empty)
    val withDuplicitPredicates: Boolean = !constraints.contains(RuleConstraint.WithoutDuplicitPredicates)

    def getHeads = {
      val atomIterator = rulePattern.map { x =>
        x.consequent.predicate
          .map(Atom(x.consequent.subject, _, x.consequent.`object`))
          .map(Iterator(_))
          .getOrElse(tripleIndex.predicates.keysIterator.map(Atom(x.consequent.subject, _, x.consequent.`object`)))
      }.getOrElse(tripleIndex.predicates.keysIterator.map(Atom(Atom.Variable(0), _, Atom.Variable(1))))
      if (isWithInstances) {
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

    def filterHeadsBySupport(atoms: Iterator[Atom]) = atoms.flatMap { atom =>
      val tm = tripleIndex.predicates(atom.predicate)
      Some(atom.subject, atom.`object`).collect {
        case (v1: Atom.Variable, v2: Atom.Variable) => ExtendedRule.TwoDanglings(v1, v2, Nil) -> tm.size
        case (Atom.Constant(c), v1: Atom.Variable) => ExtendedRule.OneDangling(v1, Nil) -> tm.subjects.get(c).map(_.size).getOrElse(0)
        case (v1: Atom.Variable, Atom.Constant(c)) => ExtendedRule.OneDangling(v1, Nil) -> tm.objects.get(c).map(_.size).getOrElse(0)
      }.filter(_._2 >= minSupport).map(x => DanglingRule(Vector.empty, atom)(Measure.Measures(Measure.HeadSize(x._2), Measure.Support(x._2)), x._1, x._1.danglings.max, getAtomTriples(atom).toIndexedSeq))
    }

    def searchRules(initRule: ExtendedRule) = {
      val queue = new HashQueue[ExtendedRule].add(initRule)
      val result = collection.mutable.ListBuffer.empty[ClosedRule]
      while (!queue.isEmpty) {
        //if (queue.size % 500 == 0) println("queue size (" + Thread.currentThread().getName + "): " + queue.size)
        val rule = queue.poll
        rule match {
          case closedRule: ClosedRule => result += closedRule
          case _ =>
        }
        if (rule.ruleLength < maxRuleLength) {
          HowLong.howLong("expansion", true)(for (rule <- rule.expand) queue.add(rule))
        }
      }
      result.toList
    }

  }

}

object Amie {

  def apply()(implicit debugger: Debugger): Amie = {
    val defaultThresholds: collection.mutable.Map[Threshold.Key, Threshold] = collection.mutable.HashMap(
      Threshold.MinSupport -> Threshold.MinSupport(100),
      Threshold.MinHeadCoverage -> Threshold.MinHeadCoverage(0.01),
      Threshold.MaxRuleLength -> Threshold.MaxRuleLength(3)
    )
    new Amie(defaultThresholds, None, Nil)
  }

}