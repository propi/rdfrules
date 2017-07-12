package eu.easyminer.rdf.rule

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
case class RulePattern private(antecedent: IndexedSeq[AtomPattern], consequent: AtomPattern) {

  private def atomIsValid(atom: AtomPattern) = {
    val variableComparator = implicitly[Ordering[Atom.Variable]]
    val patternVariables = (Iterator(consequent) ++ antecedent.iterator).flatMap(x => Iterator(x.subject, x.`object`)).collect { case x: Atom.Variable => x }.toSet
    val atomVariables = Iterator(atom.subject, atom.`object`).collect { case x: Atom.Variable => x }.toSet
    val isConnected = atomVariables.exists(patternVariables.apply)
    atom.subject != atom.`object` && isConnected && variableComparator.lteq(atomVariables.max, patternVariables.max.++)
  }

  def +(atom: AtomPattern) = if (atomIsValid(atom)) this.copy(antecedent = antecedent :+ atom) else this

}

object RulePattern {

  def apply(consequent: AtomPattern): RulePattern = {
    val items = List(consequent.subject, consequent.`object`)
    val variables = items.collect{ case x: Atom.Variable => x }
    val maxVariable = variables.iterator.map(_.index).max
    if (variables.isEmpty || items.distinct.size != items.size || maxVariable > 0 && variables.size == 1 || maxVariable > 1 && variables.size == 2) {
      throw new IllegalArgumentException
    }
    RulePattern(Vector.empty, consequent)
  }

}

case class AtomPattern(subject: Atom.Item, predicate: Option[Int], `object`: Atom.Item)
