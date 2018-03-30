package eu.easyminer.rdf.index.ops

import eu.easyminer.rdf.index.{TripleHashIndex, TripleItemHashIndex}

/**
  * Created by Vaclav Zeman on 13. 3. 2018.
  */
trait Buildable {

  protected def buildTripleHashIndex: TripleHashIndex

  protected def buildTripleItemHashIndex: TripleItemHashIndex

}
