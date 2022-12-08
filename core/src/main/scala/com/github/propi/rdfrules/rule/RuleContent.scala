package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.rule.RuleContent.checkRuleContentsEquality

trait RuleContent {
  val body: IndexedSeq[Atom]
  val head: Atom

  def bodySet: Set[Atom] = body.toSet

  def ruleLength: Int = body.size + 1

  def isClosed: Boolean = {
    val hmap = collection.mutable.HashMap.empty[Int, Int]
    for (Atom.Variable(i) <- (Iterator(head) ++ body.iterator).flatMap(x => Iterator(x.subject, x.`object`))) {
      hmap.updateWith(i)(_.map(_ + 1).orElse(Some(1)))
    }
    hmap.valuesIterator.forall(_ > 1)
  }

  def parents: Iterator[RuleContent] = body.iterator.map { atom =>
    RuleContent(body.filter(_ != atom), head)
  }

  private def atomItemHashCode(atomItem: Atom.Item): Int = atomItem match {
    case constant: Atom.Constant => constant.value
    case _ => 0
  }

  private def atomHashCode(atom: Atom): Int = atom.predicate + atomItemHashCode(atom.subject) + atomItemHashCode(atom.`object`)

  override def hashCode(): Int = body.iterator.map(atomHashCode).foldLeft(atomHashCode(head))(_ ^ _) * ruleLength * 31

  override def equals(obj: Any): Boolean = obj match {
    case x: RuleContent => ruleLength == x.ruleLength && checkRuleContentsEquality(body, x.bodySet, head, x.head)
    case _ => false
  }
}

object RuleContent {

  private case class BasicRuleContent(body: IndexedSeq[Atom], head: Atom) extends RuleContent

  def apply(body: IndexedSeq[Atom], head: Atom): RuleContent = BasicRuleContent(body, head)

  /**
    * Check two items
    *
    * @param atomItem1      atomItem1
    * @param atomItem2      atomItem2
    * @param variables      mapping for second rule
    * @param variablesItems variables which are objects of mapping
    * @return
    */
  private def checkAtomItemsEquality(atomItem1: Atom.Item, atomItem2: Atom.Item, variables: Map[Atom.Variable, Atom.Variable], variablesItems: Set[Atom.Variable]) = (atomItem1, atomItem2) match {
    case (Atom.Constant(value1), Atom.Constant(value2)) => (value1 == value2, variables, variablesItems)
    case (v1: Atom.Variable, v2: Atom.Variable) => variables.get(v2) match {
      //if item of the second rule is mapped then we return the object of mapping and we compare it with first rule item
      case Some(v2) => (v2 == v1, variables, variablesItems)
      //if item of the second rule is not mapped and the first rule item is some object of mapping we can not map the second item - it is duplicit mapping
      //e.g. variables(c -> b), variablesItems(b), we compare b with d, we can not map (d -> b), because b is also the object of mapping
      //in this case the comparison is always false
      case None if variablesItems(v1) => (false, variables, variablesItems)
      //if item of the second rule is not mapped and the first rule item is not some object of mapping we map second item to first item
      case _ => (true, variables + (v2 -> v1), variablesItems + v1)
    }
    case _ => (false, variables, variablesItems)
  }

  private def checkAtomEquality(atom1: Atom, atom2: Atom, variables: Map[Atom.Variable, Atom.Variable], variablesItems: Set[Atom.Variable]) = {
    if (atom1.predicate == atom2.predicate) {
      val (eqSubjects, v1, vi1) = checkAtomItemsEquality(atom1.subject, atom2.subject, variables, variablesItems)
      if (eqSubjects) {
        val (eqObjects, v2, vi2) = checkAtomItemsEquality(atom1.`object`, atom2.`object`, v1, vi1)
        (eqObjects, v2, vi2)
      } else {
        (false, variables, variablesItems)
      }
    } else {
      (false, variables, variablesItems)
    }
  }

  private def checkBodyEquality(body1: IndexedSeq[Atom], body2: Set[Atom], variables: Map[Atom.Variable, Atom.Variable], variablesItems: Set[Atom.Variable]): Boolean = {
    body1 match {
      case head +: tail =>
        body2.exists { atom =>
          val (eqAtoms, newVariables, newVariablesItems) = checkAtomEquality(head, atom, variables, variablesItems)
          eqAtoms && checkBodyEquality(tail, body2 - atom, newVariables, newVariablesItems)
        }
      case _ => body2.isEmpty
    }
  }

  private def checkPartialBodyEquality(bodyParent: IndexedSeq[Atom], bodyChild: Set[Atom], variables: Map[Atom.Variable, Atom.Variable], variablesItems: Set[Atom.Variable]): Boolean = {
    bodyParent match {
      case head +: tail if bodyChild.nonEmpty =>
        bodyChild.exists { atom =>
          val (eqAtoms, newVariables, newVariablesItems) = checkAtomEquality(head, atom, variables, variablesItems)
          eqAtoms && checkPartialBodyEquality(tail, bodyChild - atom, newVariables, newVariablesItems)
        }
      case _ => true
    }
  }

  /**
    * Check whether two lists of atoms are isomorphic
    *
    * @param body1 first list of body atoms
    * @param body2 second list of body atoms
    * @param head1 first head
    * @param head2 second head
    * @return boolean
    */
  def checkRuleContentsEquality(body1: IndexedSeq[Atom], body2: Set[Atom], head1: Atom, head2: Atom): Boolean = {
    val (eqHeads, newVariables, newVariablesItems) = checkAtomEquality(head1, head2, Map.empty, Set.empty)
    eqHeads && checkBodyEquality(body1, body2, newVariables, newVariablesItems)
  }

  /**
    * Check parenthood
    *
    * @param bodyParent first list of body atoms
    * @param bodyChild  second list of body atoms
    * @param headParent first head
    * @param headChild  second head
    * @return boolean
    */
  def checkParenthood(bodyParent: IndexedSeq[Atom], bodyChild: Set[Atom], headParent: Atom, headChild: Atom): Boolean = {
    val (eqHeads, newVariables, newVariablesItems) = checkAtomEquality(headParent, headChild, Map.empty, Set.empty)
    eqHeads && checkPartialBodyEquality(bodyParent, bodyChild, newVariables, newVariablesItems)
  }

}