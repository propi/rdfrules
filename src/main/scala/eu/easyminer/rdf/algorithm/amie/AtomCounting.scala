package eu.easyminer.rdf.algorithm.amie

import com.typesafe.scalalogging.Logger
import eu.easyminer.rdf.algorithm.amie.RuleExpansion.FreshAtom
import eu.easyminer.rdf.data.TripleHashIndex
import eu.easyminer.rdf.rule.Atom

/**
  * Created by Vaclav Zeman on 23. 6. 2017.
  */
trait AtomCounting {

  type VariableMap = Map[Atom.Variable, Atom.Constant]

  val logger: Logger = Logger[AtomCounting]
  val tripleIndex: TripleHashIndex

  def specifyItem(item: Atom.Item, variableMap: VariableMap): Atom.Item = item match {
    case x: Atom.Variable => variableMap.getOrElse(x, x)
    case x => x
  }

  def scoreAtom(atom: Atom, variableMap: VariableMap): Int = {
    val tm = tripleIndex.predicates(atom.predicate)
    (specifyItem(atom.subject, variableMap), specifyItem(atom.`object`, variableMap)) match {
      case (_: Atom.Variable, _: Atom.Variable) => tm.size
      case (_: Atom.Variable, Atom.Constant(oc)) => tm.objects.get(oc).map(_.size).getOrElse(0)
      case (Atom.Constant(sc), _: Atom.Variable) => tm.subjects.get(sc).map(_.size).getOrElse(0)
      case (_: Atom.Constant, _: Atom.Constant) => 1
    }
  }

  def scoreAtom(freshAtom: FreshAtom, variableMap: VariableMap): Int = (variableMap.getOrElse(freshAtom.subject, freshAtom.subject), variableMap.getOrElse(freshAtom.`object`, freshAtom.`object`)) match {
    case (_: Atom.Variable, Atom.Constant(oc)) => tripleIndex.objects.get(oc).map(_.size).getOrElse(0)
    case (Atom.Constant(sc), _: Atom.Variable) => tripleIndex.subjects.get(sc).map(_.size).getOrElse(0)
    case (_: Atom.Constant, _: Atom.Constant) => 1
    case (_: Atom.Variable, _: Atom.Variable) => tripleIndex.size
  }

  def bestAtom(atoms: Iterable[Atom], variableMap: VariableMap): Atom = atoms.minBy(scoreAtom(_, variableMap))

  def bestFreshAtom(freshAtoms: Iterable[FreshAtom], variableMap: VariableMap): FreshAtom = freshAtoms.minBy(scoreAtom(_, variableMap))

  def exists(atoms: Set[Atom], variableMap: VariableMap): Boolean = if (atoms.isEmpty) {
    true
  } else {
    val atom = if (atoms.size == 1) atoms.head else bestAtom(atoms, variableMap)
    val rest = if (atoms.size == 1) Set.empty[Atom] else atoms - atom
    specifyVariableMap(atom, variableMap).exists(exists(rest, _))
  }

  def count(atoms: Set[Atom], maxCount: Double, variableMap: VariableMap = Map.empty): Int = if (atoms.isEmpty) {
    1
  } else {
    val best = bestAtom(atoms, variableMap)
    val rest = atoms - best
    var i = 0
    val it = specifyVariableMap(best, variableMap).takeWhile { x =>
      i += count(rest, maxCount, x)
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

  def specifyVariableMapForAtom(atom: Atom): (Atom, VariableMap) => VariableMap = (atom.subject, atom.`object`) match {
    case (s: Atom.Variable, o: Atom.Variable) => (specifiedAtom, variableMap) => variableMap +(s -> specifiedAtom.subject.asInstanceOf[Atom.Constant], o -> specifiedAtom.`object`.asInstanceOf[Atom.Constant])
    case (s: Atom.Variable, _) => (specifiedAtom, variableMap) => variableMap + (s -> specifiedAtom.subject.asInstanceOf[Atom.Constant])
    case (_, o: Atom.Variable) => (specifiedAtom, variableMap) => variableMap + (o -> specifiedAtom.`object`.asInstanceOf[Atom.Constant])
    case _ => (_, variableMap) => variableMap
  }

  def specifyVariableMap(atom: Atom, variableMap: VariableMap): Iterator[VariableMap] = {
    val specifyVariableMapWithAtom = specifyVariableMapForAtom(atom)
    specifyAtom(atom, variableMap).map { specifiedAtom =>
      specifyVariableMapWithAtom(specifiedAtom, variableMap)
    }
  }

  def specifyAtom(atom: Atom, variableMap: VariableMap): Iterator[Atom] = {
    val tm = tripleIndex.predicates(atom.predicate)
    (specifyItem(atom.subject, variableMap), specifyItem(atom.`object`, variableMap)) match {
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

  def specifyAtom(atom: RuleExpansion.FreshAtom, variableMap: VariableMap): Iterator[Atom] = {
    (variableMap.getOrElse(atom.subject, atom.subject), variableMap.getOrElse(atom.`object`, atom.`object`)) match {
      case (sv: Atom.Variable, ov: Atom.Variable) =>
        tripleIndex.predicates.keysIterator.map(predicate => Atom(sv, predicate, ov))
      case (sv: Atom.Variable, ov@Atom.Constant(oc)) =>
        tripleIndex.objects.get(oc).iterator.flatMap(_.predicates.keysIterator).map(predicate => Atom(sv, predicate, ov))
      case (sv@Atom.Constant(sc), ov: Atom.Variable) =>
        tripleIndex.subjects.get(sc).iterator.flatMap(_.predicates.keysIterator).map(predicate => Atom(sv, predicate, ov))
      case (sv@Atom.Constant(sc), ov@Atom.Constant(oc)) =>
        tripleIndex.subjects.get(sc).iterator.flatMap(_.objects.get(oc).iterator.flatten.map(predicate => Atom(sv, predicate, ov)))
    }
  }

  def getAtomTriples(atom: Atom): Iterator[(Int, Int)] = specifyAtom(atom, Map.empty[Atom.Variable, Atom.Constant])
    .map(x => x.subject.asInstanceOf[Atom.Constant].value -> x.`object`.asInstanceOf[Atom.Constant].value)

}