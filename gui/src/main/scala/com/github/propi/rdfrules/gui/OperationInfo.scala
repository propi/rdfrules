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
      AddPrefixes,
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
      Prefixes,
      DatasetSize,
      Types,
      Histogram
    )
  }

  sealed trait IndexTransformation extends Transformation {
    val followingOperations: Constants[OperationInfo] = Constants(CacheIndex, ToDataset, Mine, CacheIndexAction, LoadRuleset)
  }

  sealed trait RulesetTransformation extends Transformation {
    val followingOperations: Constants[OperationInfo] = Constants(FilterRules, TakeRules, DropRules, SliceRules, Sorted, Sort, ComputeConfidence, ComputePcaConfidence, ComputeLift, MakeClusters, GraphBasedRules, CacheRuleset, CacheRulesetAction, ExportRules, GetRules, RulesetSize)
  }

  object Root extends Transformation {
    val name: String = "root"
    val title: String = ""
    val followingOperations: Constants[OperationInfo] = Constants(LoadGraph, LoadDataset, LoadIndex)

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

  object LoadIndex extends IndexTransformation {
    val name: String = "LoadIndex"
    val title: String = "Load index"

    def buildOperation(from: Operation): Operation = new LoadIndex(from)
  }

  object LoadRuleset extends RulesetTransformation {
    val name: String = "LoadRuleset"
    val title: String = "Load ruleset"

    def buildOperation(from: Operation): Operation = new LoadRuleset(from)
  }

  object AddPrefixes extends DatasetTransformation {
    val name: String = "AddPrefixes"
    val title: String = "Add prefixes"

    def buildOperation(from: Operation): Operation = new AddPrefixes(from)
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

  object Prefixes extends Action {
    val name: String = "Prefixes"
    val title: String = "Get prefixes"

    def buildOperation(from: Operation): Operation = new actions.Prefixes(from)
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

  object Index extends IndexTransformation {
    val name: String = "Index"
    val title: String = "Index"

    def buildOperation(from: Operation): Operation = new Index(from)
  }

  object CacheIndex extends IndexTransformation {
    val name: String = "CacheIndex"
    val title: String = "Cache"

    def buildOperation(from: Operation): Operation = new CacheIndex(from)
  }

  object CacheIndexAction extends Action {
    val name: String = "CacheIndex"
    val title: String = "Cache"

    def buildOperation(from: Operation): Operation = new actions.CacheIndex(from)
  }

  object ToDataset extends DatasetTransformation {
    val name: String = "ToDataset"
    val title: String = "To dataset"

    def buildOperation(from: Operation): Operation = new ToDataset(from)
  }

  object Mine extends RulesetTransformation {
    val name: String = "Mine"
    val title: String = "Mine"

    def buildOperation(from: Operation): Operation = new Mine(from)
  }

  object FilterRules extends RulesetTransformation {
    val name: String = "FilterRules"
    val title: String = "Filter"

    def buildOperation(from: Operation): Operation = new FilterRules(from)
  }

  object TakeRules extends RulesetTransformation {
    val name: String = "TakeRules"
    val title: String = "Take"

    def buildOperation(from: Operation): Operation = new TakeRules(from)
  }

  object DropRules extends RulesetTransformation {
    val name: String = "DropRules"
    val title: String = "Drop"

    def buildOperation(from: Operation): Operation = new DropRules(from)
  }

  object SliceRules extends RulesetTransformation {
    val name: String = "SliceRules"
    val title: String = "Slice"

    def buildOperation(from: Operation): Operation = new SliceRules(from)
  }

  object Sorted extends RulesetTransformation {
    val name: String = "Sorted"
    val title: String = "Sorted"

    def buildOperation(from: Operation): Operation = new Sorted(from)
  }

  object Sort extends RulesetTransformation {
    val name: String = "Sort"
    val title: String = "Sort"

    def buildOperation(from: Operation): Operation = new Sort(from)
  }

  object ComputeConfidence extends RulesetTransformation {
    val name: String = "ComputeConfidence"
    val title: String = "Compute confidence"

    def buildOperation(from: Operation): Operation = new ComputeConfidence(from)
  }

  object ComputePcaConfidence extends RulesetTransformation {
    val name: String = "ComputePcaConfidence"
    val title: String = "Compute PCA confidence"

    def buildOperation(from: Operation): Operation = new ComputePcaConfidence(from)
  }

  object ComputeLift extends RulesetTransformation {
    val name: String = "ComputeLift"
    val title: String = "Compute lift"

    def buildOperation(from: Operation): Operation = new ComputeLift(from)
  }

  object MakeClusters extends RulesetTransformation {
    val name: String = "MakeClusters"
    val title: String = "Make clusters"

    def buildOperation(from: Operation): Operation = new MakeClusters(from)
  }

  object GraphBasedRules extends RulesetTransformation {
    val name: String = "GraphBasedRules"
    val title: String = "To graph-based rules"

    def buildOperation(from: Operation): Operation = new GraphBasedRules(from)
  }

  object CacheRuleset extends RulesetTransformation {
    val name: String = "CacheRuleset"
    val title: String = "Cache"

    def buildOperation(from: Operation): Operation = new CacheRuleset(from)
  }

  object CacheRulesetAction extends Action {
    val name: String = "CacheRuleset"
    val title: String = "Cache"

    def buildOperation(from: Operation): Operation = new actions.CacheRuleset(from)
  }

  object ExportRules extends Action {
    val name: String = "ExportRules"
    val title: String = "Export"

    def buildOperation(from: Operation): Operation = new actions.ExportRules(from)
  }

  object GetRules extends Action {
    val name: String = "GetRules"
    val title: String = "Get rules"

    def buildOperation(from: Operation): Operation = new actions.GetRules(from)
  }

  object RulesetSize extends Action {
    val name: String = "RulesetSize"
    val title: String = "Size"

    def buildOperation(from: Operation): Operation = new actions.RulesetSize(from)
  }

}