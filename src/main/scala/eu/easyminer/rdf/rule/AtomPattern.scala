package eu.easyminer.rdf.rule

/**
  * Created by Vaclav Zeman on 23. 9. 2017.
  */
case class AtomPattern(subject: Atom.Item, predicate: Option[Int], `object`: Atom.Item)