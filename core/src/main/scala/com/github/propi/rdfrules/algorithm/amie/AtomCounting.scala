package com.github.propi.rdfrules.algorithm.amie

import com.github.propi.rdfrules.data.TriplePosition
import com.github.propi.rdfrules.data.TriplePosition.ConceptPosition
import com.github.propi.rdfrules.index.{TripleIndex, TripleItemIndex}
import com.github.propi.rdfrules.rule.{Atom, FreshAtom, InstantiatedAtom, ResolvedAtom}
import com.github.propi.rdfrules.utils.{BasicFunctions, Debugger, Stringifier}
import com.typesafe.scalalogging.Logger

/**
  * Created by Vaclav Zeman on 23. 6. 2017.
  */
trait AtomCounting {

  val logger: Logger = Logger[AtomCounting]

  implicit val tripleIndex: TripleIndex[Int]
  implicit val tripleItemIndex: TripleItemIndex

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
    tripleIndex.predicates.get(atom.predicate) match {
      case Some(tm) =>
        (variableMap.specifyItem(atom.subject), variableMap.specifyItem(atom.`object`)) match {
          case (_: Atom.Variable, _: Atom.Variable) => tm.size(variableMap.injectiveMapping)
          case (_: Atom.Variable, Atom.Constant(oc)) => tm.objects.get(oc).map(_.size(variableMap.injectiveMapping)).getOrElse(0)
          case (Atom.Constant(sc), _: Atom.Variable) => tm.subjects.get(sc).map(_.size(variableMap.injectiveMapping)).getOrElse(0)
          case (_: Atom.Constant, _: Atom.Constant) => 1
        }
      case None => 0
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
    case (_: Atom.Variable, Atom.Constant(oc)) => tripleIndex.objects.get(oc).map(_.size(variableMap.injectiveMapping)).getOrElse(0)
    case (Atom.Constant(sc), _: Atom.Variable) => tripleIndex.subjects.get(sc).map(_.size(variableMap.injectiveMapping)).getOrElse(0)
    case (Atom.Constant(sc), Atom.Constant(oc)) => tripleIndex.subjects.get(sc).flatMap(_.objects.get(oc).map(_.size(variableMap.injectiveMapping))).getOrElse(0)
    case (_: Atom.Variable, _: Atom.Variable) => tripleIndex.size(variableMap.injectiveMapping)
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

  def hasQuasiBinding(atoms: Set[Atom], injectiveMapping: Boolean): Boolean = {
    val hmap = collection.mutable.HashMap.empty[Atom, Atom]
    val allPaths = paths(atoms, VariableMap(injectiveMapping))
    if (allPaths.hasNext) {
      val firstPath = allPaths.next()
      for (atom <- atoms if atom.subject.isInstanceOf[Atom.Constant] || atom.`object`.isInstanceOf[Atom.Constant]) {
        hmap.put(atom, firstPath.specifyAtom(atom))
      }
    }
    while (allPaths.hasNext && hmap.nonEmpty) {
      val variableMap = allPaths.next()
      hmap.filterInPlace { case (atom, lastSpecifiedAtom) =>
        variableMap.specifyAtom(atom) == lastSpecifiedAtom
      }
    }
    hmap.nonEmpty
  }

  /**
    * For input atoms select all instantiated distinct pairs (or sequence) for input variables (headVars)
    *
    * @param atoms        all atoms
    * @param head         head to be instantiated
    * @param variableMaps variable mapping to a concrete constant
    * @param pairFilter   additional filter for each found pair - suitable for PCA confidence
    * @return iterator of instantiated distinct pairs for variables which have covered all atoms
    */
  def selectDistinctPairs(atoms: Set[Atom], head: Atom, variableMaps: Iterator[VariableMap], pairFilter: Option[Seq[Atom.Constant] => Boolean] = None): Iterator[Seq[Atom.Constant]] = {
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

    val (headVars, instantiateHead) = (head.subject, head.`object`) match {
      case (s: Atom.Variable, o: Atom.Variable) =>
        List(s, o) -> ((x: List[Atom.Constant]) => InstantiatedAtom(x.head.value, head.predicate, x.tail.head.value))
      case (s: Atom.Variable, Atom.Constant(o)) =>
        List(s) -> ((x: List[Atom.Constant]) => InstantiatedAtom(x.head.value, head.predicate, o))
      case (Atom.Constant(s), o: Atom.Variable) =>
        List(o) -> ((x: List[Atom.Constant]) => InstantiatedAtom(s, head.predicate, x.head.value))
      case (Atom.Constant(s), Atom.Constant(o)) =>
        Nil -> ((_: List[Atom.Constant]) => InstantiatedAtom(s, head.predicate, o))
    }

    def sdp(atoms: Set[Atom], variableMap: VariableMap): Iterator[Seq[Atom.Constant]] = {
      if (atoms.isEmpty || headVars.forall(variableMap.contains)) {
        //if all variables are mapped then we create an instantiated pair
        val pair = headVars.map(variableMap.apply)
        if (!foundPairs(pair) &&
          pairFilter.forall(_(pair)) &&
          (atoms.isEmpty || exists(atoms, variableMap)) &&
          (!variableMap.injectiveMapping || !variableMap.containsAtom(instantiateHead(pair)))) {
          //if the pair has not been found yet and atoms is empty or there exists a path for remaining atoms then we use this pair
          foundPairs += pair
          Iterator(pair)
        } else {
          //otherwise return empty
          Iterator.empty
        }
      } else {
        //choose best atom for faster computing
        val isLastAtom = atoms.size == 1
        val best = if (isLastAtom) atoms.head else bestAtom(atoms, variableMap)
        val rest = if (isLastAtom) Set.empty[Atom] else atoms - best
        //specify variables in the best atom and process the rest of atoms for each instance
        specifyVariableMap(best, variableMap).flatMap(sdp(rest, _))
      }
    }

    if (atoms.isEmpty) {
      //variant for zero rules: {} => (?a p C)
      variableMaps.filter(variableMap => headVars.forall(variableMap.contains)).flatMap(variableMap => sdp(atoms, variableMap))
    } else {
      variableMaps.flatMap(variableMap => sdp(atoms, variableMap))
    }
  }

  /**
    * For input atoms count all instantiated distinct pairs (or sequence) for input variables in the head atom
    *
    * @param atoms            all atoms
    * @param head             variables to be instantiated in the head
    * @param maxCount         a threshold for stopping counting
    * @param injectiveMapping injective mapping
    * @param pairFilter       additional filter for each found pair - suitable for PCA confidence
    * @return number of distinct pairs for variables which have covered all atoms
    */
  def countDistinctPairs(atoms: Set[Atom], head: Atom, maxCount: Double, injectiveMapping: Boolean, pairFilter: Option[Seq[Atom.Constant] => Boolean] = None)(implicit debugger: Debugger): Int = {
    countDistinctPairs(atoms, head, maxCount, Iterator(VariableMap(injectiveMapping)), pairFilter)
  }

  /**
    * For input atoms count all instantiated distinct pairs (or sequence) for input variables (headVars)
    *
    * @param atoms        all atoms
    * @param head         variables to be instantiated in the head
    * @param maxCount     a threshold for stopping counting
    * @param variableMaps variable mapping to a concrete constant
    * @param pairFilter   additional filter for each found pair - suitable for PCA confidence
    * @return number of distinct pairs for variables which have covered all atoms
    */
  def countDistinctPairs(atoms: Set[Atom], head: Atom, maxCount: Double, variableMaps: Iterator[VariableMap], pairFilter: Option[Seq[Atom.Constant] => Boolean])(implicit debugger: Debugger): Int = {
    var i = 0
    val it = selectDistinctPairs(atoms, head, variableMaps, pairFilter)
    var thresholdTime = System.currentTimeMillis() + 30000
    lazy val atomString = atoms.iterator.map(ResolvedAtom(_)).map(Stringifier(_)).mkString(" ^ ")
    while (it.hasNext && i <= maxCount && !debugger.isInterrupted) {
      it.next()
      i += 1
      if (i % 500 == 0) {
        logger.trace(s"Atom pairs counting, body size: $i (max body size: $maxCount)")
        if (thresholdTime < System.currentTimeMillis()) {
          thresholdTime = System.currentTimeMillis() + 30000
          debugger.logger.info(s"Long counting of body size for: $atomString --- (${BigDecimal(i).toString}${if (maxCount > 0) s" of ${BigDecimal(maxCount).toString} - ${BasicFunctions.round((i / maxCount) * 100, 2)}%" else ""})")
        }
      }
    }
    if (debugger.isInterrupted) 0 else i
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
  def functionality(atom: Atom): Double = tripleIndex.predicates.get(atom.predicate).map(_.subjectRelativeCardinality).getOrElse(0.0)

  /**
    * Inverse of the functionality
    *
    * @param atom atom
    * @return number between 0 and 1
    */
  def inverseFunctionality(atom: Atom): Double = tripleIndex.predicates.get(atom.predicate).map(_.objectRelativeCardinality).getOrElse(0.0)

  /**
    * Create function for unspecified atom which specifies variable map by specified atom
    *
    * @param atom atom to be specified
    * @return function which return variableMap from specified atom which specifies unspecified atom
    */
  def specifyVariableMapForAtom(atom: Atom): (InstantiatedAtom, VariableMap) => VariableMap = (atom.subject, atom.`object`) match {
    case (s: Atom.Variable, o: Atom.Variable) => (specifiedAtom, variableMap) => variableMap + (s -> Atom.Constant(specifiedAtom.subject), atom.predicate, o -> Atom.Constant(specifiedAtom.`object`))
    case (s: Atom.Variable, o: Atom.Constant) => (specifiedAtom, variableMap) => variableMap + (s -> Atom.Constant(specifiedAtom.subject), atom.predicate, o)
    case (s: Atom.Constant, o: Atom.Variable) => (specifiedAtom, variableMap) => variableMap + (s, atom.predicate, o -> Atom.Constant(specifiedAtom.`object`))
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

  def specifyVariableMapAtPosition(position: ConceptPosition, atom: Atom, variableMap: VariableMap): Iterator[VariableMap] = {
    val zeroConstant = Atom.Constant(tripleItemIndex.zero)

    def constantOrZero(item: Atom.Item): Atom.Constant = item match {
      case o: Atom.Constant => o
      case _ => zeroConstant
    }

    position match {
      case TriplePosition.Subject =>
        atom.subject match {
          case _: Atom.Constant => Iterator(variableMap)
          case v: Atom.Variable =>
            val o = constantOrZero(atom.`object`)
            tripleIndex.predicates.get(atom.predicate).iterator.flatMap(_.subjects.iterator).map(s => variableMap + (v -> Atom.Constant(s), atom.predicate, o))
        }
      case TriplePosition.Object =>
        atom.`object` match {
          case _: Atom.Constant => Iterator(variableMap)
          case v: Atom.Variable =>
            val s = constantOrZero(atom.subject)
            tripleIndex.predicates.get(atom.predicate).iterator.flatMap(_.objects.iterator).map(o => variableMap + (v -> s, atom.predicate, Atom.Constant(o)))
        }
    }
  }

  def specifySubject(atom: Atom): Iterator[Atom] = tripleIndex.predicates.get(atom.predicate).iterator.flatMap(_.subjects.iterator).map(subject => atom.transform(subject = Atom.Constant(subject)))

  def specifyObject(atom: Atom): Iterator[Atom] = tripleIndex.predicates.get(atom.predicate).iterator.flatMap(_.objects.iterator).map(`object` => atom.transform(`object` = Atom.Constant(`object`)))

  /**
    * Get all specified atoms (projections) for input atom and variableMap
    *
    * @param atom        atom to be specified
    * @param variableMap constants which will be mapped to variables
    * @return iterator of all projections
    */
  def specifyAtom(atom: Atom, variableMap: VariableMap): Iterator[InstantiatedAtom] = {
    tripleIndex.predicates.get(atom.predicate) match {
      case Some(tm) =>
        (variableMap.specifyItem(atom.subject), variableMap.specifyItem(atom.`object`)) match {
          case (_: Atom.Variable, _: Atom.Variable) =>
            tm.subjects.pairIterator.flatMap(x => x._2.iterator.flatMap { y =>
              val mappedAtom = InstantiatedAtom(x._1, atom.predicate, y)
              if (variableMap.injectiveMapping && (x._1 == y || variableMap.containsConstant(x._1) || variableMap.containsConstant(y) || variableMap.containsAtom(mappedAtom))) {
                None
              } else {
                Some(mappedAtom)
              }
            })
          case (_: Atom.Variable, Atom.Constant(oc)) =>
            tm.objects.get(oc).iterator.flatMap(_.iterator).flatMap { subject =>
              val mappedAtom = InstantiatedAtom(subject, atom.predicate, oc)
              if (variableMap.injectiveMapping && (variableMap.containsConstant(subject) || variableMap.containsAtom(mappedAtom))) {
                None
              } else {
                Some(mappedAtom)
              }
            }
          case (Atom.Constant(sc), _: Atom.Variable) =>
            tm.subjects.get(sc).iterator.flatMap(_.iterator).flatMap { `object` =>
              val mappedAtom = InstantiatedAtom(sc, atom.predicate, `object`)
              if (variableMap.injectiveMapping && (variableMap.containsConstant(`object`) || variableMap.containsAtom(mappedAtom))) {
                None
              } else {
                Some(mappedAtom)
              }
            }
          case (Atom.Constant(sc), Atom.Constant(oc)) =>
            val instantiatedAtom = InstantiatedAtom(sc, atom.predicate, oc)
            if (tm.subjects.get(sc).exists(x => x.contains(oc))) {
              if (variableMap.injectiveMapping && variableMap.containsAtom(instantiatedAtom)) {
                Iterator.empty
              } else {
                Iterator(instantiatedAtom)
              }
            } else {
              Iterator.empty
            }
        }
      case None => Iterator.empty
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
        tripleIndex.predicates.iterator.map(predicate => Atom(sv, predicate, ov))
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
  def getAtomTriples(atom: Atom, injectiveMapping: Boolean): Iterator[(Int, Int)] = specifyAtom(atom, VariableMap(injectiveMapping))
    .map(x => x.subject -> x.`object`)

}

object AtomCounting {

  def apply()(implicit ti: TripleIndex[Int], tii: TripleItemIndex): AtomCounting = new AtomCounting {
    implicit val tripleIndex: TripleIndex[Int] = ti
    implicit val tripleItemIndex: TripleItemIndex = tii
  }

}