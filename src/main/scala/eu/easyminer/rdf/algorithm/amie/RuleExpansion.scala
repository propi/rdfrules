package eu.easyminer.rdf.algorithm.amie

import com.typesafe.scalalogging.Logger
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

  private val logger = Logger[RuleExpansion]

  val tripleIndex: TripleHashIndex
  val isWithInstances: Boolean
  val withDuplicitPredicates: Boolean
  val minHeadCoverage: Double
  val maxRuleLength: Int
  val bodyPattern: IndexedSeq[AtomPattern]

  /*private class Supports {
    val support = new IncrementalInt
    val totalSupport = new IncrementalInt
  }*/

  implicit class PimpedRule(rule: Rule) {

    type Projections = collection.mutable.HashMap[Atom, IncrementalInt]
    type TripleProjections = collection.mutable.HashSet[Atom]

    val dangling = rule.maxVariable.++
    private val patternAtom = bodyPattern.drop(rule.body.size).headOption
    private val rulePredicates = rule.body.iterator.map(_.predicate).toSet + rule.head.predicate
    //private val potentiallyRedundantAtoms =

    /**
      * Pokud se predikat nevyskytuje v pravidlu vrat true
      * Pokud se vyskytuje, zkontroluj jestli se freshAtom variable rovna promenne v headu a vyskytuje se na stejne pozici
      * Pokud ano, vrat false (je jiz spocteno)
      * Pokud ne, a nejsou povoleny instance, vrat false (je jiz spocteno)
      * Pokud ne, a nejsou povoleny duplicity, vrat false (nebudeme pocitat duplicity)
      * V opacnem pripade: jiny predikát NEBO stejný predikát a variable se nerovna head variable a jsou povoleny duplicity a jsou povoleny instance - vrat true (je potreba spocitat)
      *
      * @param predicate
      * @param connectingItem
      * @return
      */
    implicit private def predicateFilter(predicate: Int, connectingItem: TripleItemPosition): Boolean = !rulePredicates(predicate) || (connectingItem match {
      case TripleItemPosition.Subject(item) => item != rule.head.subject
      case TripleItemPosition.Object(item) => item != rule.head.`object`
    }) && isWithInstances && withDuplicitPredicates

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

    private def countFreshAtom(atom: FreshAtom, variableMap: VariableMap)
                              (implicit projections: TripleProjections): Unit = {
      (variableMap.getOrElse(atom.subject, atom.subject), variableMap.getOrElse(atom.`object`, atom.`object`)) match {
        case (sv: Atom.Variable, ov@Atom.Constant(oc)) =>
          for ((predicate, subjects) <- tripleIndex.objects.get(oc).iterator.flatMap(_.predicates) if predicateFilter(predicate, atom.objectPosition)) {
            if (isWithInstances) for (subject <- subjects; newAtom = Atom(Atom.Constant(subject), predicate, atom.`object`) if matchAtom(newAtom)) projections += newAtom
            if (!rulePredicates(predicate)) {
              val newAtom = Atom(sv, predicate, atom.`object`)
              if (matchAtom(newAtom)) projections += newAtom
            }
          }
        case (sv@Atom.Constant(sc), ov: Atom.Variable) =>
          for ((predicate, objects) <- tripleIndex.subjects.get(sc).iterator.flatMap(_.predicates) if predicateFilter(predicate, atom.subjectPosition)) {
            if (isWithInstances) for (_object <- objects; newAtom = Atom(atom.subject, predicate, Atom.Constant(_object)) if matchAtom(newAtom)) projections += newAtom
            if (!rulePredicates(predicate)) {
              val newAtom = Atom(atom.subject, predicate, ov)
              if (matchAtom(newAtom)) projections += newAtom
            }
          }
        case (Atom.Constant(sc), Atom.Constant(oc)) =>
          for (predicate <- tripleIndex.subjects.get(sc).iterator.flatMap(_.objects.get(oc).iterator.flatten); newAtom = Atom(atom.subject, predicate, atom.`object`) if matchAtom(newAtom)) {
            projections += newAtom
          }
        case _ =>
      }
    }

    /*private def countFreshAtom2(atom: FreshAtom, variableMap: VariableMap, isWithInstances: Boolean)
                               (implicit projections: Projections): Iterator[(Atom, Int)] = {
      (variableMap.getOrElse(atom.subject, atom.subject), variableMap.getOrElse(atom.`object`, atom.`object`)) match {
        case (sv: Atom.Variable, Atom.Constant(oc)) =>
          val it = for ((predicate, subjects) <- tripleIndex.objects.get(oc).iterator.flatMap(_.predicates)) yield {
            val instances = if (isWithInstances) for (subject <- subjects.iterator; newAtom = Atom(Atom.Constant(subject), predicate, atom.`object`) if matchAtom(newAtom)) yield newAtom -> 1 else Iterator.empty
            val newAtom = Atom(sv, predicate, atom.`object`)
            val atoms = if (matchAtom(newAtom)) Iterator(newAtom -> subjects.size) else Iterator.empty
            atoms ++ instances
          }
          it.flatten
        case (Atom.Constant(sc), ov: Atom.Variable) =>
          val it = for ((predicate, objects) <- tripleIndex.subjects.get(sc).iterator.flatMap(_.predicates)) yield {
            val instances = if (isWithInstances) for (_object <- objects.iterator; newAtom = Atom(atom.subject, predicate, Atom.Constant(_object)) if matchAtom(newAtom)) yield newAtom -> 1 else Iterator.empty
            val newAtom = Atom(atom.subject, predicate, ov)
            val atoms = if (matchAtom(newAtom)) Iterator(newAtom -> objects.size) else Iterator.empty
            atoms ++ instances
          }
          it.flatten
        case (Atom.Constant(sc), Atom.Constant(oc)) =>
          for (predicate <- tripleIndex.subjects.get(sc).iterator.flatMap(_.objects.get(oc).iterator.flatten); newAtom = Atom(atom.subject, predicate, atom.`object`) if matchAtom(newAtom)) yield {
            newAtom -> 1
          }
        case _ => Iterator.empty
      }
    }*/

    private def selectAtomsWithExistingPredicate(freshAtom: FreshAtom)
                                                (implicit projections: Projections = collection.mutable.HashMap.empty) = {
      lazy val bodySet = rule.body.toSet
      def countDanglingFreshAtom(variable: Atom.Variable,
                                 variablePosition: TripleItemPosition
                                 /*filterAtoms: Atom => Boolean,
                                 tripleToAtom: (Int, Int) => Atom,
                                 predicateToItems: Int => collection.Set[Int],
                                 predicateItemToAtoms: (Int, Int) => Iterator[Atom]*/) = {
        val filterAtoms: Atom => Boolean = variablePosition match {
          case TripleItemPosition.Subject(s) => _.subject == s
          case TripleItemPosition.Object(o) => _.`object` == o
        }
        val headTripleToNewAtom: (Int, Int) => Atom = variablePosition match {
          case _: TripleItemPosition.Subject => (s, o) => Atom(Atom.Constant(s), rule.head.predicate, variable)
          case _: TripleItemPosition.Object => (s, o) => Atom(variable, rule.head.predicate, Atom.Constant(o))
        }
        (Iterator(rule.head) ++ rule.body.iterator).filter(filterAtoms).distinctBy(_.predicate).foreach { atom =>
          val newGeneralAtom = Atom(freshAtom.subject, atom.predicate, freshAtom.`object`)
          if (matchAtom(newGeneralAtom)) {
            projections += (Atom(freshAtom.subject, atom.predicate, freshAtom.`object`) -> IncrementalInt(rule.measures(Measure.Support).asInstanceOf[Measure.Support].value))
          }
          if (isWithInstances && atom == rule.head && atom.subject.isInstanceOf[Atom.Variable] && atom.`object`.isInstanceOf[Atom.Variable]) {
            rule.headTriples.iterator
              .filter(x => exists(bodySet, Map(rule.head.subject -> Atom.Constant(x._1), rule.head.`object` -> Atom.Constant(x._2))))
              .map(x => headTripleToNewAtom(x._1, x._2))
              .filter(matchAtom)
              .foreach(atom => projections.getOrElseUpdate(atom, IncrementalInt()).++)
            /* else {
             val items = predicateToItems(atom.predicate)
             rule.headTriples.iterator
               .flatMap(x => items.iterator.filter(y => exists(bodySet, Map(variable -> Atom.Constant(y), rule.head.subject -> Atom.Constant(x._1), rule.head.`object` -> Atom.Constant(x._2)))))
               .flatMap(x => predicateItemToAtoms(atom.predicate, x))
               .foreach(atom => projections.getOrElseUpdate(atom, IncrementalInt()).++)
           }*/
          }
        }
      }
      freshAtom match {
        case FreshAtom(`dangling`, variable) => countDanglingFreshAtom(variable, TripleItemPosition.Object(variable))
        case FreshAtom(variable, `dangling`) => countDanglingFreshAtom(variable, TripleItemPosition.Subject(variable))
        case _ =>
      }
    }

    private def selectAtoms2(atoms: Set[Atom], possibleFreshAtoms: List[FreshAtom], countableFreshAtoms: List[FreshAtom], variableMap: VariableMap)
                            (implicit projections: TripleProjections = collection.mutable.HashSet.empty): TripleProjections = {
      if (countableFreshAtoms.nonEmpty && exists(atoms, variableMap)) {
        countableFreshAtoms.foreach(countFreshAtom(_, variableMap))
      }
      val bestAtoms = possibleFreshAtoms.map(freshAtom => bestAtomWithFresh(atoms, freshAtom, variableMap))
      bestAtoms.collectFirst { case Left(bestAtom) => bestAtom } match {
        case Some(bestAtom) =>
          val rest = if (atoms.size == 1) Set.empty[Atom] else atoms - bestAtom
          val (countableAtoms, restAtoms) = possibleFreshAtoms.partition(freshAtom => List(freshAtom.subject, freshAtom.`object`).forall(x => x == dangling || x == bestAtom.subject || x == bestAtom.`object` || variableMap.contains(x)))
          for (variableMap <- specify(bestAtom, variableMap)) {
            selectAtoms2(rest, restAtoms, countableAtoms, variableMap)
          }
        case None =>
          for (freshAtom <- possibleFreshAtoms) {
            specify2(freshAtom, variableMap).filter { atom =>
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
      }
      projections
    }

    /*private def selectAtoms(atoms: Set[Atom], possibleFreshAtoms: List[FreshAtom], countableFreshAtoms: List[FreshAtom], variableMap: VariableMap)
                           (implicit projections: Projections = collection.mutable.HashMap.empty): Projections = {
      if (countableFreshAtoms.nonEmpty && exists(atoms, variableMap)) {
        countableFreshAtoms.foreach(countFreshAtom(_, variableMap))
      }
      if (possibleFreshAtoms.nonEmpty) {
        val atom = if (atoms.size == 1) atoms.head else bestAtom(atoms, variableMap)
        val rest = if (atoms.size == 1) Set.empty[Atom] else atoms - atom
        val (countableAtoms, restAtoms) = possibleFreshAtoms.partition(freshAtom => List(freshAtom.subject, freshAtom.`object`).forall(x => x == dangling || x == atom.subject || x == atom.`object` || variableMap.contains(x)))
        for (variableMap <- specify(atom, variableMap)) {
          selectAtoms(rest, restAtoms, countableAtoms, variableMap)
        }
      }
      projections
    }*/

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

    private def matchFreshAtom(freshAtom: FreshAtom) = patternAtom.forall { patternAtom =>
      (patternAtom.subject.isInstanceOf[Atom.Constant] || freshAtom.subject == patternAtom.subject) &&
        (patternAtom.`object`.isInstanceOf[Atom.Constant] || freshAtom.`object` == patternAtom.`object`)
    }

    private def matchAtom(atom: Atom) = patternAtom.forall { patternAtom =>
      patternAtom.predicate.forall(_ == atom.predicate) &&
        (atom.subject == patternAtom.subject || atom.subject.isInstanceOf[Atom.Constant] && patternAtom.subject.isInstanceOf[Atom.Variable]) &&
        (atom.`object` == patternAtom.`object` || atom.`object`.isInstanceOf[Atom.Constant] && patternAtom.`object`.isInstanceOf[Atom.Variable])
    } && (withDuplicitPredicates || !rulePredicates(atom.predicate))

    def expand(implicit debugger: Debugger) = {
      implicit val projections = collection.mutable.HashMap.empty[Atom, IncrementalInt]
      val (countableFreshAtoms, possibleFreshAtoms) = getPossibleFreshAtoms.filter(matchFreshAtom).toList.partition(freshAtom => List(freshAtom.subject, freshAtom.`object`).forall(x => x == rule.head.subject || x == rule.head.`object` || x == dangling))
      val minSupport = rule.headSize * minHeadCoverage
      val bodySet = rule.body.toSet
      var maxSupport = 0
      var i = 0
      debugger.debug("Rule expansion: " + rule, rule.headSize + 1) { ad =>
        logger.debug("" + rule + "\n" + "countable: " + countableFreshAtoms + "\n" + "possible: " + possibleFreshAtoms)
        if (withDuplicitPredicates) (countableFreshAtoms ::: possibleFreshAtoms).foreach(selectAtomsWithExistingPredicate)
        ad.done()
        rule.headTriples.iterator.zipWithIndex.takeWhile { x =>
          val remains = rule.headSize - x._2
          maxSupport + remains >= minSupport
        }.foreach { case ((_subject, _object), _) =>
          val selectedAtoms = selectAtoms2(bodySet, possibleFreshAtoms, countableFreshAtoms, Map(rule.head.subject -> Atom.Constant(_subject), rule.head.`object` -> Atom.Constant(_object)))
          for (atom <- selectedAtoms) {
            maxSupport = math.max(projections.getOrElseUpdate(atom, IncrementalInt()).++.getValue, maxSupport)
          }
          i += 1
          if (i % 500 == 0) logger.debug(s"Counted projections $i from ${rule.headSize}")
          ad.done()
        }
      }
      logger.debug("total projections: " + projections.size)
      projections.iterator.filter(x => x._2.getValue >= minSupport && !bodySet(x._1) && x._1 != rule.head).map { case (atom, support) =>
        expandWithAtom(atom, support.getValue)
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