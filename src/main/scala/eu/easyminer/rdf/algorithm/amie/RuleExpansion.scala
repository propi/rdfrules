package eu.easyminer.rdf.algorithm.amie

import eu.easyminer.rdf.algorithm.amie.RuleExpansion.{FreshAtom, TripleItemPosition}
import eu.easyminer.rdf.data.{IncrementalInt, TripleHashIndex}
import eu.easyminer.rdf.rule.Rule.OneDangling
import eu.easyminer.rdf.rule._
import eu.easyminer.rdf.utils.{Debugger, HowLong}
import eu.easyminer.rdf.utils.IteratorExtensions._

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 23. 6. 2017.
  */
trait RuleExpansion extends AtomCounting {

  implicit val debugger: Debugger
  val tripleIndex: TripleHashIndex
  val isWithInstances: Boolean
  val withDuplicitPredicates: Boolean
  val minHeadCoverage: Double
  val maxRuleLength: Int
  val bodyPattern: IndexedSeq[AtomPattern]

  implicit class PimpedRule(rule: Rule) {

    type Projections = collection.mutable.HashMap[Atom, IncrementalInt]
    type TripleProjections = collection.mutable.HashSet[Atom]

    val dangling = rule.maxVariable.++
    private val patternAtom = bodyPattern.drop(rule.body.size).headOption
    private val rulePredicates: Map[Int, Set[TripleItemPosition]] = (rule.body :+ rule.head).groupBy(_.predicate).mapValues(_.iterator.flatMap(x => Iterator(TripleItemPosition.Subject(x.subject), TripleItemPosition.Object(x.`object`))).toSet)

    /**
      * Check whether fresh atom has been counted before the main mining stage.
      * Counted fresh atoms are atoms which have same predicate as any atom in the rule AND are dangling AND connecting item is on the same position as connecting atom item in the rule
      *
      * @param freshAtom fresh atom to be checked
      * @param predicate check for this predicate
      * @return true = fresh atom is counted, false = fresh atom is not counted
      */
    implicit private def isCounted(freshAtom: FreshAtom, predicate: Int): Boolean = rulePredicates.get(predicate).exists { positions =>
      (freshAtom.subject == dangling && positions(freshAtom.objectPosition)) || (freshAtom.`object` == dangling && positions(freshAtom.subjectPosition))
    }

    private def matchFreshAtom(freshAtom: FreshAtom) = patternAtom.forall { patternAtom =>
      (patternAtom.subject.isInstanceOf[Atom.Constant] || freshAtom.subject == patternAtom.subject) &&
        (patternAtom.`object`.isInstanceOf[Atom.Constant] || freshAtom.`object` == patternAtom.`object`)
    }

    private def matchAtom(atom: Atom) = patternAtom.forall { patternAtom =>
      patternAtom.predicate.forall(_ == atom.predicate) &&
        (atom.subject == patternAtom.subject || atom.subject.isInstanceOf[Atom.Constant] && patternAtom.subject.isInstanceOf[Atom.Variable]) &&
        (atom.`object` == patternAtom.`object` || atom.`object`.isInstanceOf[Atom.Constant] && patternAtom.`object`.isInstanceOf[Atom.Variable])
    } && (withDuplicitPredicates || !rulePredicates.contains(atom.predicate))

    def expand = {
      implicit val projections = collection.mutable.HashMap.empty[Atom, IncrementalInt]
      //first we get all possible fresh atoms for the current rule (with one dangling variable or with two exising variables as closed rule)
      //we separate fresh atoms to two parts: countable and others (possible)
      //countable fresh atoms is atom for which we know all existing variables (we needn't know dangling variable)
      // - then we can count all projections for this fresh atom and then only check existence of rest atoms in the rule
      //for other fresh atoms we need to find instances for unknown variables (not for dangling variable)
      val (countableFreshAtoms, possibleFreshAtoms) = getPossibleFreshAtoms.filter(matchFreshAtom).toList.partition(freshAtom => List(freshAtom.subject, freshAtom.`object`).forall(x => x == rule.head.subject || x == rule.head.`object` || x == dangling))
      //minimal support threshold for this rule
      val minSupport = rule.headSize * minHeadCoverage
      val bodySet = rule.body.toSet
      //maxSupport is variable where the maximal support from all extension rules is saved
      var maxSupport = 0
      var i = 0
      debugger.debug("Rule expansion: " + rule, rule.headSize + 1) { ad =>
        logger.debug("" + rule + "\n" + "countable: " + countableFreshAtoms + "\n" + "possible: " + possibleFreshAtoms)
        //if duplicit predicates are allowed then
        //for all fresh atoms return all extension with duplicit predicates by more efficient way
        println("****************************************")
        println("" + rule + "\n" + "countable: " + countableFreshAtoms + "\n" + "possible: " + possibleFreshAtoms)
        HowLong.howLong("count duplicit")(if (withDuplicitPredicates) (countableFreshAtoms ::: possibleFreshAtoms).foreach(selectAtomsWithExistingPredicate))
        ad.done()
        HowLong.howLong("count propjections") {
          rule.headTriples.iterator.zipWithIndex.takeWhile { x =>
            //if max support + remaining steps is lower than min support we can finish "count projection" process
            //example 1: head size is 10, min support is 5. Only 4 steps are remaining and there are no projection found then we can stop "count projection"
            // - because no projection can have support greater or equal 5
            //example 2: head size is 10, min support is 5, remaining steps 2, projection with maximal support has value 2: 2 + 2 = 4 it is less than 5 - we can stop counting
            val remains = rule.headSize - x._2
            maxSupport + remains >= minSupport
          }.foreach { case ((_subject, _object), _) =>
            //for each triple covering head of this rule, find and count all possible projections for all possible fresh atoms
            val selectedAtoms = selectAtoms2(bodySet, possibleFreshAtoms, countableFreshAtoms, Map(rule.head.subject -> Atom.Constant(_subject), rule.head.`object` -> Atom.Constant(_object)))
            for (atom <- selectedAtoms) {
              //for each found projection increase support by 1 and find max support
              maxSupport = math.max(projections.getOrElseUpdate(atom, IncrementalInt()).++.getValue, maxSupport)
            }
            /*i += 1
            if (i % 1000 == 0) logger.debug(s"Counted projections $i from ${rule.headSize}")*/
            ad.done()
          }
        }
      }
      println("total projections: " + projections.size)
      logger.debug("total projections: " + projections.size)
      //filter all projections by minimal support and remove all duplicit projections
      //then create new rules from all projections (atoms)
      projections.iterator.filter(x => x._2.getValue >= minSupport && !bodySet(x._1) && x._1 != rule.head).map { case (atom, support) =>
        expandWithAtom(atom, support.getValue)
      }
    }

    private def getPossibleFreshAtoms = rule match {
      case rule: ClosedRule =>
        val danglings = rule.variables.iterator.map(x => Iterator(FreshAtom(dangling, x), FreshAtom(x, dangling)))
        val closed = rule.variables.combinations(2).collect { case List(x, y) => Iterator(FreshAtom(x, y), FreshAtom(y, x)) }
        (danglings ++ closed).flatten
      case rule: DanglingRule =>
        rule.variables match {
          case Rule.OneDangling(dangling1, others) =>
            val danglings = Iterator(FreshAtom(dangling1, dangling), FreshAtom(dangling, dangling1))
            val closed = others.iterator.flatMap(x => Iterator(FreshAtom(dangling1, x), FreshAtom(x, dangling1)))
            danglings ++ closed
          case Rule.TwoDanglings(dangling1, dangling2, others) =>
            val danglings = if ((rule.ruleLength + 1) < maxRuleLength) rule.variables.danglings.iterator.flatMap(x => Iterator(FreshAtom(dangling, x), FreshAtom(x, dangling))) else Iterator.empty
            val closed = Iterator(FreshAtom(dangling1, dangling2), FreshAtom(dangling2, dangling1))
            danglings ++ closed
        }
    }

    private def selectAtomsWithExistingPredicate(freshAtom: FreshAtom)
                                                (implicit projections: Projections = collection.mutable.HashMap.empty) = {
      lazy val bodySet = rule.body.toSet
      /**
        * count all projections for fresh atom which is contained in the rule (only dangling atoms are supposed)
        *
        * @param variable         variable which is connected with the rule (not the dangling variable)
        * @param variablePosition position of this variable (subject or object)
        */
      def countDanglingFreshAtom(variable: Atom.Variable,
                                 variablePosition: TripleItemPosition
                                 /*filterAtoms: Atom => Boolean,
                                 tripleToAtom: (Int, Int) => Atom,
                                 predicateToItems: Int => collection.Set[Int],
                                 predicateItemToAtoms: (Int, Int) => Iterator[Atom]*/) = {
        //filter for all atoms within the rule
        //atom subject = fresh atom subject OR atom object = fresh atom object
        val filterAtoms: Atom => Boolean = variablePosition match {
          case TripleItemPosition.Subject(s) => _.subject == s
          case TripleItemPosition.Object(o) => _.`object` == o
        }
        //convert head triple to atom
        //if connecting variable is subject then we need to specify object (dangling item)
        //if connecting variable is object then we need to specify subject (dangling item)
        val headTripleToNewAtom: (Int, Int) => Atom = variablePosition match {
          case _: TripleItemPosition.Subject => (s, o) => Atom(variable, rule.head.predicate, Atom.Constant(o))
          case _: TripleItemPosition.Object => (s, o) => Atom(Atom.Constant(s), rule.head.predicate, variable)
        }
        //convert atom to instance atom projections by variableMap
        //if connecting item is subject we need to create projections for all objects of this atom (dangling item)
        //if object (dangling item) is constant we put this item as variable and then we specify this variable and save all projections for it
        //if object (dangling item) is variable then we have instance of this variable in variableMap; we can use it and save it as a new projection
        //if connecting item is subject then it is analogical behaviour
        val projectInstanceAtoms: (Atom, VariableMap, TripleProjections) => Unit = variablePosition match {
          case _: TripleItemPosition.Subject => (atom, variableMap, projections) =>
            if (atom.`object`.isInstanceOf[Atom.Constant]) {
              specifyAtom(atom.copy(`object` = dangling), variableMap).foreach(projections += _.copy(subject = atom.subject))
            } else {
              projections += Atom(atom.subject, atom.predicate, variableMap(atom.`object`))
            }
          case _: TripleItemPosition.Object => (atom, variableMap, projections) =>
            if (atom.subject.isInstanceOf[Atom.Constant]) {
              specifyAtom(atom.copy(subject = dangling), variableMap).foreach(projections += _.copy(`object` = atom.`object`))
            } else {
              projections += Atom(variableMap(atom.subject), atom.predicate, atom.`object`)
            }
        }
        //get all atoms in the rule which satisfies the filter
        //get only atoms with distinct predicates
        (Iterator(rule.head) ++ rule.body.iterator).filter(filterAtoms).distinctBy(_.predicate).foreach { atom =>
          //add this atom as a new projection with the same support as the current rule
          //if (atom.subject.isInstanceOf[Atom.Variable])
          projections += (Atom(freshAtom.subject, atom.predicate, freshAtom.`object`) -> IncrementalInt(rule.measures(Measure.Support).asInstanceOf[Measure.Support].value))
          //we can project for all filtered atoms predicates all instances much quickly
          if (isWithInstances) {
            if (atom == rule.head && atom.subject.isInstanceOf[Atom.Variable] && atom.`object`.isInstanceOf[Atom.Variable]) {
              //if the atom predicate is head and head has only variables
              //then we filter all head triples which are valid (existing according to body)
              //then we can map these triples into new projections
              //fresh-p(a, c) ^ p1(c, b) -> p(a, b) -- fresh atom is bound with all head triples which satisfies p1(c, b)
              rule.headTriples.iterator
                .filter(x => exists(bodySet, Map(rule.head.subject -> Atom.Constant(x._1), rule.head.`object` -> Atom.Constant(x._2))))
                .map(x => headTripleToNewAtom(x._1, x._2))
                .foreach(atom => projections.getOrElseUpdate(atom, IncrementalInt()).++)
            } else {
              println(s"duplicit rule: fresh - $freshAtom, atom - $atom")
              //for other cases, first we need to specify connecting atom
              //if we know all variables for this atom and subject of this atom is connecting variable then:
              // - we check existence for rest atoms if it exists then
              // - get all projections for all objects (if the connecting variable is object do it conversely)
              def instanceProjections(atoms: Set[Atom], variableMap: VariableMap)(implicit projections: TripleProjections = collection.mutable.HashSet.empty): TripleProjections = {
                if (Iterator(atom.subject, atom.`object`).forall(x => variableMap.contains(x) || x.isInstanceOf[Atom.Constant]) && exists(atoms, variableMap)) {
                  projectInstanceAtoms(atom, variableMap, projections)
                } else if (atoms.nonEmpty) {
                  val best = bestAtom(atoms, variableMap)
                  val rest = atoms - best
                  specifyVariableMap(best, variableMap).foreach(instanceProjections(rest, _))
                }
                projections
              }
              rule.headTriples.iterator
                .map(x => Map(rule.head.subject -> Atom.Constant(x._1), rule.head.`object` -> Atom.Constant(x._2)))
                .map(instanceProjections(bodySet, _))
                .foreach(_.foreach(atom => projections.getOrElseUpdate(atom, IncrementalInt()).++))
              /*val items = predicateToItems(atom.predicate)
              rule.headTriples.iterator
                .flatMap(x => items.iterator.filter(y => exists(bodySet, Map(variable -> Atom.Constant(y), rule.head.subject -> Atom.Constant(x._1), rule.head.`object` -> Atom.Constant(x._2)))))
                .flatMap(x => predicateItemToAtoms(atom.predicate, x))
                .foreach(atom => projections.getOrElseUpdate(atom, IncrementalInt()).++)*/
            }
          }
        }
        //we filter such all atoms that satisfy all constraints
        projections.retain((atom, _) => matchAtom(atom))
      }
      freshAtom match {
        case FreshAtom(`dangling`, variable) => countDanglingFreshAtom(variable, TripleItemPosition.Object(variable))
        case FreshAtom(variable, `dangling`) => countDanglingFreshAtom(variable, TripleItemPosition.Subject(variable))
        case _ =>
      }
    }

    /**
      * Select all projections (atoms) from possible fresh atoms and by instacized variables
      *
      * @param atoms
      * @param possibleFreshAtoms
      * @param countableFreshAtoms
      * @param variableMap
      * @param projections
      * @return
      */
    private def selectAtoms2(atoms: Set[Atom], possibleFreshAtoms: List[FreshAtom], countableFreshAtoms: List[FreshAtom], variableMap: VariableMap)
                            (implicit projections: TripleProjections = collection.mutable.HashSet.empty): TripleProjections = {
      //if there are some countable fresh atoms and there exists path for rest atoms then we can find all projections for these fresh atoms
      if (countableFreshAtoms.nonEmpty && exists(atoms, variableMap)) {
        countableFreshAtoms.foreach(countFreshAtom(_, variableMap))
      }
      //for each possible fresh atom select best
      if (possibleFreshAtoms.nonEmpty) {
        val (best, bestScore) = atoms.iterator.map(x => x -> scoreAtom(x, variableMap)).minBy(_._2)
        val (bestFreshAtoms, otherFreshAtoms) = possibleFreshAtoms.iterator.partition(scoreAtom(_, variableMap) <= bestScore)
        for (freshAtom <- bestFreshAtoms) {
          specifyAtom(freshAtom, variableMap).filter { atom =>
            matchAtom(atom) &&
              exists(atoms, variableMap +(freshAtom.subject -> atom.subject.asInstanceOf[Atom.Constant], freshAtom.`object` -> atom.`object`.asInstanceOf[Atom.Constant]))
          }.foreach { atom =>
            if (isWithInstances) {
              if (freshAtom.subject == dangling) projections += atom.copy(`object` = freshAtom.`object`)
              else if (freshAtom.`object` == dangling) projections += atom.copy(subject = freshAtom.subject)
            }
            projections += Atom(freshAtom.subject, atom.predicate, freshAtom.`object`)
          }
        }
        if (otherFreshAtoms.nonEmpty) {
          val rest = if (atoms.size == 1) Set.empty[Atom] else atoms - best
          val (countableAtoms, restAtoms) = otherFreshAtoms.partition(freshAtom => List(freshAtom.subject, freshAtom.`object`).forall(x => x == dangling || x == best.subject || x == best.`object` || variableMap.contains(x)))
          for (variableMap <- specifyVariableMap(best, variableMap)) {
            selectAtoms2(rest, restAtoms.toList, countableAtoms.toList, variableMap)
          }
        }
      }
      projections
    }

    /**
      * if all variables are known (instead of dangling nodes) we can count all projections
      *
      * @param atom        countable fresh atom
      * @param variableMap variable map to constants
      * @param projections projections repository
      */
    private def countFreshAtom(atom: FreshAtom, variableMap: VariableMap)
                              (implicit projections: TripleProjections): Unit = {
      //first we map all variables to constants
      (variableMap.getOrElse(atom.subject, atom.subject), variableMap.getOrElse(atom.`object`, atom.`object`)) match {
        //if there is variable on the subject place (it is dangling), count all dangling projections
        case (sv: Atom.Variable, ov@Atom.Constant(oc)) =>
          //get all preditaces for this object constant
          //skip all counted predicates that are included in this rule
          for ((predicate, subjects) <- tripleIndex.objects.get(oc).iterator.flatMap(_.predicates) if !isCounted(atom, predicate)) {
            if (isWithInstances) for (subject <- subjects; newAtom = Atom(Atom.Constant(subject), predicate, atom.`object`) if matchAtom(newAtom)) projections += newAtom
            val newAtom = Atom(sv, predicate, atom.`object`)
            if (matchAtom(newAtom)) projections += newAtom
          }
        //if there is variable on the object place (it is dangling), count all dangling projections
        case (sv@Atom.Constant(sc), ov: Atom.Variable) =>
          //get all preditaces for this subject constant
          //skip all counted predicates that are included in this rule
          for ((predicate, objects) <- tripleIndex.subjects.get(sc).iterator.flatMap(_.predicates) if !isCounted(atom, predicate)) {
            if (isWithInstances) for (_object <- objects; newAtom = Atom(atom.subject, predicate, Atom.Constant(_object)) if matchAtom(newAtom)) projections += newAtom
            val newAtom = Atom(atom.subject, predicate, ov)
            if (matchAtom(newAtom)) projections += newAtom
          }
        //all variables are constants, there are not any dangling variables - atom is closed; there is only one projection
        case (Atom.Constant(sc), Atom.Constant(oc)) =>
          //we dont need to skip counted predicate because in this case we count only with closed atom which are not counted for the existing predicate
          //we need to count all closed atoms
          for (predicate <- tripleIndex.subjects.get(sc).iterator.flatMap(_.objects.get(oc).iterator.flatten); newAtom = Atom(atom.subject, predicate, atom.`object`) if matchAtom(newAtom)) {
            projections += newAtom
          }
        case _ =>
      }
    }

    private def expandWithAtom(atom: Atom, support: Int): Rule = {
      val measures = collection.mutable.HashMap[Measure.Key, Measure](
        Measure.Support(support),
        Measure.HeadCoverage(support / rule.headSize.toDouble),
        Measure.HeadSize(rule.headSize)
      )
      (atom.subject, atom.`object`) match {
        case (sv: Atom.Variable, ov: Atom.Variable) => if (sv == dangling || ov == dangling) {
          rule match {
            case rule: DanglingRule => rule.variables match {
              case Rule.OneDangling(originalDangling, others) =>
                //(d, c) | (a, c) (a, b) (a, b) => OneDangling(c) -> OneDangling(d)
                rule.copy(body = atom +: rule.body)(measures, Rule.OneDangling(dangling, originalDangling :: others), dangling, rule.headTriples)
              case Rule.TwoDanglings(dangling1, dangling2, others) =>
                //(d, c) | (a, c) (a, b) => TwoDanglings(c, b) -> TwoDanglings(d, b)
                val (pastDangling, secondDangling) = if (sv == dangling1 || ov == dangling1) (dangling1, dangling2) else (dangling2, dangling1)
                rule.copy(body = atom +: rule.body)(measures, Rule.TwoDanglings(dangling, secondDangling, pastDangling :: others), dangling, rule.headTriples)
            }
            case rule: ClosedRule =>
              //(c, a) | (a, b) (a, b) => ClosedRule -> OneDangling(c)
              DanglingRule(atom +: rule.body, rule.head)(measures, OneDangling(dangling, rule.variables), dangling, rule.headTriples)
          }
        } else {
          rule match {
            case rule: ClosedRule =>
              //(a, b) | (a, b) (a, b) => ClosedRule -> ClosedRule
              rule.copy(atom +: rule.body)(measures, rule.variables, rule.maxVariable, rule.headTriples)
            case rule: DanglingRule =>
              //(c, a) | (c, a) (a, b) (a, b) => OneDangling(c) -> ClosedRule
              //(c, b) |(a, c) (a, b) => TwoDanglings(c, b) -> ClosedRule
              ClosedRule(atom +: rule.body, rule.head)(measures, rule.variables.danglings ::: rule.variables.others, rule.maxVariable, rule.headTriples)
          }
        }
        case (_: Atom.Variable, _: Atom.Constant) | (_: Atom.Constant, _: Atom.Variable) => rule match {
          case rule: ClosedRule =>
            //(a, C) | (a, b) (a, b) => ClosedRule -> ClosedRule
            rule.copy(atom +: rule.body)(measures, rule.variables, rule.maxVariable, rule.headTriples)
          case rule: DanglingRule => rule.variables match {
            case Rule.OneDangling(dangling, others) =>
              //(c, C) | (a, c) (a, b) (a, b) => OneDangling(c) -> ClosedRule
              ClosedRule(atom +: rule.body, rule.head)(measures, dangling :: others, dangling, rule.headTriples)
            case Rule.TwoDanglings(dangling1, dangling2, others) =>
              //(c, C) | (a, c) (a, b) => TwoDanglings(c, b) -> OneDangling(b)
              val (pastDangling, dangling) = if (atom.subject == dangling1 || atom.`object` == dangling1) (dangling1, dangling2) else (dangling2, dangling1)
              DanglingRule(atom +: rule.body, rule.head)(measures, OneDangling(dangling, pastDangling :: others), rule.maxVariable, rule.headTriples)
          }
        }
        case _ => throw new IllegalStateException
      }
    }

  }

}

object RuleExpansion {

  case class FreshAtom(subject: Atom.Variable, `object`: Atom.Variable) {
    def subjectPosition = TripleItemPosition.Subject(subject)

    def objectPosition = TripleItemPosition.Object(`object`)
  }

  sealed trait TripleItemPosition {
    val item: Atom.Item
  }

  object TripleItemPosition {

    case class Subject(item: Atom.Item) extends TripleItemPosition

    case class Object(item: Atom.Item) extends TripleItemPosition

  }

}