package eu.easyminer.rdf.algorithm.amie

import eu.easyminer.rdf.data.TripleHashIndex
import eu.easyminer.rdf.rule.Atom

/**
  * Created by Vaclav Zeman on 23. 6. 2017.
  */
trait AtomCounting {

  type VariableMap = Map[Atom.Item, Atom.Constant]

  val tripleIndex: TripleHashIndex

  def bestAtom(atoms: Set[Atom], variableMap: Map[Atom.Item, Atom.Constant]) = atoms.minBy { atom =>
    val tm = tripleIndex.predicates(atom.predicate)
    (variableMap.getOrElse(atom.subject, atom.subject), variableMap.getOrElse(atom.`object`, atom.`object`)) match {
      case (_: Atom.Variable, _: Atom.Variable) => tm.size
      case (_: Atom.Variable, Atom.Constant(oc)) => tm.objects.get(oc).map(_.size).getOrElse(0)
      case (Atom.Constant(sc), _: Atom.Variable) => tm.subjects.get(sc).map(_.size).getOrElse(0)
      case (_: Atom.Constant, _: Atom.Constant) => 1
    }
  }

  def exists(atoms: Set[Atom], variableMap: VariableMap): Boolean = if (atoms.isEmpty) {
    true
  } else {
    val atom = if (atoms.size == 1) atoms.head else bestAtom(atoms, variableMap)
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