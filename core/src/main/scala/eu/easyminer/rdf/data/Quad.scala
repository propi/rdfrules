package eu.easyminer.rdf.data

import scala.collection.TraversableView

/**
  * Created by Vaclav Zeman on 15. 1. 2018.
  */
case class Quad(triple: Triple, graph: TripleItem.Uri)

object Quad {

  type QuadTraversableView = TraversableView[Quad, Traversable[_]]

}
