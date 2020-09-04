package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.data.{Dataset, Quad}
import com.github.propi.rdfrules.index.{Index, IndexItem}

/**
  * Created by Vaclav Zeman on 12. 3. 2018.
  */
trait QuadsIndex {

  self: Index =>

  protected def compressedQuads: Traversable[IndexItem.IntQuad] = new Traversable[IndexItem.IntQuad] {
    def foreach[U](f: IndexItem.IntQuad => U): Unit = self.tripleMap { thi =>
      thi.quads.foreach(f)
    }
  }

  def toDataset: Dataset = Dataset(
    new Traversable[Quad] {
      def foreach[U](f: Quad => U): Unit = {
        self.tripleItemMap { implicit mapper =>
          compressedQuads.foreach(x => f(x.toQuad))
        }
      }
    }.view,
    true
  )

}