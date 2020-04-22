package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.data.{Dataset, TripleItem}
import com.github.propi.rdfrules.index.{Index, TripleHashIndex, TripleItemHashIndex}

/**
  * Created by Vaclav Zeman on 13. 3. 2018.
  */
trait FromDatasetBuildable extends Buildable {

  self: Index =>

  protected val dataset: Dataset

  protected def buildTripleHashIndex: TripleHashIndex[Int] = self.tripleItemMap { implicit tihi =>
    TripleHashIndex(dataset.quads.filter(!_.triple.predicate.hasSameUriAs(TripleItem.sameAs)).map(q => new TripleHashIndex.Quad(
      tihi.getIndex(q.triple.subject),
      tihi.getIndex(q.triple.predicate),
      tihi.getIndex(q.triple.`object`),
      tihi.getIndex(q.graph)
    )))
  }

  protected def buildTripleItemHashIndex: TripleItemHashIndex = TripleItemHashIndex(dataset.quads)

}
