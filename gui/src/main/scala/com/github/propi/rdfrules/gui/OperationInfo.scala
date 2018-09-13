package com.github.propi.rdfrules.gui

import com.github.propi.rdfrules.gui.operations._
import com.thoughtworks.binding.Binding.Constants

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
sealed trait OperationInfo {
  val name: String
  val title: String
  val `type`: Operation.Type
  val followingOperations: Constants[OperationInfo]

  def buildOperation(from: Operation): Operation
}

object OperationInfo {

  sealed trait Transformation extends OperationInfo {
    val `type`: Operation.Type = Operation.Type.Transformation
  }

  sealed trait DatasetTransformation extends Transformation {
    val followingOperations: Constants[OperationInfo] = Constants(
      LoadGraph,
      LoadDataset,
      MergeDatasets,
      MapQuads,
      FilterQuads,
      TakeQuads,
      DropQuads,
      SliceQuads
    )
  }

  object Root extends Transformation {
    val name: String = "root"
    val title: String = ""
    val followingOperations: Constants[OperationInfo] = Constants(LoadGraph, LoadDataset)

    def buildOperation(from: Operation): Operation = new Root
  }

  object LoadGraph extends DatasetTransformation {
    val name: String = "LoadGraph"
    val title: String = "Load graph"

    def buildOperation(from: Operation): Operation = new LoadGraph(from)
  }

  object LoadDataset extends DatasetTransformation {
    val name: String = "LoadDataset"
    val title: String = "Load dataset"

    def buildOperation(from: Operation): Operation = new LoadDataset(from)
  }

  object MergeDatasets extends DatasetTransformation {
    val name: String = "MergeDatasets"
    val title: String = "Merge datasets"

    def buildOperation(from: Operation): Operation = new MergeDatasets(from)
  }

  object MapQuads extends DatasetTransformation {
    val name: String = "MapQuads"
    val title: String = "Map quads"

    def buildOperation(from: Operation): Operation = new MapQuads(from)
  }

  object FilterQuads extends DatasetTransformation {
    val name: String = "FilterQuads"
    val title: String = "Filter quads"

    def buildOperation(from: Operation): Operation = new FilterQuads(from)
  }

  object TakeQuads extends DatasetTransformation {
    val name: String = "TakeQuads"
    val title: String = "Take"

    def buildOperation(from: Operation): Operation = new TakeQuads(from)
  }

  object DropQuads extends DatasetTransformation {
    val name: String = "DropQuads"
    val title: String = "Drop"

    def buildOperation(from: Operation): Operation = new DropQuads(from)
  }

  object SliceQuads extends DatasetTransformation {
    val name: String = "SliceQuads"
    val title: String = "Slice"

    def buildOperation(from: Operation): Operation = new SliceQuads(from)
  }

}