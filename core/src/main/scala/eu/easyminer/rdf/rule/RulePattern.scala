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

  /*def apply(string: String)(implicit mapper: TripleItem => Int, stringToTripleItem: String => Option[TripleItem]): Option[RulePattern] = {
    def stripItem(s: String) = s.stripPrefix("\"").stripSuffix("\"")

    def createAtomPattern(s: String, p: String, o: String) = {
      (AtomPattern.AtomItemPattern(stripItem(s)), if (p == "?") None else Some(stringToTripleItem(p)), AtomPattern.AtomItemPattern(stripItem(o))) match {
        case (Some(s: AtomPattern.AtomItemPattern), p, Some(o: AtomPattern.AtomItemPattern)) if p.isEmpty || p.exists(_.exists(_.isInstanceOf[TripleItem.Uri])) =>
          Some(AtomPattern(s, p.flatten.collect { case x: TripleItem.Uri => x }, o))
        case _ =>
          None
      }
    }

    val BodyHead = "(.*)\\s+->\\s+(.*)".r
    val AtomParts = "(\".+?\"|\\S+)\\s+(\\S+)\\s+(\".+?\"|\\S+)".r
    Some(string).collect {
      case BodyHead(body, head) => Some(head.trim).collect {
        case AtomParts(s, p, o) => createAtomPattern(s, p, o).map { headAtomPattern =>
          body.split('^').reverseIterator.map(_.trim).collect {
            case AtomParts(s, p, o) => createAtomPattern(s, p, o)
          }.flatten.foldLeft(apply(headAtomPattern))(_ + _)
        }
      }.flatten
    }.flatten
  }*/

}