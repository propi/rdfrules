package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.data.{Dataset, Quad}
import com.github.propi.rdfrules.index.{CompressedQuad, Index}

/**
  * Created by Vaclav Zeman on 12. 3. 2018.
  */
trait QuadsIndex {

  self: Index =>

  protected def compressedQuads: Traversable[CompressedQuad] = new Traversable[CompressedQuad] {
    def foreach[U](f: CompressedQuad => U): Unit = self.tripleMap { thi =>
      for {
        (p, m1) <- thi.predicates.iterator
        (s, m2) <- m1.subjects.iterator
        (o, m3) <- m2.iterator
        g <- m3.iterator
      } {
        f(CompressedQuad(s, p, o, g))
      }
    }
  }

  def toDataset: Dataset = Dataset(
    new Traversable[Quad] {
      def foreach[U](f: Quad => U): Unit = {
        self.tripleItemMap { implicit mapper =>
          compressedQuads.foreach(x => f(x.toQuad))
        }
      }
    }.view
  )

}