package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.data.{Dataset, TripleItem}
import com.github.propi.rdfrules.index._
import com.github.propi.rdfrules.utils.ForEach

/**
  * Created by Vaclav Zeman on 13. 3. 2018.
  */
trait FromDatasetBuildable extends Buildable {

  self: Index =>

  @volatile protected var dataset: Option[Dataset]
  @volatile protected var parent: Option[Index]

  protected def buildTripleIndex: TripleIndex[Int] = {
    val tihi = self.tripleItemMap
    val thi = TripleHashIndex(ForEach.from(dataset).flatMap(_.quads.filter(!_.triple.predicate.hasSameUriAs(TripleItem.sameAs)).flatMap { q =>
      ForEach.from(for {
        s <- tihi.getIndexOpt(q.triple.subject)
        p <- tihi.getIndexOpt(q.triple.predicate)
        o <- tihi.getIndexOpt(q.triple.`object`)
        g <- tihi.getIndexOpt(q.graph)
      } yield {
        IndexItem.Quad(s, p, o, g)
      })
    }))
    dataset = None
    thi
  }

  protected def buildTripleItemIndex: TripleItemIndex = {
    val res = TripleItemHashIndex(dataset.map(_.quads).getOrElse(ForEach.empty), parent.map(_.tripleItemMap))
    parent = None
    res
  }

  protected def buildAll: (TripleItemIndex, TripleIndex[Int]) = {
    val res = TripleItemHashIndex.mapQuads(dataset.map(_.quads).getOrElse(ForEach.empty), parent.map(_.tripleItemMap)) { mappedQuads =>
      TripleHashIndex(mappedQuads)
    }
    dataset = None
    parent = None
    res
  }

}