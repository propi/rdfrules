package eu.easyminer.rdf.rule

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
case class RulePattern private(antecedent: IndexedSeq[AtomPattern], consequent: Option[AtomPattern], exact: Boolean) {

  def ::(atom: AtomPattern): RulePattern = this.copy(antecedent = atom +: antecedent)

}

object RulePattern {

  def apply(consequent: Option[AtomPattern], exact: Boolean): RulePattern = RulePattern(Vector.empty, consequent, exact)

  def apply(consequent: AtomPattern, exact: Boolean): RulePattern = apply(Some(consequent), exact)

  def apply(consequent: AtomPattern): RulePattern = apply(consequent, false)

}