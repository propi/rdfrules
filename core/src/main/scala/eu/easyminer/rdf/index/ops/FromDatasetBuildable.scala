package eu.easyminer.rdf.index.ops

import eu.easyminer.rdf.index.{Index, TripleHashIndex, TripleItemHashIndex}

/**
  * Created by Vaclav Zeman on 13. 3. 2018.
  */
trait FromDatasetBuildable extends Buildable {

  self: Index =>

  protected def buildTripleHashIndex: TripleHashIndex = self.tripleItemMap { implicit tihi =>
    TripleHashIndex(toDataset.quads)
  }

  protected def buildTripleItemHashIndex: TripleItemHashIndex = TripleItemHashIndex(toDataset.quads.flatMap { quad =>
    List(quad.graph, quad.triple.subject, quad.triple.predicate, quad.triple.`object`)
  })

}
