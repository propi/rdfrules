package eu.easyminer.rdf.algorithm.amie

import eu.easyminer.rdf.data.TripleHashIndex
import eu.easyminer.rdf.rule.Rule.OneDangling
import eu.easyminer.rdf.rule._
import eu.easyminer.rdf.utils.HowLong

import scala.collection.mutable.ListBuffer
import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 19. 6. 2017.
  */
trait Counting {

  case class Triple(subject: String, predicate: String, `object`: String)

  class IncrementalInt {
    private var value = 0

    def ++ = {
      value += 1
      this
    }

    def getValue = value
  }

  type AtomWithIndex = (Atom, TripleHashIndex.TripleIndex)

  def bestAtom(atoms: Set[AtomWithIndex], variableMap: Map[Atom.Item, Atom.Constant]) = atoms.minBy { case (atom, tm) =>
    (variableMap.getOrElse(atom.subject, atom.subject), variableMap.getOrElse(atom.`object`, atom.`object`)) match {
      case (_: Atom.Variable, _: Atom.Variable) => tm.size
      case (_: Atom.Variable, Atom.Constant(oc)) => tm.objects.get(oc).map(_.size).getOrElse(0)
      case (Atom.Constant(sc), _: Atom.Variable) => tm.subjects.get(sc).map(_.size).getOrElse(0)
      case (_: Atom.Constant, _: Atom.Constant) => 1
    }
  }

  def exists(atoms: Set[AtomWithIndex], variableMap: Map[Atom.Item, Atom.Constant]): Boolean = {
    val best@(atom, tm) = if (atoms.size == 1) atoms.head else bestAtom(atoms, variableMap)
    val rest = if (atoms.size == 1) Set.empty[AtomWithIndex] else atoms - best
    (variableMap.getOrElse(atom.subject, atom.subject), variableMap.getOrElse(atom.`object`, atom.`object`)) match {
      case (sv: Atom.Variable, ov: Atom.Variable) =>
        tm.subjects.iterator.flatMap(x => x._2.iterator.map(y => variableMap +(sv -> Atom.Constant(x._1), ov -> Atom.Constant(y)))).exists(exists(rest, _))
      case (sv: Atom.Variable, Atom.Constant(oc)) =>
        tm.objects.get(oc).exists(_.exists(subject => rest.isEmpty || exists(rest, variableMap + (sv -> Atom.Constant(subject)))))
      case (Atom.Constant(sc), ov: Atom.Variable) =>
        tm.subjects.get(sc).exists(_.exists(`object` => rest.isEmpty || exists(rest, variableMap + (ov -> Atom.Constant(`object`)))))
      case (Atom.Constant(sc), Atom.Constant(oc)) =>
        tm.subjects.get(sc).exists(x => x(oc) && (rest.isEmpty || exists(rest, variableMap)))
    }
  }

  def selectDistinct(atoms: Set[AtomWithIndex], variableMap: Map[Atom.Item, Atom.Constant])(implicit variable: Atom.Variable, result: collection.mutable.HashSet[Atom.Constant]): Unit = {
    val best@(atom, tm) = if (atoms.size == 1) atoms.head else bestAtom(atoms, variableMap)
    val rest = if (atoms.size == 1) Set.empty[AtomWithIndex] else atoms - best
    (variableMap.getOrElse(atom.subject, atom.subject), variableMap.getOrElse(atom.`object`, atom.`object`)) match {
      case (sv: Atom.Variable, ov: Atom.Variable) =>
        if (sv == variable || ov == variable) {
          (if (sv == variable) tm.subjects else tm.objects)
            .iterator
            .filter(x => x._2.iterator.map(y => variableMap +(sv -> Atom.Constant(x._1), ov -> Atom.Constant(y))).exists(x => rest.isEmpty || exists(rest, x)))
            .foreach(x => result += Atom.Constant(x._1))
        } else {
          tm.subjects.iterator.flatMap(x => x._2.iterator.map(y => variableMap +(sv -> Atom.Constant(x._1), ov -> Atom.Constant(y)))).foreach(selectDistinct(rest, _))
        }
      case (sv: Atom.Variable, Atom.Constant(oc)) =>
        val it = tm.objects.get(oc).map(_.iterator).getOrElse(Iterator.empty).map(Atom.Constant)
        if (sv == variable) {
          it.filter(subject => rest.isEmpty || exists(rest, variableMap + (sv -> subject))).foreach(result += _)
        } else {
          it.foreach(subject => selectDistinct(rest, variableMap + (sv -> subject)))
        }
      case (Atom.Constant(sc), ov: Atom.Variable) =>
        val it = tm.subjects.get(sc).map(_.iterator).getOrElse(Iterator.empty).map(Atom.Constant)
        if (ov == variable) {
          it.filter(`object` => rest.isEmpty || exists(rest, variableMap + (ov -> `object`))).foreach(result += _)
        } else {
          it.foreach(`object` => selectDistinct(rest, variableMap + (ov -> `object`)))
        }
      case (Atom.Constant(sc), Atom.Constant(oc)) =>
        if (tm.subjects.get(sc).exists(x => x(oc))) selectDistinct(rest, variableMap)
    }
  }

  def countProjection(body: Set[AtomWithIndex], head: AtomWithIndex, variable: Atom.Variable, minSupport: Int): Iterator[(Atom.Constant, Int)] = {
    val (headAtom, tm) = head
    if (headAtom.subject == variable || headAtom.`object` == variable) {
      val projections = (headAtom.subject, headAtom.`object`) match {
        case (sv: Atom.Variable, ov: Atom.Variable) =>
          val (v1, v2, instances) = if (headAtom.subject == variable) (sv, ov, tm.subjects) else (ov, sv, tm.objects)
          instances.iterator.map(x => x._1 -> x._2.count(y => exists(body, Map(v1 -> Atom.Constant(x._1), v2 -> Atom.Constant(y)))))
        case (sv: Atom.Variable, Atom.Constant(oc)) =>
          tm.objects.get(oc).iterator.flatten.map(x => x -> (if (exists(body, Map(sv -> Atom.Constant(x)))) 1 else 0))
        case (Atom.Constant(sc), ov: Atom.Variable) =>
          tm.subjects.get(sc).iterator.flatten.map(x => x -> (if (exists(body, Map(ov -> Atom.Constant(x)))) 1 else 0))
      }
      projections.map(x => Atom.Constant(x._1) -> x._2)
    } else {
      val projections = collection.mutable.HashMap.empty[Atom.Constant, IncrementalInt]
      val it = (headAtom.subject, headAtom.`object`) match {
        case (sv: Atom.Variable, ov: Atom.Variable) =>
          tm.subjects.iterator.flatMap(x => x._2.iterator.map(y => Map[Atom.Item, Atom.Constant](sv -> Atom.Constant(x._1), ov -> Atom.Constant(y))))
        case (sv: Atom.Variable, Atom.Constant(oc)) =>
          tm.objects.get(oc).iterator.flatten.map(x => Map[Atom.Item, Atom.Constant](sv -> Atom.Constant(x)))
        case (Atom.Constant(sc), ov: Atom.Variable) =>
          tm.subjects.get(sc).iterator.flatten.map(x => Map[Atom.Item, Atom.Constant](ov -> Atom.Constant(x)))
      }
      it.foreach { variableMap =>
        val result = collection.mutable.HashSet.empty[Atom.Constant]
        selectDistinct(body, variableMap)(variable, result)
        result.foreach(projections.getOrElseUpdate(_, new IncrementalInt).++)
      }
      projections.iterator.map(x => x._1 -> x._2.getValue).filter(_._2 >= minSupport)
    }
  }

  def countSupport2(rule: Rule[List[Atom]], support: Int, headSize: Int) = {
    rule.measures += Measure.Support(support)
    rule.measures += Measure.HeadSize(headSize)
    rule.measures += Measure.HeadCoverage(if (headSize > 0) support.toDouble / headSize else 0)
  }

  def countPaths(atoms: List[(Atom, TripleHashIndex.TripleIndex)], variableMap: Map[Atom.Item, Atom.Constant])(implicit reduce: Iterator[Int] => Int): Int = atoms match {
    case (head, tm) :: tail =>
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
    //val sortedRelations = /*rule.body.reverse*//*sortRelations(rule.body, Set(rule.head.subject, rule.head.`object`).collect { case x: Atom.Variable => x })*/sortRelations(rule.body.toSet, Set(rule.head.subject, rule.head.`object`).collect { case x: Atom.Variable => x })
    val sortedRelations = rule.body.map(x => x -> tripleMap(x.predicate)).toSet //sortRelations(rule.body.toSet, Set(rule.head.subject, rule.head.`object`).collect { case x: Atom.Variable => x }).map(x => x -> tripleMap(x.predicate))
    val (size, supp) = Some(rule.head).collect {
        case Atom(sv: Atom.Variable, _, ov: Atom.Variable) =>
          tm.subjects.iterator.flatMap(x => x._2.iterator.map(y => x._1 -> y)).map(x => 1 -> exists(sortedRelations, Map(sv -> Atom.Constant(x._1), ov -> Atom.Constant(x._2))))
        case Atom(sv: Atom.Variable, _, Atom.Constant(oc)) =>
          val subjects = tm.objects.getOrElse(oc, Set.empty[String])
          subjects.iterator.map(_ -> oc).map(x => 1 -> exists(sortedRelations, Map(sv -> Atom.Constant(x._1))))
        case Atom(Atom.Constant(sc), _, ov: Atom.Variable) =>
          val objects = tm.subjects.getOrElse(sc, Set.empty[String])
          objects.iterator.map(sc -> _).map(x => 1 -> exists(sortedRelations, Map(ov -> Atom.Constant(x._2))))
      }.map(_.map(x => (x._1, if (x._2) 1 else 0)).reduce((x, y) => (x._1 + y._1, x._2 + y._2))).getOrElse(0 -> 0)
    rule.measures += Measure.Support(supp)
    rule.measures += Measure.HeadSize(size)
    rule.measures += Measure.HeadCoverage(if (size > 0) supp.toDouble / size else 0)
  }

  def danglingInstances(atoms: List[(Atom, TripleHashIndex.TripleIndex)], variableMap: Map[Atom.Item, Atom.Constant])(implicit tripleMap: TripleHashIndex.TripleMap, result: collection.mutable.Set[String] = collection.mutable.HashSet.empty): collection.mutable.Set[String] = {
    atoms match {
      case (head, tm) :: tail =>
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
      val sortedRelations = sortRelations(rule.body.tail.toSet, Set(rule.head.subject, rule.head.`object`).collect { case x: Atom.Variable => x }).map(x => x -> tripleMap(x.predicate))
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
      val nrule: (String) => Rule[List[Atom]] = rule.variables match {
        case Rule.OneDangling(_, others) => x => ClosedRule(nr(x) :: rule.body.tail, rule.head, rule.measures.empty, others, rule.maxVariable.--)
        case Rule.TwoDanglings(_, dangling2, others) => x => DanglingRule(nr(x) :: rule.body.tail, rule.head, rule.measures.empty, OneDangling(dangling2, others), rule.maxVariable.--)
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
      Iterator.empty
    }
  }

  @scala.annotation.tailrec
  private def sortRelations(atoms: Set[Atom], specifiedVariables: Set[Atom.Variable], result: ListBuffer[Atom] = ListBuffer.empty)(implicit tripleMap: TripleHashIndex.TripleMap): List[Atom] = {
    def count(r: Atom) = {
      val tripleIndex = tripleMap(r.predicate)
      val (unknownSize1, subjectSize) = r.subject match {
        case v: Atom.Variable if !specifiedVariables(v) => 1 -> tripleIndex.size
        case _ => 0 -> 0
      }
      val (unknownSize2, objectSize) = r.`object` match {
        case v: Atom.Variable if !specifiedVariables(v) => 1 -> tripleIndex.size
        case _ => 0 -> 0
      }
      (unknownSize1 + unknownSize2, subjectSize + objectSize)
    }
    if (atoms.isEmpty) {
      result.toList
    } else {
      val minAtom = if (atoms.size > 1) atoms.minBy(count) else atoms.head
      result += minAtom
      sortRelations(atoms - minAtom, specifiedVariables ++ List(minAtom.subject, minAtom.`object`).collect { case x: Atom.Variable => x }, result)
    }
  }

}

object Counting extends Counting