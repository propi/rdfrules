package eu.easyminer.rdf.algorithm.amie

import eu.easyminer.rdf.data.TripleHashIndex
import eu.easyminer.rdf.rule.Rule.OneDangling
import eu.easyminer.rdf.rule.Threshold.Thresholds
import eu.easyminer.rdf.rule._

/**
  * Created by Vaclav Zeman on 23. 6. 2017.
  */
class RuleExpansion(thresholds: Thresholds, constraints: List[RuleConstraint])(implicit val tripleIndex: TripleHashIndex) {

  self: AtomCounting =>

  private val isWithInstances = constraints.contains(RuleConstraint.WithInstances)
  private val minHeadCoverage = thresholds(Threshold.MinHeadCoverage).asInstanceOf[Threshold.MinHeadCoverage].value

  private class IncrementalInt {
    private var value = 0

    def ++ = this += 1

    def +=(x: Int) = {
      value += x
      this
    }

    def getValue = value
  }

  private class Supports {
    val support = new IncrementalInt
    val totalSupport = new IncrementalInt
  }

  private case class FreshAtom(subject: Atom.Variable, `object`: Atom.Variable)

  private def countFreshAtom(atom: FreshAtom, variableMap: Map[Atom.Item, Atom.Constant])
                            (implicit result: collection.mutable.HashMap[Atom, IncrementalInt]): Unit = {
    (variableMap.getOrElse(atom.subject, atom.subject), variableMap.getOrElse(atom.`object`, atom.`object`)) match {
      case (sv: Atom.Variable, Atom.Constant(oc)) =>
        for ((predicate, subjects) <- tripleIndex.objects(oc).predicates) {
          if (isWithInstances) for (subject <- subjects) result.getOrElseUpdate(Atom(Atom.Constant(subject), predicate, atom.`object`), new IncrementalInt).++
          result.getOrElseUpdate(Atom(sv, predicate, atom.`object`), new IncrementalInt) += subjects.size
        }
      case (Atom.Constant(sc), ov: Atom.Variable) =>
        for ((predicate, objects) <- tripleIndex.subjects(sc).predicates) {
          if (isWithInstances) for (_object <- objects) result.getOrElseUpdate(Atom(atom.subject, predicate, Atom.Constant(_object)), new IncrementalInt).++
          result.getOrElseUpdate(Atom(atom.subject, predicate, ov), new IncrementalInt) += objects.size
        }
      case (Atom.Constant(sc), Atom.Constant(oc)) =>
        for (predicate <- tripleIndex.subjects(sc).objects(oc)) {
          result.getOrElseUpdate(Atom(atom.subject, predicate, atom.`object`), new IncrementalInt).++
        }
      case _ =>
    }
  }

  implicit class PimpedRule(rule: Rule[List[Atom]]) {

    val dangling = rule.maxVariable.++

    private def getPossibleFreshAtoms = rule match {
      case rule: ClosedRule =>
        val danglings = rule.variables.iterator.map(x => Iterator(FreshAtom(dangling, x), FreshAtom(x, dangling)))
        val closed = rule.variables.combinations(2).collect { case List(x, y) => Iterator(FreshAtom(x, y), FreshAtom(y, x)) }
        (danglings ++ closed).flatten
      case rule: DanglingRule =>
        val danglings = rule.variables.danglings.iterator.flatMap(x => Iterator(FreshAtom(dangling, x), FreshAtom(x, dangling)))
        val closed = rule.variables match {
          case Rule.OneDangling(dangling, others) => others.iterator.flatMap(x => Iterator(FreshAtom(dangling, x), FreshAtom(x, dangling)))
          case Rule.TwoDanglings(dangling1, dangling2, others) => Iterator(FreshAtom(dangling1, dangling2), FreshAtom(dangling2, dangling1))
        }
        danglings ++ closed
    }

    private def selectAtoms(atoms: Set[Atom], possibleFreshAtoms: List[FreshAtom], variableMap: Map[Atom.Item, Atom.Constant])
                           (implicit result: collection.mutable.HashMap[Atom, IncrementalInt] = collection.mutable.HashMap.empty): collection.Map[Atom, IncrementalInt] = {
      val (countableAtoms, restAtoms) = possibleFreshAtoms.partition(atom => (variableMap.contains(atom.subject) || atom.subject == dangling) && (variableMap.contains(atom.`object`) || atom.`object` == dangling))
      countableAtoms.foreach(countFreshAtom(_, variableMap))
      if (restAtoms.nonEmpty) {
        val atom = if (atoms.size == 1) atoms.head else bestAtom(atoms, variableMap)
        val rest = if (atoms.size == 1) Set.empty[Atom] else atoms - atom
        val tm = tripleIndex.predicates(atom.predicate)
        (variableMap.getOrElse(atom.subject, atom.subject), variableMap.getOrElse(atom.`object`, atom.`object`)) match {
          case (sv: Atom.Variable, ov: Atom.Variable) =>
            tm.subjects.iterator
              .flatMap(x => x._2.iterator.map(y => variableMap +(sv -> Atom.Constant(x._1), ov -> Atom.Constant(y))))
              .foreach(selectAtoms(rest, restAtoms, _))
          case (sv: Atom.Variable, Atom.Constant(oc)) =>
            tm.objects.getOrElse(oc, collection.mutable.Set.empty).foreach(subject => selectAtoms(rest, restAtoms, variableMap + (sv -> Atom.Constant(subject))))
          case (Atom.Constant(sc), ov: Atom.Variable) =>
            tm.subjects.getOrElse(sc, collection.mutable.Set.empty).foreach(`object` => selectAtoms(rest, restAtoms, variableMap + (ov -> Atom.Constant(`object`))))
          case (Atom.Constant(sc), Atom.Constant(oc)) =>
            if (tm.subjects.get(sc).exists(x => x(oc))) selectAtoms(rest, restAtoms, variableMap)
        }
      }
      result
    }

    private def expandRule(atom: Atom, supports: Supports): Rule[List[Atom]] = {
      val measures = collection.mutable.HashMap[Measure.Key, Measure](
        Measure.Support(supports.support.getValue),
        Measure.HeadCoverage(rule.headSize.toDouble / supports.support.getValue),
        Measure.HeadSize(rule.headSize)
      )
      (atom.subject, atom.`object`) match {
        case (sv: Atom.Variable, ov: Atom.Variable) => if (sv == dangling || ov == dangling) {
          rule match {
            case rule: DanglingRule => rule.variables match {
              case Rule.OneDangling(originalDangling, others) =>
                //(d, c) | (a, c) (a, b) (a, b) => OneDangling(c) -> OneDangling(d)
                rule.copy(body = atom :: rule.body)(measures = measures, variables = Rule.OneDangling(dangling, originalDangling :: others), maxVariable = dangling)
              case Rule.TwoDanglings(dangling1, dangling2, others) =>
                //(d, c) | (a, c) (a, b) => TwoDanglings(c, b) -> TwoDanglings(d, b)
                val (pastDangling, secondDangling) = if (sv == dangling1 || ov == dangling1) (dangling1, dangling2) else (dangling2, dangling1)
                rule.copy(body = atom :: rule.body)(measures = measures, variables = Rule.TwoDanglings(dangling, secondDangling, pastDangling :: others), maxVariable = dangling)
            }
            case rule: ClosedRule =>
              //(c, a) | (a, b) (a, b) => ClosedRule -> OneDangling(c)
              DanglingRule(atom :: rule.body, rule.head)(measures, OneDangling(dangling, rule.variables), dangling, rule.headTriples)
          }
        } else {
          rule match {
            case rule: ClosedRule =>
              //(a, b) | (a, b) (a, b) => ClosedRule -> ClosedRule
              rule.copy(atom :: rule.body)(measures = measures)
            case rule: DanglingRule =>
              //(c, a) | (c, a) (a, b) (a, b) => OneDangling(c) -> ClosedRule
              //(c, b) |(a, c) (a, b) => TwoDanglings(c, b) -> ClosedRule
              ClosedRule(atom :: rule.body, rule.head)(measures, rule.variables.danglings ::: rule.variables.others, rule.maxVariable, rule.headTriples)
          }
        }
        case (_: Atom.Variable, _: Atom.Constant) | (_: Atom.Constant, _: Atom.Variable) => rule match {
          case rule: ClosedRule =>
            //(a, C) | (a, b) (a, b) => ClosedRule -> ClosedRule
            rule.copy(atom :: rule.body)(measures = measures)
          case rule: DanglingRule => rule.variables match {
            case Rule.OneDangling(dangling, others) =>
              //(c, C) | (a, c) (a, b) (a, b) => OneDangling(c) -> ClosedRule
              ClosedRule(atom :: rule.body, rule.head)(measures, dangling :: others, dangling, rule.headTriples)
            case Rule.TwoDanglings(dangling1, dangling2, others) =>
              //(c, C) | (a, c) (a, b) => TwoDanglings(c, b) -> OneDangling(b)
              val (pastDangling, dangling) = if (atom.subject == dangling1 || atom.`object` == dangling1) (dangling1, dangling2) else (dangling2, dangling1)
              DanglingRule(atom :: rule.body, rule.head)(measures, OneDangling(dangling, pastDangling :: others), rule.maxVariable, rule.headTriples)
          }
        }
        case _ => throw new IllegalStateException
      }
    }

    def expand = {
      val possibleFreshAtoms = getPossibleFreshAtoms.toList
      val minSupport = rule.headSize * minHeadCoverage
      val bodySet = rule.body.toSet
      val projections = collection.mutable.HashMap.empty[Atom, Supports]
      for ((_subject, _object) <- rule.headTriples) {
        val selectedAtoms = selectAtoms(bodySet, possibleFreshAtoms, Map(rule.head.subject -> Atom.Constant(_subject), rule.head.`object` -> Atom.Constant(_object)))
        for ((atom, support) <- selectedAtoms) {
          val atomSupports = projections.getOrElseUpdate(atom, new Supports)
          atomSupports.support.++
          atomSupports.totalSupport += support.getValue
        }
      }
      projections.iterator.filter(_._2.support.getValue >= minSupport).map { case (atom, supports) =>
        expandRule(atom, supports)
      }
    }

  }

}
