package eu.easyminer.rdf.algorithm.amie

import com.typesafe.scalalogging.Logger
import eu.easyminer.rdf.index.TripleHashIndex
import eu.easyminer.rdf.rule.{Atom, FreshAtom}

/**
  * Created by Vaclav Zeman on 23. 6. 2017.
  */
trait AtomCounting {

  type VariableMap = Map[Atom.Variable, Atom.Constant]

  val logger: Logger = Logger[AtomCounting]
  val tripleIndex: TripleHashIndex

  /**
    * Specify item from variableMap.
    * If the item is not included in variableMap it returns original item otherwise returns constant
    *
    * @param item        item to be specified
    * @param variableMap variable map
    * @return specified item or original item if it is not included in the variable map
    */
  def specifyItem(item: Atom.Item, variableMap: VariableMap): Atom.Item = item match {
    case x: Atom.Variable => variableMap.getOrElse(x, x)
    case x => x
  }

  /**
    * Score atom. Lower value is better score.
    * Score is counted by number of triples for this atom.
    * Atom items are specified by variableMap
    *
    * @param atom        input atom to be scored
    * @param variableMap constants which will be mapped to variables
    * @return score (number of triples)
    */
  def scoreAtom(atom: Atom, variableMap: VariableMap): Int = {
    val tm = tripleIndex.predicates(atom.predicate)
    (specifyItem(atom.subject, variableMap), specifyItem(atom.`object`, variableMap)) match {
      case (_: Atom.Variable, _: Atom.Variable) => tm.size
      case (_: Atom.Variable, Atom.Constant(oc)) => tm.objects.get(oc).map(_.size).getOrElse(0)
      case (Atom.Constant(sc), _: Atom.Variable) => tm.subjects.get(sc).map(_.size).getOrElse(0)
      case (_: Atom.Constant, _: Atom.Constant) => 1
    }
  }

  /**
    * Score fresh atom. Lower value is better score.
    * Score is counted by number of triples for this atom which has not specified any predicates.
    * Therefore the score is counted across all possible predicates and may be greater than score of normal atom.
    * Atom items are specified by variableMap
    *
    * @param freshAtom   input fresh atom to be scored
    * @param variableMap constants which will be mapped to variables
    * @return score (number of triples)
    */
  def scoreAtom(freshAtom: FreshAtom, variableMap: VariableMap): Int = (variableMap.getOrElse(freshAtom.subject, freshAtom.subject), variableMap.getOrElse(freshAtom.`object`, freshAtom.`object`)) match {
    case (_: Atom.Variable, Atom.Constant(oc)) => tripleIndex.objects.get(oc).map(_.size).getOrElse(0)
    case (Atom.Constant(sc), _: Atom.Variable) => tripleIndex.subjects.get(sc).map(_.size).getOrElse(0)
    case (Atom.Constant(sc), Atom.Constant(oc)) => tripleIndex.subjects.get(sc).flatMap(_.objects.get(oc).map(_.size)).getOrElse(0)
    case (_: Atom.Variable, _: Atom.Variable) => tripleIndex.size
  }

  /**
    * Get best atom from atoms by best score (scoreAtom function)
    *
    * @param atoms       atoms collection
    * @param variableMap constants which will be mapped to variables
    * @return best atom
    */
  def bestAtom(atoms: Iterable[Atom], variableMap: VariableMap): Atom = atoms.minBy(scoreAtom(_, variableMap))

  /**
    * Get best fresh atom from atoms by best score (scoreAtom function)
    *
    * @param freshAtoms  fresh atoms collection
    * @param variableMap constants which will be mapped to variables
    * @return best fresh atom
    */
  def bestFreshAtom(freshAtoms: Iterable[FreshAtom], variableMap: VariableMap): FreshAtom = freshAtoms.minBy(scoreAtom(_, variableMap))

  /**
    * Check connection of atoms set.
    * All atoms need to be connected by variables and for this connection/path triples must exist
    *
    * @param atoms       set of atoms
    * @param variableMap constants which will be mapped to variables
    * @return true = atoms are connected and the path exists within dataset
    */
  def exists(atoms: Set[Atom], variableMap: VariableMap): Boolean = if (atoms.isEmpty) {
    true
  } else {
    val atom = if (atoms.size == 1) atoms.head else bestAtom(atoms, variableMap)
    val rest = if (atoms.size == 1) Set.empty[Atom] else atoms - atom
    specifyVariableMap(atom, variableMap).exists(exists(rest, _))
  }

  /**
    * It is similar as the exists function, but it does not check an existence but it counts all possible path for given atoms
    *
    * @param atoms       set of atoms
    * @param maxCount    upper limit - we count possible paths until we reach to this max count limit (combinatoric explosion prevention)
    *                    e.g.: this is speed up for confidence counting. If number of path is greater than some limit then confidence will be lower than chosen threshold.
    * @param variableMap constants which will be mapped to variables
    * @return number of possible paths for the set of atoms which are contained in dataset
    */
  def count(atoms: Set[Atom], maxCount: Double, variableMap: VariableMap = Map.empty): Int = if (atoms.isEmpty) {
    1
  } else if (maxCount <= 0) {
    0
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
      if (variableMap.isEmpty && j % 500 == 0) logger.trace(s"Atom counting, step $j, body size: $i (max body size: $maxCount)")
    }
    i
  }

  /**
    * Create function for unspecified atom which specifies variable map by specified atom
    *
    * @param atom atom to be specified
    * @return function which return variableMap from specified atom which specifies unspecified atom
    */
  def specifyVariableMapForAtom(atom: Atom): (Atom, VariableMap) => VariableMap = (atom.subject, atom.`object`) match {
    case (s: Atom.Variable, o: Atom.Variable) => (specifiedAtom, variableMap) => variableMap + (s -> specifiedAtom.subject.asInstanceOf[Atom.Constant], o -> specifiedAtom.`object`.asInstanceOf[Atom.Constant])
    case (s: Atom.Variable, _) => (specifiedAtom, variableMap) => variableMap + (s -> specifiedAtom.subject.asInstanceOf[Atom.Constant])
    case (_, o: Atom.Variable) => (specifiedAtom, variableMap) => variableMap + (o -> specifiedAtom.`object`.asInstanceOf[Atom.Constant])
    case _ => (_, variableMap) => variableMap
  }

  /**
    * Get all projections for input atom and variableMap and put them into variableMap.
    * It is same as specifyAtom function, but instead of atoms (projections) returns variableMaps
    *
    * @param atom        atom to be specified
    * @param variableMap constants which will be mapped to variables
    * @return iterator of all projections of this atom in variableMap
    */
  def specifyVariableMap(atom: Atom, variableMap: VariableMap): Iterator[VariableMap] = {
    val specifyVariableMapWithAtom = specifyVariableMapForAtom(atom)
    specifyAtom(atom, variableMap).map { specifiedAtom =>
      specifyVariableMapWithAtom(specifiedAtom, variableMap)
    }
  }

  /**
    * Get all specified atoms (projections) for input atom and variableMap
    *
    * @param atom        atom to be specified
    * @param variableMap constants which will be mapped to variables
    * @return iterator of all projections
    */
  def specifyAtom(atom: Atom, variableMap: VariableMap): Iterator[Atom] = {
    val tm = tripleIndex.predicates(atom.predicate)
    (specifyItem(atom.subject, variableMap), specifyItem(atom.`object`, variableMap)) match {
      case (_: Atom.Variable, _: Atom.Variable) =>
        tm.subjects.iterator
          .flatMap(x => x._2.iterator.map(y => Atom(Atom.Constant(x._1), atom.predicate, Atom.Constant(y))))
      case (_: Atom.Variable, ov@Atom.Constant(oc)) =>
        tm.objects.get(oc).iterator.flatten.map(subject => Atom(Atom.Constant(subject), atom.predicate, ov))
      case (sv@Atom.Constant(sc), _: Atom.Variable) =>
        tm.subjects.get(sc).iterator.flatten.map(`object` => Atom(sv, atom.predicate, `object` = Atom.Constant(`object`)))
      case (sv@Atom.Constant(sc), ov@Atom.Constant(oc)) =>
        if (tm.subjects.get(sc).exists(x => x(oc))) Iterator(Atom(sv, atom.predicate, ov)) else Iterator.empty
    }
  }

  /**
    * Get all specified atoms (projections) for input fresh atom and variableMap
    * This function specifies only predicates, not variables!
    *
    * @param atom        fresh atom to be specified
    * @param variableMap constants which will be mapped to variables
    * @return iterator of all predicate projections
    */
  def specifyAtom(atom: FreshAtom, variableMap: VariableMap): Iterator[Atom] = {
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

  /**
    * Specify atom and get all specified triples (only subject -> object couples)
    *
    * @param atom atom to be specified
    * @return iterator of all triples for this atom
    */
  def getAtomTriples(atom: Atom): Iterator[(Int, Int)] = specifyAtom(atom, Map.empty[Atom.Variable, Atom.Constant])
    .map(x => x.subject.asInstanceOf[Atom.Constant].value -> x.`object`.asInstanceOf[Atom.Constant].value)

}