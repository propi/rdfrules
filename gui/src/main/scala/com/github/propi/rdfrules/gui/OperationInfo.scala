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

  sealed trait Action extends OperationInfo {
    val `type`: Operation.Type = Operation.Type.Action
    val followingOperations: Constants[OperationInfo] = Constants()
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
      SliceQuads,
      DiscretizeEqualDistance,
      DiscretizeEqualFrequency,
      DiscretizeEqualSize,
      CacheDataset,
      Index,
      CacheDatasetAction,
      ExportQuads,
      GetQuads,
      DatasetSize,
      Types,
      Histogram
    )
  }

  sealed trait IndexTransformation extends Transformation {
    val followingOperations: Constants[OperationInfo] = Constants()
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

  object DiscretizeEqualFrequency extends DatasetTransformation {
    val name: String = "Discretize"
    val title: String = "Discretize (equal frequency)"

    def buildOperation(from: Operation): Operation = new DiscretizeEqualFrequency(from)
  }

  object DiscretizeEqualSize extends DatasetTransformation {
    val name: String = "Discretize"
    val title: String = "Discretize (equal size)"

    def buildOperation(from: Operation): Operation = new DiscretizeEqualSize(from)
  }

  object DiscretizeEqualDistance extends DatasetTransformation {
    val name: String = "Discretize"
    val title: String = "Discretize (equal distance)"

    def buildOperation(from: Operation): Operation = new DiscretizeEqualDistance(from)
  }

  object CacheDataset extends DatasetTransformation {
    val name: String = "CacheDataset"
    val title: String = "Cache"

    def buildOperation(from: Operation): Operation = new CacheDataset(from)
  }

  object Index extends IndexTransformation {
    val name: String = "Index"
    val title: String = "Index"

    def buildOperation(from: Operation): Operation = new Index(from)
  }

  object CacheDatasetAction extends Action {
    val name: String = "CacheDataset"
    val title: String = "Cache"

    def buildOperation(from: Operation): Operation = new actions.CacheDataset(from)
  }

  object ExportQuads extends Action {
    val name: String = "ExportQuads"
    val title: String = "Export"

    def buildOperation(from: Operation): Operation = new actions.ExportQuads(from)
  }

  object GetQuads extends Action {
    val name: String = "GetQuads"
    val title: String = "Get quads"

    def buildOperation(from: Operation): Operation = new actions.GetQuads(from)
  }

  object DatasetSize extends Action {
    val name: String = "DatasetSize"
    val title: String = "Size"

    def buildOperation(from: Operation): Operation = new actions.DatasetSize(from)
  }

  object Types extends Action {
    val name: String = "Types"
    val title: String = "Types"

    def buildOperation(from: Operation): Operation = new actions.Types(from)
  }

  object Histogram extends Action {
    val name: String = "Histogram"
    val title: String = "Histogram"

    def buildOperation(from: Operation): Operation = new actions.Histogram(from)
  }

}