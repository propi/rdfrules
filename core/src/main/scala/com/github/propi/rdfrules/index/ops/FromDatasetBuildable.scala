package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.data.{Dataset, TripleItem}
import com.github.propi.rdfrules.index._

/**
  * Created by Vaclav Zeman on 13. 3. 2018.
  */
trait FromDatasetBuildable extends Buildable {

  self: Index =>

  @volatile protected var dataset: Option[Dataset]

  protected def buildTripleIndex: TripleIndex[Int] = self.tripleItemMap { implicit tihi =>
    val thi = TripleHashIndex(dataset.map(_.quads.filter(!_.triple.predicate.hasSameUriAs(TripleItem.sameAs)).flatMap { q =>
      for {
        s <- tihi.getIndexOpt(q.triple.subject)
        p <- tihi.getIndexOpt(q.triple.predicate)
        o <- tihi.getIndexOpt(q.triple.`object`)
        g <- tihi.getIndexOpt(q.graph)
      } yield {
        IndexItem.Quad(s, p, o, g)
      }
    }).getOrElse(Nil))
    dataset = None
    thi
  }

  protected def buildTripleItemIndex: TripleItemIndex = TripleItemHashIndex(dataset.map(_.quads).getOrElse(Nil))

  protected def buildAll: (TripleItemIndex, TripleIndex[Int]) = {
    TripleItemHashIndex.mapQuads(dataset.map(_.quads).getOrElse(Nil)) { mappedQuads =>
      TripleHashIndex(mappedQuads)
    }
  }

}