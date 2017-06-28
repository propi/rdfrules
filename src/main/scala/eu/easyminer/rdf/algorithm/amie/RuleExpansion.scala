package eu.easyminer.rdf.algorithm.amie

import eu.easyminer.rdf.data.{IncrementalInt, TripleHashIndex}
import eu.easyminer.rdf.rule.Rule.OneDangling
import eu.easyminer.rdf.rule._
import eu.easyminer.rdf.utils.HowLong

/**
  * Created by Vaclav Zeman on 23. 6. 2017.
  */
trait RuleExpansion extends AtomCounting {

  val tripleIndex: TripleHashIndex
  val isWithInstances: Boolean
  val minHeadCoverage: Double
  val maxRuleLength: Int
  val bodyPattern: IndexedSeq[AtomPattern]

  private class Supports {
    val support = new IncrementalInt
    val totalSupport = new IncrementalInt
  }

  private case class FreshAtom(subject: Atom.Variable, `object`: Atom.Variable)

  implicit class PimpedRule(rule: Rule) {

    type Projections = collection.mutable.HashMap[Atom, IncrementalInt]

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
                              (implicit projections: Projections): Unit = {
      (variableMap.getOrElse(atom.subject, atom.subject), variableMap.getOrElse(atom.`object`, atom.`object`)) match {
        case (sv: Atom.Variable, Atom.Constant(oc)) =>
          for ((predicate, subjects) <- tripleIndex.objects.get(oc).iterator.flatMap(_.predicates)) {
            if (isWithInstances) for (subject <- subjects; newAtom = Atom(Atom.Constant(subject), predicate, atom.`object`) if matchAtom(newAtom)) projections.getOrElseUpdate(newAtom, new IncrementalInt).++
            val newAtom = Atom(sv, predicate, atom.`object`)
            if (matchAtom(newAtom)) projections.getOrElseUpdate(newAtom, new IncrementalInt) += subjects.size
          }
        case (Atom.Constant(sc), ov: Atom.Variable) =>
          for ((predicate, objects) <- tripleIndex.subjects.get(sc).iterator.flatMap(_.predicates)) {
            if (isWithInstances) for (_object <- objects; newAtom = Atom(atom.subject, predicate, Atom.Constant(_object)) if matchAtom(newAtom)) projections.getOrElseUpdate(newAtom, new IncrementalInt).++
            val newAtom = Atom(atom.subject, predicate, ov)
            if (matchAtom(newAtom)) projections.getOrElseUpdate(newAtom, new IncrementalInt) += objects.size
          }
        case (Atom.Constant(sc), Atom.Constant(oc)) =>
          for (predicate <- tripleIndex.subjects.get(sc).iterator.flatMap(_.objects.get(oc).iterator.flatten); newAtom = Atom(atom.subject, predicate, atom.`object`) if matchAtom(newAtom)) {
            projections.getOrElseUpdate(newAtom, new IncrementalInt).++
          }
        case _ =>
      }
    }

    private def selectAtoms(atoms: Set[Atom], possibleFreshAtoms: List[FreshAtom], countableFreshAtoms: List[FreshAtom], variableMap: VariableMap)
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
    }

    private def expandWithAtom(atom: Atom, supports: Supports): Rule = {
      val measures = collection.mutable.HashMap[Measure.Key, Measure](
        Measure.Support(supports.support.getValue),
        Measure.HeadCoverage(supports.support.getValue / rule.headSize.toDouble),
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
      val projections = collection.mutable.HashMap.empty[Atom, Supports]
      var maxSupport = 0
      println(rule)
      println(rule.headSize)
      /*HowLong.howLong("count projections", false)(for ((_subject, _object) <- rule.headTriples) {
        val selectedAtoms = HowLong.howLong("select one head triple", true)(selectAtoms(bodySet, possibleFreshAtoms, countableFreshAtoms, Map(rule.head.subject -> Atom.Constant(_subject), rule.head.`object` -> Atom.Constant(_object))))
        HowLong.howLong("sum projections results", true)(for ((atom, support) <- selectedAtoms) {
          val atomSupports = projections.getOrElseUpdate(atom, new Supports)
          atomSupports.support.++
          atomSupports.totalSupport += support.getValue
        })
      })*/
      HowLong.howLong("count projections", false)(rule.headTriples.iterator.zipWithIndex.takeWhile { x =>
        val remains = rule.headSize - x._2
        maxSupport + remains >= minSupport
      }.foreach { case ((_subject, _object), _) =>
        val selectedAtoms = /*HowLong.howLong("select one head triple", true)(*/ selectAtoms(bodySet, possibleFreshAtoms, countableFreshAtoms, Map(rule.head.subject -> Atom.Constant(_subject), rule.head.`object` -> Atom.Constant(_object))) //)
        for ((atom, support) <- selectedAtoms) {
          val atomSupports = projections.getOrElseUpdate(atom, new Supports)
          maxSupport = math.max(atomSupports.support.++.getValue, maxSupport)
          atomSupports.totalSupport += support.getValue
        }
      })
      projections.iterator.filter(x => x._2.support.getValue >= minSupport && !bodySet(x._1) && x._1 != rule.head).map { case (atom, supports) =>
        expandWithAtom(atom, supports)
      }
    }

  }

}
