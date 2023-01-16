package com.github.propi.rdfrules.index

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.utils.Debugger

sealed trait TrainTestIndex {
  def testIsTrain: Boolean

  def train: Index

  def test: Index

  def merged: Index
}

object TrainTestIndex {

  private class OneIndex(val train: Index) extends TrainTestIndex {
    def testIsTrain: Boolean = true

    def test: Index = train

    def merged: Index = train
  }

  private class TwoIndexes(val train: Index, val test: Index, _merged: => Index) extends TrainTestIndex {
    lazy val merged: Index = _merged

    def testIsTrain: Boolean = false
  }

  def apply(train: Index): TrainTestIndex = new OneIndex(train)

  def apply(train: Index, test: Dataset)(implicit debugger: Debugger): TrainTestIndex = {
    val testIndex = Index(test, train, false)
    new TwoIndexes(train, testIndex, Index(MergedTripleIndex(train.tripleMap, testIndex.tripleMap), testIndex.tripleItemMap))
  }
}