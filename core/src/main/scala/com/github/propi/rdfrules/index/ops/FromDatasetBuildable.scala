package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.data.{Dataset, TripleItem}
import com.github.propi.rdfrules.index.TripleHashIndex.IndexItem
import com.github.propi.rdfrules.index.{Index, TripleHashIndex, TripleItemHashIndex}

/**
  * Created by Vaclav Zeman on 13. 3. 2018.
  */
trait FromDatasetBuildable extends Buildable {

  self: Index =>

  @volatile protected var dataset: Option[Dataset]

  protected def buildTripleHashIndex: TripleHashIndex[Int] = self.tripleItemMap { implicit tihi =>
    val thi = TripleHashIndex(dataset.map(_.quads.filter(!_.triple.predicate.hasSameUriAs(TripleItem.sameAs)).flatMap { q =>
      for {
        s <- tihi.getIndexOpt(q.triple.subject)
        p <- tihi.getIndexOpt(q.triple.predicate)
        o <- tihi.getIndexOpt(q.triple.`object`)
        g <- tihi.getIndexOpt(q.graph)
      } yield {
        new IndexItem.Quad[Int](s, p, o, g)
      }
    }).getOrElse(Nil))
    dataset = None
    thi
  }

  protected def buildTripleItemHashIndex: TripleItemHashIndex = TripleItemHashIndex(dataset.map(_.quads).getOrElse(Nil))

  protected def buildAll: (TripleItemHashIndex, TripleHashIndex[Int]) = {
    val tihi = TripleItemHashIndex.empty
    TripleHashIndex()
    dataset.map(dataset => new Traversable[IndexItem[Int]] {
      def foreach[U](f: IndexItem[Int] => U): Unit = {
        for (quad <- dataset.quads) {
          tihi.addQuad(quad)
        }
      }
    })
  }
}
