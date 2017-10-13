package eu.easyminer.rdf.rule

import eu.easyminer.rdf.data.TripleItem

/**
  * Created by Vaclav Zeman on 23. 9. 2017.
  */
case class AtomPattern(subject: Atom.Item, predicate: Option[Int], `object`: Atom.Item)

object AtomPattern {

  sealed trait AtomItemPattern

  object AtomItemPattern {

    case class Variable(variable: Atom.Variable)

    case class Constant(tripleItem: TripleItem)

  }

  def apply(subject: AtomItemPattern, predicate: Option[TripleItem.Uri], `object`: AtomItemPattern)
           (implicit mapper: TripleItem => Int): AtomPattern = {
    def aipToAi(x: AtomItemPattern): Atom.Item = x match {
      case AtomItemPattern.Variable(v) => v
      case AtomItemPattern.Constant(tripleItem) => Atom.Constant(mapper(tripleItem))
    }

    AtomPattern(aipToAi(subject), predicate.map(x => mapper(x)), aipToAi(`object`))
  }

}