package eu.easyminer.rdf.rule

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
case class RulePattern private(antecedent: List[AtomPattern], consequent: AtomPattern) {

  def +(atom: AtomPattern) = {
    val variables = (consequent :: antecedent).flatMap(x => List(x.subject, x.predicate)).collect { case x: Atom.Variable => x }
    val isConnected = List(atom.subject, atom.`object`).collect { case x: Atom.Variable => x }.exists(variables.contains)
    if (isConnected) this.copy(antecedent = atom :: antecedent) else this
  }

}

case class AtomPattern(subject: Atom.Item, predicate: Option[String], `object`: Atom.Item)

object RulePattern {

  def apply(consequent: AtomPattern): RulePattern = RulePattern(Nil, consequent)

}
