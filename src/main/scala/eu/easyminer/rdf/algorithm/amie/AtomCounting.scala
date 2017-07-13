package eu.easyminer.rdf.algorithm.amie

import com.typesafe.scalalogging.Logger
import eu.easyminer.rdf.data.TripleHashIndex
import eu.easyminer.rdf.rule.Atom

/**
  * Created by Vaclav Zeman on 23. 6. 2017.
  */
trait AtomCounting {

  type VariableMap = Map[Atom.Item, Atom.Constant]

  val logger = Logger[AtomCounting]
  val tripleIndex: TripleHashIndex

  def bestAtom(atoms: Set[Atom], variableMap: Map[Atom.Item, Atom.Constant]) = atoms.iterator.map { atom =>
    val tm = tripleIndex.predicates(atom.predicate)
    val size = (variableMap.getOrElse(atom.subject, atom.subject), variableMap.getOrElse(atom.`object`, atom.`object`)) match {
      case (_: Atom.Variable, _: Atom.Variable) => tm.size
      case (_: Atom.Variable, Atom.Constant(oc)) => tm.objects.get(oc).map(_.size).getOrElse(0)
      case (Atom.Constant(sc), _: Atom.Variable) => tm.subjects.get(sc).map(_.size).getOrElse(0)
      case (_: Atom.Constant, _: Atom.Constant) => 1
    }
    atom -> size
  }.minBy(_._2)

  def bestAtomWithFresh(atoms: Set[Atom], freshAtom: RuleExpansion.FreshAtom, variableMap: Map[Atom.Item, Atom.Constant]) = if (atoms.isEmpty) {
    Right(freshAtom)
  } else {
    val (minAtom, minAtomSize) = bestAtom(atoms, variableMap)
    val freshAtomSize = (variableMap.getOrElse(freshAtom.subject, freshAtom.subject), variableMap.getOrElse(freshAtom.`object`, freshAtom.`object`)) match {
      case (_: Atom.Variable, Atom.Constant(oc)) => tripleIndex.objects.get(oc).map(_.size).getOrElse(0)
      case (Atom.Constant(sc), _: Atom.Variable) => tripleIndex.subjects.get(sc).map(_.size).getOrElse(0)
      case (_: Atom.Constant, _: Atom.Constant) => 1
      case (_: Atom.Variable, _: Atom.Variable) => tripleIndex.size
    }
    if (freshAtomSize < minAtomSize) Right(freshAtom) else Left(minAtom)
  }

  def exists(atoms: Set[Atom], variableMap: VariableMap): Boolean = if (atoms.isEmpty) {
    true
  } else {
    val atom = if (atoms.size == 1) atoms.head else bestAtom(atoms, variableMap)._1
    val rest = if (atoms.size == 1) Set.empty[Atom] else atoms - atom
    val tm = tripleIndex.predicates(atom.predicate)
    (variableMap.getOrElse(atom.subject, atom.subject), variableMap.getOrElse(atom.`object`, atom.`object`)) match {
      case (sv: Atom.Variable, ov: Atom.Variable) =>
        tm.subjects.iterator
          .flatMap(x => x._2.iterator.map(y => variableMap +(sv -> Atom.Constant(x._1), ov -> Atom.Constant(y))))
          .exists(exists(rest, _))
      case (sv: Atom.Variable, Atom.Constant(oc)) =>
        tm.objects.getOrElse(oc, collection.mutable.Set.empty).exists(subject => exists(rest, variableMap + (sv -> Atom.Constant(subject))))
      case (Atom.Constant(sc), ov: Atom.Variable) =>
        tm.subjects.getOrElse(sc, collection.mutable.Set.empty).exists(`object` => exists(rest, variableMap + (ov -> Atom.Constant(`object`))))
      case (Atom.Constant(sc), Atom.Constant(oc)) =>
        tm.subjects.get(sc).exists(x => x(oc) && exists(rest, variableMap))
    }
  }

  def count(atoms: Set[Atom], maxCount: Double, variableMap: VariableMap = Map.empty): Int = if (atoms.isEmpty) {
    1
  } else {
    val best = bestAtom(atoms, variableMap)._1
    val rest = atoms - best
    var i = 0
    val it = specify2(best, variableMap).takeWhile { x =>
      i += count(rest, maxCount, variableMap +(best.subject -> x.subject.asInstanceOf[Atom.Constant], best.`object` -> x.`object`.asInstanceOf[Atom.Constant]))
      i <= maxCount
    }
    var j = 0
    while (it.hasNext) {
      it.next()
      j += 1
      if (variableMap.isEmpty && j % 500 == 0) logger.debug(s"Atom counting, step $j, body size: $i (max body size: $maxCount)")
    }
    i
  }

  def specify(atom: Atom, variableMap: VariableMap) = {
    val tm = tripleIndex.predicates(atom.predicate)
    (variableMap.getOrElse(atom.subject, atom.subject), variableMap.getOrElse(atom.`object`, atom.`object`)) match {
      case (sv: Atom.Variable, ov: Atom.Variable) =>
        tm.subjects.iterator
          .flatMap(x => x._2.iterator.map(y => variableMap +(sv -> Atom.Constant(x._1), ov -> Atom.Constant(y))))
      case (sv: Atom.Variable, Atom.Constant(oc)) =>
        tm.objects.get(oc).iterator.flatten.map(subject => variableMap + (sv -> Atom.Constant(subject)))
      case (Atom.Constant(sc), ov: Atom.Variable) =>
        tm.subjects.get(sc).iterator.flatten.map(`object` => variableMap + (ov -> Atom.Constant(`object`)))
      case (Atom.Constant(sc), Atom.Constant(oc)) =>
        if (tm.subjects.get(sc).exists(x => x(oc))) Iterator(variableMap) else Iterator.empty
    }
  }

  def specify2(atom: Atom, variableMap: VariableMap): Iterator[Atom] = {
    val tm = tripleIndex.predicates(atom.predicate)
    (variableMap.getOrElse(atom.subject, atom.subject), variableMap.getOrElse(atom.`object`, atom.`object`)) match {
      case (sv: Atom.Variable, ov: Atom.Variable) =>
        tm.subjects.iterator
          .flatMap(x => x._2.iterator.map(y => Atom(Atom.Constant(x._1), atom.predicate, Atom.Constant(y))))
      case (sv: Atom.Variable, ov@Atom.Constant(oc)) =>
        tm.objects.get(oc).iterator.flatten.map(subject => Atom(Atom.Constant(subject), atom.predicate, ov))
      case (sv@Atom.Constant(sc), ov: Atom.Variable) =>
        tm.subjects.get(sc).iterator.flatten.map(`object` => Atom(sv, atom.predicate, `object` = Atom.Constant(`object`)))
      case (sv@Atom.Constant(sc), ov@Atom.Constant(oc)) =>
        if (tm.subjects.get(sc).exists(x => x(oc))) Iterator(Atom(sv, atom.predicate, ov)) else Iterator.empty
    }
  }

  def specify2(atom: RuleExpansion.FreshAtom, variableMap: VariableMap)(implicit predicateFilter: (Int, RuleExpansion.TripleItemPosition) => Boolean): Iterator[Atom] = {
    (variableMap.getOrElse(atom.subject, atom.subject), variableMap.getOrElse(atom.`object`, atom.`object`)) match {
      case (sv: Atom.Variable, ov: Atom.Variable) =>
        tripleIndex.predicates.keysIterator.flatMap(predicate => specify2(Atom(sv, predicate, ov), variableMap))
      case (sv: Atom.Variable, ov@Atom.Constant(oc)) =>
        tripleIndex.objects.get(oc).iterator.flatMap(_.predicates.keysIterator.filter(x => predicateFilter(x, atom.objectPosition)).flatMap(predicate => specify2(Atom(sv, predicate, ov), variableMap)))
      case (sv@Atom.Constant(sc), ov: Atom.Variable) =>
        tripleIndex.subjects.get(sc).iterator.flatMap(_.predicates.keysIterator.filter(x => predicateFilter(x, atom.subjectPosition)).flatMap(predicate => specify2(Atom(sv, predicate, ov), variableMap)))
      case (sv@Atom.Constant(sc), ov@Atom.Constant(oc)) =>
        tripleIndex.subjects.get(sc).iterator.flatMap(_.objects.get(oc).iterator.flatten.map(predicate => Atom(sv, predicate, ov)))
    }
  }

  def getAtomTriples(atom: Atom) = {
    val tm = tripleIndex.predicates(atom.predicate)
    (atom.subject, atom.`object`) match {
      case (sv: Atom.Variable, ov: Atom.Variable) =>
        tm.subjects.iterator.flatMap(x => x._2.iterator.map(x._1 -> _))
      case (sv: Atom.Variable, Atom.Constant(oc)) =>
        tm.objects.get(oc).iterator.flatMap(_.iterator.map(_ -> oc))
      case (Atom.Constant(sc), ov: Atom.Variable) =>
        tm.subjects.get(sc).iterator.flatMap(_.iterator.map(sc -> _))
      case (Atom.Constant(sc), Atom.Constant(oc)) =>
        if (tm.subjects.get(sc).exists(x => x(oc))) Iterator(sc -> oc) else Iterator.empty
    }
  }

}