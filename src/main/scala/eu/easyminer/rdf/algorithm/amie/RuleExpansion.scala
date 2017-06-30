package eu.easyminer.rdf.algorithm.amie

import eu.easyminer.rdf.algorithm.amie.RuleExpansion.FreshAtom
import eu.easyminer.rdf.data.{IncrementalInt, TripleHashIndex}
import eu.easyminer.rdf.rule.Rule.OneDangling
import eu.easyminer.rdf.rule._
import eu.easyminer.rdf.utils.HowLong
import eu.easyminer.rdf.utils.IteratorExtensions._

/**
  * Created by Vaclav Zeman on 23. 6. 2017.
  */
trait RuleExpansion extends AtomCounting {

  val tripleIndex: TripleHashIndex
  val isWithInstances: Boolean
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
    val patternAtom = bodyPattern.drop(rule.body.size).headOption

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
        case (sv: Atom.Variable, Atom.Constant(oc)) =>
          for ((predicate, subjects) <- tripleIndex.objects.get(oc).iterator.flatMap(_.predicates)) {
            if (isWithInstances) for (subject <- subjects; newAtom = Atom(Atom.Constant(subject), predicate, atom.`object`) if matchAtom(newAtom)) projections += newAtom
            val newAtom = Atom(sv, predicate, atom.`object`)
            if (matchAtom(newAtom)) projections += newAtom
          }
        case (Atom.Constant(sc), ov: Atom.Variable) =>
          for ((predicate, objects) <- tripleIndex.subjects.get(sc).iterator.flatMap(_.predicates)) {
            if (isWithInstances) for (_object <- objects; newAtom = Atom(atom.subject, predicate, Atom.Constant(_object)) if matchAtom(newAtom)) projections += newAtom
            val newAtom = Atom(atom.subject, predicate, ov)
            if (matchAtom(newAtom)) projections += newAtom
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
      freshAtom match {
        case FreshAtom(`dangling`, variable) => (Iterator(rule.head) ++ rule.body.iterator).filter(_.`object` == variable).distinctBy(_.predicate).foreach { atom =>
          projections += (Atom(freshAtom.subject, atom.predicate, freshAtom.`object`) -> IncrementalInt(rule.measures(Measure.Support).asInstanceOf[Measure.Support].value))
          if (isWithInstances) {
            if (atom == rule.head) {
              val bodySet = rule.body.toSet
              rule.headTriples.iterator
                .filter(x => exists(bodySet, Map(rule.head.subject -> Atom.Constant(x._1), rule.head.`object` -> Atom.Constant(x._2))))
                .map(x => Atom(Atom.Constant(x._1), rule.head.predicate, variable))
                .foreach(atom => projections.getOrElseUpdate(atom, IncrementalInt()).++)
            } else {
              //atom je v telu - jiny pristup TODO
            }
          }
        }
      }
    }

    private def selectAtoms2(atoms: Set[Atom], possibleFreshAtoms: List[FreshAtom], countableFreshAtoms: List[FreshAtom], variableMap: VariableMap)
                            (implicit projections: TripleProjections = collection.mutable.HashSet.empty): TripleProjections = {
      if (countableFreshAtoms.nonEmpty && exists(atoms, variableMap)) {
        countableFreshAtoms.foreach { freshAtom =>
          countFreshAtom(_, variableMap)
        }
      }
      val bestAtoms = possibleFreshAtoms.map(freshAtom => freshAtom -> bestAtom2(atoms, freshAtom, variableMap))
      bestAtoms.collectFirst { case (_, Left(bestAtom)) => bestAtom } match {
        case Some(bestAtom) =>
          val rest = if (atoms.size == 1) Set.empty[Atom] else atoms - bestAtom
          val (countableAtoms, restAtoms) = possibleFreshAtoms.partition(freshAtom => List(freshAtom.subject, freshAtom.`object`).forall(x => x == dangling || x == bestAtom.subject || x == bestAtom.`object` || variableMap.contains(x)))
          for (variableMap <- specify(bestAtom, variableMap)) {
            selectAtoms2(rest, restAtoms, countableAtoms, variableMap)
          }
        case None =>
          for (freshAtom <- possibleFreshAtoms) {
            specify2(freshAtom, variableMap).filter { atom =>
              exists(atoms, variableMap +(freshAtom.subject -> atom.subject.asInstanceOf[Atom.Constant], freshAtom.`object` -> atom.subject.asInstanceOf[Atom.Constant]))
            }.foreach { atom =>
              if (isWithInstances) {
                if (freshAtom.subject == dangling) projections += atom.copy(`object` = dangling)
                else if (freshAtom.`object` == dangling) projections += atom.copy(subject = dangling)
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
    }

    def expand = {
      val (countableFreshAtoms, possibleFreshAtoms) = getPossibleFreshAtoms.filter(matchFreshAtom).toList.partition(freshAtom => List(freshAtom.subject, freshAtom.`object`).forall(x => x == rule.head.subject || x == rule.head.`object` || x == dangling))
      val minSupport = rule.headSize * minHeadCoverage
      val bodySet = rule.body.toSet
      val projections = collection.mutable.HashMap.empty[Atom, IncrementalInt]
      var maxSupport = 0
      println(rule)
      println("countable: " + countableFreshAtoms)
      println("possible: " + possibleFreshAtoms)
      /*HowLong.howLong("count projections", false)(for ((_subject, _object) <- rule.headTriples) {
        val selectedAtoms = HowLong.howLong("select one head triple", true)(selectAtoms(bodySet, possibleFreshAtoms, countableFreshAtoms, Map(rule.head.subject -> Atom.Constant(_subject), rule.head.`object` -> Atom.Constant(_object))))
        HowLong.howLong("sum projections results", true)(for ((atom, support) <- selectedAtoms) {
          val atomSupports = projections.getOrElseUpdate(atom, new Supports)
          atomSupports.support.++
          atomSupports.totalSupport += support.getValue
        })
      })*/
      var i = 0
      HowLong.howLong("count projections", false)(rule.headTriples.iterator.zipWithIndex.takeWhile { x =>
        val remains = rule.headSize - x._2
        maxSupport + remains >= minSupport
      }.foreach { case ((_subject, _object), _) =>
        i += 1
        if (i % 500 == 0) println(s"zpracovano: $i z ${rule.headSize}")
        val selectedAtoms = HowLong.howLong("select one head triple", true)(selectAtoms2(bodySet, possibleFreshAtoms, countableFreshAtoms, Map(rule.head.subject -> Atom.Constant(_subject), rule.head.`object` -> Atom.Constant(_object))))
        for (atom <- selectedAtoms) {
          maxSupport = math.max(projections.getOrElseUpdate(atom, IncrementalInt()).++.getValue, maxSupport)
        }
      })
      projections.iterator.filter(x => x._2.getValue >= minSupport && !bodySet(x._1) && x._1 != rule.head).map { case (atom, support) =>
        expandWithAtom(atom, support.getValue)
      }
    }

  }

}

object RuleExpansion {

  case class FreshAtom(subject: Atom.Variable, `object`: Atom.Variable)

}