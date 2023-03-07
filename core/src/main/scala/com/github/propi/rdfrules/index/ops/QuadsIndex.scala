package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.index.{IndexPart, IndexItem, TripleItemIndex}
import com.github.propi.rdfrules.utils.ForEach

/**
  * Created by Vaclav Zeman on 12. 3. 2018.
  */
trait QuadsIndex {

  self: IndexPart =>

  def toDataset: Dataset = {
    implicit val mapper: TripleItemIndex = tripleItemMap
    Dataset(self.compressedQuads.map(_.toQuad))
  }

}

object QuadsIndex {

  implicit class PimpedQuadsIndex(val indexPart: IndexPart) extends AnyVal {
    def compressedQuads: ForEach[IndexItem.IntQuad] = new ForEach[IndexItem.IntQuad] {
      def foreach(f: IndexItem.IntQuad => Unit): Unit = indexPart.tripleMap.quads.foreach(f)

      override lazy val knownSize: Int = indexPart.tripleMap.size(false)
    }
  }

}