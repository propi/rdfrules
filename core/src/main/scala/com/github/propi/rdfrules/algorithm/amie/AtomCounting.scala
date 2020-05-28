package com.github.propi.rdfrules.algorithm.amie

import com.github.propi.rdfrules.index.TripleHashIndex
import com.github.propi.rdfrules.rule.{Atom, FreshAtom}
import com.typesafe.scalalogging.Logger

/**
  * Created by Vaclav Zeman on 23. 6. 2017.
  */
trait AtomCounting {

  //type VariableMap = Map[Atom.Variable, Atom.Constant]
  //TODO check it. These lines are solution for this situation: (?a dbo:officialLanguage ?b) ^ (?a dbo:officialLanguage dbr:English_language) -> (?a dbo:language ?b)
  //There can exist two identical mapped atoms - 2x (A dbo:officialLanguage English)
  //For this variant it should be banned - so no mapped atom should be returned if the same atom was mapped in the previous rule refinement (it should reduce support and body size)
  //We need to check it whether it is a good premision, because some ?a can have more official languages inluding English
  //For this variant it should be useful: ( ?c <direction> "east" ) ^ ( ?b <train_id> ?c ) ^ ( ?b <train_id> ?a ) ⇒ ( ?a <direction> "east" )
  //ABOVE It is isomorphic group and should be filteren after projections counting
  //We partialy solved that by the switch allowDuplicitAtoms. It is true for bodySize counting, but false for projectionBounding and support counzing. Check it whether it is right!
  class VariableMap private(hmap: Map[Atom.Variable, Atom.Constant], atoms: Set[Atom], allowDuplicitAtoms: Boolean) {
    def this(allowDuplicitAtoms: Boolean) = this(Map.empty, Set.empty, allowDuplicitAtoms)

    def getOrElse[T >: Atom.Constant](key: Atom.Variable, default: => T): T = hmap.getOrElse(key, default)

    def contains(key: Atom.Variable): Boolean = hmap.contains(key)

    def containsAtom(atom: Atom): Boolean = atoms(atom)

    def specifyAtom(atom: Atom): Atom = Atom(specifyItem(atom.subject, this), atom.predicate, specifyItem(atom.`object`, this))

    def apply(key: Atom.Variable): Atom.Constant = hmap(key)

    def +(s: (Atom.Variable, Atom.Constant), p: Int, o: (Atom.Variable, Atom.Constant)): VariableMap = if (allowDuplicitAtoms) {
      new VariableMap(hmap + (s, o), atoms, allowDuplicitAtoms)
    } else {
      new VariableMap(hmap + (s, o), atoms + Atom(s._2, p, o._2), allowDuplicitAtoms)
    }

    def +(s: Atom.Constant, p: Int, o: (Atom.Variable, Atom.Constant)): VariableMap = if (allowDuplicitAtoms) {
      new VariableMap(hmap + o, atoms, allowDuplicitAtoms)
    } else {
      new VariableMap(hmap + o, atoms + Atom(s, p, o._2), allowDuplicitAtoms)
    }

    def +(s: (Atom.Variable, Atom.Constant), p: Int, o: Atom.Constant): VariableMap = if (allowDuplicitAtoms) {
      new VariableMap(hmap + s, atoms, allowDuplicitAtoms)
    } else {
      new VariableMap(hmap + s, atoms + Atom(s._2, p, o), allowDuplicitAtoms)
    }

    /*def +(s: Atom.Constant, p: Int, o: Atom.Constant): VariableMap = if (allowDuplicitAtoms) {
      new VariableMap(hmap, atoms, allowDuplicitAtoms)
    } else {
      new VariableMap(hmap, atoms + Atom(s, p, o), allowDuplicitAtoms)
    }*/

    def isEmpty: Boolean = hmap.isEmpty
  }

  val logger: Logger = Logger[AtomCounting]
  implicit val tripleIndex: TripleHashIndex[Int]

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
  def count(atoms: Set[Atom], maxCount: Double, variableMap: VariableMap): Int = {
    var i = 0
    val it = paths(atoms, variableMap).takeWhile { _ =>
      i += 1
      i <= maxCount
    }
    while (it.hasNext) {
      it.next()
      if (i % 500 == 0) logger.trace(s"Atom counting, body size: $i (max body size: $maxCount)")
    }
    i
  }

  /**
    * Get all distinct paths for a seq of atoms
    *
    * @param atoms       atoms
    * @param variableMap variableMap
    * @return
    */
  def paths(atoms: Set[Atom], variableMap: VariableMap): Iterator[VariableMap] = {
    if (atoms.isEmpty) {
      Iterator(variableMap)
    } else {
      val best = bestAtom(atoms, variableMap)
      val rest = atoms - best
      specifyVariableMap(best, variableMap).flatMap(paths(rest, _))
    }
  }

  /**
    * For input atoms select all instantiated distinct pairs (or sequence) for input variables (headVars)
    *
    * @param atoms       all atoms
    * @param headVars    variables to be instantiated
    * @param variableMap variable mapping to a concrete constant
    * @param pairFilter  additional filter for each found pair - suitable for PCA confidence
    * @return iterator of instantiated distinct pairs for variables which have covered all atoms
    */
  def selectDistinctPairs(atoms: Set[Atom], headVars: Seq[Atom.Variable], variableMap: VariableMap, pairFilter: Seq[Atom.Constant] => Boolean = _ => true): Iterator[Seq[Atom.Constant]] = {
    val foundPairs = collection.mutable.Set.empty[Seq[Atom.Constant]]

    //TODO check it. This is maybe better solution to choose best atom but it must be tested!
    /*def chooseBestAtom(atoms: Set[Atom], variableMap: VariableMap): Atom = {
      val notMappedVars = headVars.filter(!variableMap.contains(_))
      val bestsAfterFirstRound = collection.mutable.ListBuffer.empty[Atom]
      var bestScore = 0
      for (atom <- atoms) {
        val score = notMappedVars.count(headVar => headVar == atom.subject || headVar == atom.`object`)
        if (score > bestScore) {
          bestsAfterFirstRound.clear()
          bestsAfterFirstRound += atom
          bestScore = score
        } else if (score == bestScore) {
          bestsAfterFirstRound += atom
        }
      }
      if (bestsAfterFirstRound.length == 1) {
        bestsAfterFirstRound.head
      } else {
        bestAtom(bestsAfterFirstRound, variableMap)
      }
    }*/

    def sdp(atoms: Set[Atom], variableMap: VariableMap): Iterator[Seq[Atom.Constant]] = {
      if (headVars.forall(variableMap.contains)) {
        //if all variables are mapped then we create an instantiated pair
        val pair = headVars.map(variableMap.apply)
        if (!foundPairs(pair) && pairFilter(pair) && (atoms.isEmpty || exists(atoms, variableMap))) {
          //if the pair has not been found yet and atoms is empty or there exists a path for remaining atoms then we use this pair
          foundPairs += pair
          Iterator(pair)
        } else {
          //otherwise return empty
          Iterator.empty
        }
      } else {
        //choose best atom for faster computing
        val best = bestAtom(atoms, variableMap)
        val rest = atoms - best
        //specify variables in the best atom and process the rest of atoms for each instance
        specifyVariableMap(best, variableMap).flatMap(sdp(rest, _))
      }
    }

    sdp(atoms, variableMap)
  }

  /**
    * For input atoms count all instantiated distinct pairs (or sequence) for input variables in the head atom
    *
    * @param atoms      all atoms
    * @param head       variables to be instantiated in the head
    * @param maxCount   a threshold for stopping counting
    * @param pairFilter additional filter for each found pair - suitable for PCA confidence
    * @return number of distinct pairs for variables which have covered all atoms
    */
  def countDistinctPairs(atoms: Set[Atom], head: Atom, maxCount: Double, pairFilter: Seq[Atom.Constant] => Boolean = _ => true): Int = {
    countDistinctPairs(atoms, head, maxCount, new VariableMap(true), pairFilter)
  }

  /**
    * For input atoms count all instantiated distinct pairs (or sequence) for input variables in the head atom
    *
    * @param atoms       all atoms
    * @param head        variables to be instantiated in the head
    * @param maxCount    a threshold for stopping counting
    * @param variableMap variable mapping to a concrete constant
    * @param pairFilter  additional filter for each found pair - suitable for PCA confidence
    * @return number of distinct pairs for variables which have covered all atoms
    */
  def countDistinctPairs(atoms: Set[Atom], head: Atom, maxCount: Double, variableMap: VariableMap, pairFilter: Seq[Atom.Constant] => Boolean): Int = {
    val headVars = List(head.subject, head.`object`).collect {
      case x: Atom.Variable => x
    }
    countDistinctPairs(atoms, headVars, maxCount, variableMap, pairFilter)
  }

  /**
    * For input atoms count all instantiated distinct pairs (or sequence) for input variables (headVars)
    *
    * @param atoms       all atoms
    * @param headVars    variables to be instantiated
    * @param maxCount    a threshold for stopping counting
    * @param variableMap variable mapping to a concrete constant
    * @param pairFilter  additional filter for each found pair - suitable for PCA confidence
    * @return number of distinct pairs for variables which have covered all atoms
    */
  def countDistinctPairs(atoms: Set[Atom], headVars: Seq[Atom.Variable], maxCount: Double, variableMap: VariableMap, pairFilter: Seq[Atom.Constant] => Boolean): Int = {
    var i = 0
    val it = selectDistinctPairs(atoms, headVars, variableMap, pairFilter)
    while (it.hasNext && i <= maxCount) {
      it.next()
      i += 1
      if (i % 500 == 0) logger.trace(s"Atom pairs counting, body size: $i (max body size: $maxCount)")
    }
    i
  }

  /**
    * This function returns number between 0 and 1 to recognize whether the atom should be inversed or not
    * Ex.: (?a isActorOf ?b) is inversed to (?b hasActor ?a)
    * for this: x hasActor a, x hasActor b, x hasActor c; the functionality is 1/3 = 0.33 (inversed func is: 1)
    * for this: a isActorOf x, b isActorOf x, c isActorOf x; the functionality is 3/3 = 1 (inversed func is 0.33)
    * This is used for PCA confidence counting:
    * - if in KB is this type of statement: ?b hasActor ?a then for PCA confidence we predicate ?b = subject (inverse functionality is greater)
    * - if the statement is: ?a isActorOf ?b then we predicate ?b = object
    * - so by default we predicate object, but if the inverse functionality is greater, then we need to virtually inverse the atom (swap subject/object)
    * In a nutshell: the functionality is good to recognize right subject of a statement
    *
    * @param atom atom
    * @return number between 0 and 1
    */
  def functionality(atom: Atom): Double = tripleIndex.predicates(atom.predicate).functionality

  /**
    * Inverse of the functionality
    *
    * @param atom atom
    * @return number between 0 and 1
    */
  def inverseFunctionality(atom: Atom): Double = tripleIndex.predicates(atom.predicate).inverseFunctionality

  /**
    * Create function for unspecified atom which specifies variable map by specified atom
    *
    * @param atom atom to be specified
    * @return function which return variableMap from specified atom which specifies unspecified atom
    */
  def specifyVariableMapForAtom(atom: Atom): (Atom, VariableMap) => VariableMap = (atom.subject, atom.`object`) match {
    case (s: Atom.Variable, o: Atom.Variable) => (specifiedAtom, variableMap) => variableMap + (s -> specifiedAtom.subject.asInstanceOf[Atom.Constant], atom.predicate, o -> specifiedAtom.`object`.asInstanceOf[Atom.Constant])
    case (s: Atom.Variable, o: Atom.Constant) => (specifiedAtom, variableMap) => variableMap + (s -> specifiedAtom.subject.asInstanceOf[Atom.Constant], atom.predicate, o)
    case (s: Atom.Constant, o: Atom.Variable) => (specifiedAtom, variableMap) => variableMap + (s, atom.predicate, o -> specifiedAtom.`object`.asInstanceOf[Atom.Constant])
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

  def specifySubject(atom: Atom): Iterator[Atom] = tripleIndex.predicates(atom.predicate).subjects.keysIterator.map(subject => atom.transform(subject = Atom.Constant(subject)))

  def specifyObject(atom: Atom): Iterator[Atom] = tripleIndex.predicates(atom.predicate).objects.keysIterator.map(`object` => atom.transform(`object` = Atom.Constant(`object`)))

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
        tm.subjects.iterator.flatMap(x => x._2.iterator.flatMap { y =>
          val mappedAtom = Atom(Atom.Constant(x._1), atom.predicate, Atom.Constant(y._1))
          if (variableMap.containsAtom(mappedAtom)) {
            None
          } else {
            Some(mappedAtom)
          }
        })
      case (_: Atom.Variable, ov@Atom.Constant(oc)) =>
        tm.objects.get(oc).iterator.flatMap(_.iterator).flatMap { subject =>
          val mappedAtom = Atom(Atom.Constant(subject), atom.predicate, ov)
          if (variableMap.containsAtom(mappedAtom)) {
            None
          } else {
            Some(mappedAtom)
          }
        }
      case (sv@Atom.Constant(sc), _: Atom.Variable) =>
        tm.subjects.get(sc).iterator.flatMap(_.iterator.map(_._1)).flatMap { `object` =>
          val mappedAtom = Atom(sv, atom.predicate, Atom.Constant(`object`))
          if (variableMap.containsAtom(mappedAtom)) {
            None
          } else {
            Some(mappedAtom)
          }
        }
      case (sv@Atom.Constant(sc), ov@Atom.Constant(oc)) =>
        val instantiatedAtom = Atom(sv, atom.predicate, ov)
        if (tm.subjects.get(sc).exists(x => x.contains(oc)) && !variableMap.containsAtom(instantiatedAtom)) Iterator(instantiatedAtom) else Iterator.empty
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
        tripleIndex.objects.get(oc).iterator.flatMap(_.predicates.iterator).map(predicate => Atom(sv, predicate, ov))
      case (sv@Atom.Constant(sc), ov: Atom.Variable) =>
        tripleIndex.subjects.get(sc).iterator.flatMap(_.predicates.iterator).map(predicate => Atom(sv, predicate, ov))
      case (sv@Atom.Constant(sc), ov@Atom.Constant(oc)) =>
        tripleIndex.subjects.get(sc).iterator.flatMap(_.objects.get(oc).iterator.flatMap(_.iterator).map(predicate => Atom(sv, predicate, ov)))
    }
  }

  /**
    * Specify atom and get all specified triples (only subject -> object couples)
    *
    * @param atom atom to be specified
    * @return iterator of all triples for this atom
    */
  def getAtomTriples(atom: Atom): Iterator[(Int, Int)] = specifyAtom(atom, new VariableMap(true))
    .map(x => x.subject.asInstanceOf[Atom.Constant].value -> x.`object`.asInstanceOf[Atom.Constant].value)

}