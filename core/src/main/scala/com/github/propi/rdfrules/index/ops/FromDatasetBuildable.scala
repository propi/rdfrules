package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.index.{Index, TripleHashIndex, TripleItemHashIndex}

/**
  * Created by Vaclav Zeman on 13. 3. 2018.
  */
trait FromDatasetBuildable extends Buildable {

  self: Index =>

  protected def buildTripleHashIndex: TripleHashIndex[Int] = self.tripleItemMap { implicit tihi =>
    TripleHashIndex(toDataset.quads.map(q => new TripleHashIndex.Quad(
      tihi.getIndex(q.triple.subject),
      tihi.getIndex(q.triple.predicate),
      tihi.getIndex(q.triple.`object`),
      tihi.getIndex(q.graph)
    )))
  }

  protected def buildTripleItemHashIndex: TripleItemHashIndex = TripleItemHashIndex(toDataset.quads)

}
