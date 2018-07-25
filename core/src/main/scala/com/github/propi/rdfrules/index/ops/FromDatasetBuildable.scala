package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.index.{Index, TripleHashIndex, TripleItemHashIndex}

/**
  * Created by Vaclav Zeman on 13. 3. 2018.
  */
trait FromDatasetBuildable extends Buildable {

  self: Index =>

  protected def buildTripleHashIndex: TripleHashIndex = self.tripleItemMap { implicit tihi =>
    TripleHashIndex(toDataset.quads)
  }

  protected def buildTripleItemHashIndex: TripleItemHashIndex = TripleItemHashIndex(toDataset.quads)

}
