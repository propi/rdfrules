package com.github.propi.rdfrules.index

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.index.IndexCollections.TypedCollectionsBuilder
import com.github.propi.rdfrules.utils.Debugger

sealed trait TrainTestIndex {
  def testIsTrain: Boolean

  def train: Index

  def test: Index
}

object TrainTestIndex {

  private class OneIndex(val train: Index) extends TrainTestIndex {
    def testIsTrain: Boolean = true

    def test: Index = train
  }

  private class TwoIndexes(val train: Index, val test: Index) extends TrainTestIndex {
    def testIsTrain: Boolean = false
  }

  def apply(train: Index): TrainTestIndex = new OneIndex(train)

  def apply(train: Index, test: Dataset)(implicit debugger: Debugger, collectionsBuilder: TypedCollectionsBuilder[Int]): TrainTestIndex = new TwoIndexes(train, Index(test, train, false))
}