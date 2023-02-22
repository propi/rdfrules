package com.github.propi.rdfrules.index

sealed trait IndexContainer {
  def main: Index

  final def tripleMap: TripleIndex[Int] = main.tripleMap

  final def tripleItemMap: TripleItemIndex = main.tripleItemMap
}

object IndexContainer {

  case class Single(index: Index) extends IndexContainer {
    def main: Index = index
  }

  case class TrainTest(trainTestIndex: TrainTestIndex) extends IndexContainer {
    def main: Index = trainTestIndex.train
  }

}