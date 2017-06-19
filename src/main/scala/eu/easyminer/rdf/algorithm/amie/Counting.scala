package eu.easyminer.rdf.algorithm.amie

import eu.easyminer.rdf.data.TripleHashIndex
import eu.easyminer.rdf.rule.Rule.OneDangling
import eu.easyminer.rdf.rule._

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 19. 6. 2017.
  */
trait Counting {

  case class Triple(subject: String, predicate: String, `object`: String)

  def countPaths(atoms: List[Atom], variableMap: Map[Atom.Item, Atom.Constant])(implicit tripleMap: TripleHashIndex.TripleMap, reduce: Iterator[Int] => Int): Int = atoms match {
    case head :: tail =>
      val tm = tripleMap(head.predicate)
      val it = (variableMap.getOrElse(head.subject, head.subject), variableMap.getOrElse(head.`object`, head.`object`)) match {
        case (sv: Atom.Variable, ov: Atom.Variable) =>
          tm.subjects.iterator
            .flatMap(x => x._2.iterator.map(y => Triple(x._1, head.predicate, y)))
            .map(x => countPaths(tail, variableMap +(sv -> Atom.Constant(x.subject), ov -> Atom.Constant(x.`object`))))
        case (sv: Atom.Variable, Atom.Constant(oc)) =>
          tm.objects.getOrElse(oc, Set.empty[String]).iterator.map(Triple(_, head.predicate, oc)).map(x => countPaths(tail, variableMap + (sv -> Atom.Constant(x.subject))))
        case (Atom.Constant(sc), ov: Atom.Variable) =>
          tm.subjects.getOrElse(sc, Set.empty[String]).iterator.map(Triple(sc, head.predicate, _)).map(x => countPaths(tail, variableMap + (ov -> Atom.Constant(x.`object`))))
        case (Atom.Constant(sc), Atom.Constant(oc)) =>
          if (tm.subjects.getOrElse(sc, Set.empty[String]).contains(oc)) Iterator(countPaths(tail, variableMap)) else Iterator()
      }
      reduce(it)
    case Nil => 1
  }

  def countSupport(rule: Rule[List[Atom]])(implicit tripleMap: TripleHashIndex.TripleMap): Unit = {
    implicit def reduce(it: Iterator[Int]): Int = it.find(_ == 1).getOrElse(0)
    val tm = tripleMap(rule.head.predicate)
    val sortedRelations = rule.body.reverse
    val (size, supp) = Some(rule.head).collect {
      case Atom(sv: Atom.Variable, _, ov: Atom.Variable) =>
        tm.subjects.iterator.flatMap(x => x._2.iterator.map(y => x._1 -> y)).map(x => 1 -> countPaths(sortedRelations, Map(sv -> Atom.Constant(x._1), ov -> Atom.Constant(x._2))))
      case Atom(sv: Atom.Variable, _, Atom.Constant(oc)) =>
        val subjects = tm.objects.getOrElse(oc, Set.empty[String])
        subjects.iterator.map(_ -> oc).map(x => 1 -> countPaths(sortedRelations, Map(sv -> Atom.Constant(x._1))))
      case Atom(Atom.Constant(sc), _, ov: Atom.Variable) =>
        val objects = tm.subjects.getOrElse(sc, Set.empty[String])
        objects.iterator.map(sc -> _).map(x => 1 -> countPaths(sortedRelations, Map(ov -> Atom.Constant(x._2))))
    }.map(_.reduce((x, y) => (x._1 + y._1, x._2 + y._2))).getOrElse(0 -> 0)
    rule.measures += Measure.Support(supp)
    rule.measures += Measure.HeadSize(size)
    rule.measures += Measure.HeadCoverage(if (size > 0) supp.toDouble / size else 0)
  }

  def danglingInstances(atoms: List[Atom], variableMap: Map[Atom.Item, Atom.Constant])(implicit tripleMap: TripleHashIndex.TripleMap, result: collection.mutable.Set[String] = collection.mutable.HashSet.empty): collection.mutable.Set[String] = {
    atoms match {
      case head :: tail =>
        val tm = tripleMap(head.predicate)
        (variableMap.getOrElse(head.subject, head.subject), variableMap.getOrElse(head.`object`, head.`object`)) match {
          case (sv: Atom.Variable, ov: Atom.Variable) =>
            tm.subjects.iterator
              .flatMap(x => x._2.iterator.map(y => Triple(x._1, head.predicate, y)))
              .foreach(x => danglingInstances(tail, variableMap +(sv -> Atom.Constant(x.subject), ov -> Atom.Constant(x.`object`))))
          case (sv: Atom.Variable, Atom.Constant(oc)) =>
            if (tail.isEmpty) {
              tm.objects.get(oc).foreach(result ++= _)
            } else {
              tm.objects.getOrElse(oc, Set.empty[String]).iterator.map(Triple(_, head.predicate, oc)).foreach(x => danglingInstances(tail, variableMap + (sv -> Atom.Constant(x.subject))))
            }
          case (Atom.Constant(sc), ov: Atom.Variable) =>
            if (tail.isEmpty) {
              tm.subjects.get(sc).foreach(result ++= _)
            } else {
              tm.subjects.getOrElse(sc, Set.empty[String]).iterator.map(Triple(sc, head.predicate, _)).foreach(x => danglingInstances(tail, variableMap + (ov -> Atom.Constant(x.`object`))))
            }
          case (Atom.Constant(sc), Atom.Constant(oc)) =>
            if (tm.subjects.getOrElse(sc, Set.empty[String]).contains(oc)) danglingInstances(tail, variableMap)
        }
      case Nil => throw new IllegalArgumentException
    }
    result
  }

  def countSupportInstances(rule: DanglingRule)(implicit tripleMap: TripleHashIndex.TripleMap) = {
    val dangling = rule.variables.danglings.head
    if (dangling == rule.body.head.subject || dangling == rule.body.head.`object`) {
      val tm = tripleMap(rule.head.predicate)
      val sortedRelations = rule.body.reverse
      val m = collection.mutable.HashMap.empty[String, Int]
      val r = Some(rule.head).collect {
        case Atom(sv: Atom.Variable, _, ov: Atom.Variable) =>
          tm.subjects.iterator.flatMap(x => x._2.iterator.map(y => x._1 -> y)).map(x => danglingInstances(sortedRelations, Map(sv -> Atom.Constant(x._1), ov -> Atom.Constant(x._2))))
        case Atom(sv: Atom.Variable, _, Atom.Constant(oc)) =>
          val subjects = tm.objects.getOrElse(oc, Set.empty[String])
          subjects.iterator.map(_ -> oc).map(x => danglingInstances(sortedRelations, Map(sv -> Atom.Constant(x._1))))
        case Atom(Atom.Constant(sc), _, ov: Atom.Variable) =>
          val objects = tm.subjects.getOrElse(sc, Set.empty[String])
          objects.iterator.map(sc -> _).map(x => danglingInstances(sortedRelations, Map(ov -> Atom.Constant(x._2))))
      }
      for (x <- r; y <- x; i <- y) {
        m += i -> (m.getOrElse(i, 0) + 1)
      }
      val nr = if (rule.body.head.subject == dangling) (x: String) => rule.body.head.copy(subject = Atom.Constant(x)) else (x: String) => rule.body.head.copy(`object` = Atom.Constant(x))
      val nrule: (String) => Rule = rule.variables match {
        case Rule.OneDangling(_, others) => x => ClosedRule(nr(x) :: rule.body.tail, rule.head, rule.measures.empty, others)
        case Rule.TwoDanglings(_, dangling2, others) => x => DanglingRule(nr(x) :: rule.body.tail, rule.head, rule.measures.empty, OneDangling(dangling2, others))
      }
      val headSize = rule.measures(Measure.HeadSize).asInstanceOf[Measure.HeadSize]
      m.iterator.map { x =>
        val irule = nrule(x._1)
        irule.measures += headSize
        irule.measures += Measure.Support(x._2)
        irule.measures += Measure.HeadCoverage(x._2.toDouble / headSize.value)
        irule
      }
    } else {
      Iterator()
    }
  }

}
