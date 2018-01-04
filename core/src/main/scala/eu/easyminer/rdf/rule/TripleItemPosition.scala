package eu.easyminer.rdf.rule

/**
  * Created by Vaclav Zeman on 2. 1. 2018.
  */
sealed trait TripleItemPosition {
  val item: Atom.Item
}

object TripleItemPosition {

  case class Subject(item: Atom.Item) extends TripleItemPosition

  case class Object(item: Atom.Item) extends TripleItemPosition

}