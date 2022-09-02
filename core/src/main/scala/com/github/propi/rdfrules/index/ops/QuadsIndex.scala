package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.index.{Index, IndexItem, TripleItemIndex}
import com.github.propi.rdfrules.utils.ForEach

/**
  * Created by Vaclav Zeman on 12. 3. 2018.
  */
trait QuadsIndex {

  self: Index =>

  protected def compressedQuads: ForEach[IndexItem.IntQuad] = new ForEach[IndexItem.IntQuad] {
    def foreach(f: IndexItem.IntQuad => Unit): Unit = self.tripleMap.quads.foreach(f)

    override lazy val knownSize: Int = self.tripleMap.size(false)
  }

  def toDataset: Dataset = {
    implicit val mapper: TripleItemIndex = tripleItemMap
    Dataset(compressedQuads.map(_.toQuad))
  }

}