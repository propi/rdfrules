package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.index.{IndexCollections, TripleIndex, TripleItemIndex}

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 13. 3. 2018.
  */
trait Buildable {

  implicit protected def collectionBuilders: IndexCollections.TypedCollectionsBuilder[Int] = CollectionBuilders.intCollectionBuilder

  protected def buildTripleIndex: TripleIndex[Int]

  protected def buildTripleItemIndex: TripleItemIndex

  protected def buildAll: (TripleItemIndex, TripleIndex[Int])

}