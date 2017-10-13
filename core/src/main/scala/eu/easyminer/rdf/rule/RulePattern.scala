package eu.easyminer.rdf.rule

import eu.easyminer.rdf.data.TripleItem

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

  def +(atom: AtomPattern): RulePattern = if (atomIsValid(atom)) this.copy(antecedent = atom +: antecedent) else this

}

object RulePattern {

  def apply(consequent: AtomPattern): RulePattern = {
    val items = List(consequent.subject, consequent.`object`)
    val variables = items.collect { case x: Atom.Variable => x }
    val maxVariable = variables.iterator.map(_.index).max
    if (variables.isEmpty || items.distinct.size != items.size || maxVariable > 0 && variables.size == 1 || maxVariable > 1 && variables.size == 2) {
      throw new IllegalArgumentException
    }
    RulePattern(Vector.empty, consequent)
  }

  def apply(string: String)(implicit mapper: TripleItem => Int, stringToTripleItem: String => Option[TripleItem]): Option[RulePattern] = {
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
  }

}