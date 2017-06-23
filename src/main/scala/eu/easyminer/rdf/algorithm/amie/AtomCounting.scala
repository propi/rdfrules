package eu.easyminer.rdf.algorithm.amie

import eu.easyminer.rdf.data.TripleHashIndex
import eu.easyminer.rdf.rule.Atom

/**
  * Created by Vaclav Zeman on 23. 6. 2017.
  */
trait AtomCounting {

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

  private def getAtomTriples(atom: Atom) = {
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