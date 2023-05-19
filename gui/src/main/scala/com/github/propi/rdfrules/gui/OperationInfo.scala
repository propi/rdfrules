package com.github.propi.rdfrules.gui

import com.github.propi.rdfrules.gui.operations._
import com.thoughtworks.binding.Binding.Constants

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
sealed trait OperationInfo {
  val name: String
  val title: String

  def isHidden: Boolean = false

  def subname: Option[String] = None

  def `type`: Operation.Type

  def sourceStructure: OperationStructure

  def targetStructure: OperationStructure

  final def isTransforming: Boolean = sourceStructure != targetStructure

  final def context: Documentation.Context = {
    val structType = if (sourceStructure == OperationStructure.Empty) targetStructure else sourceStructure
    Documentation(structType.toString)(`type`.docName)(title)
  }

  //val can not be overrided, therefore, def is here instead of val
  def groups: Set[OperationGroup] = Set.empty

  final def description: String = context.description

  val followingOperations: Constants[OperationInfo]

  def buildOperation(from: Operation): Operation
}

object OperationInfo {

  sealed trait Transformation extends OperationInfo {
    def `type`: Operation.Type = Operation.Type.Transformation
  }

  sealed trait Action extends OperationInfo {
    val `type`: Operation.Type = Operation.Type.Action
    val followingOperations: Constants[OperationInfo] = Constants()

    def targetStructure: OperationStructure = OperationStructure.Empty
  }

  sealed trait DatasetTransformation extends Transformation {
    val followingOperations: Constants[OperationInfo] = Constants(
      DatasetTransformation.LoadGraph,
      DatasetTransformation.LoadDataset,
      DatasetTransformation.MergeDatasets,
      DatasetTransformation.AddPrefixes,
      DatasetTransformation.MapQuads,
      DatasetTransformation.FilterQuads,
      DatasetTransformation.ShrinkQuads,
      DatasetTransformation.Discretize,
      DatasetTransformation.DiscretizeInBulk,
      DatasetTransformation.Split,
      DatasetTransformation.CacheDataset,
      DatasetTransformation.Index,
      ExportQuads,
      GetQuads,
      Prefixes,
      DatasetSize,
      Properties,
      Histogram
    )
  }

  sealed trait IndexTransformation extends Transformation {
    val followingOperations: Constants[OperationInfo] = Constants(
      IndexTransformation.CacheIndex,
      IndexTransformation.IndexToDataset,
      IndexTransformation.Mine,
      IndexTransformation.LoadRuleset,
      IndexTransformation.LoadRulesetWithRules,
      IndexTransformation.LoadPrediction,
      PropertiesCardinalities,
      ExportIndex,
    )
  }

  sealed trait RulesetTransformation extends Transformation {
    val followingOperations: Constants[OperationInfo] = Constants(
      RulesetTransformation.FilterRules,
      RulesetTransformation.ShrinkRules,
      RulesetTransformation.Sort,
      RulesetTransformation.ComputeConfidence,
      RulesetTransformation.ComputeSupport,
      RulesetTransformation.MakeClusters,
      RulesetTransformation.GraphAwareRules,
      RulesetTransformation.CacheRuleset,
      RulesetTransformation.Predict,
      RulesetTransformation.Prune,
      ExportRules,
      GetRules,
      RulesetSize,
      Instantiate
    )
  }

  sealed trait PredictionTransformation extends Transformation {
    val followingOperations: Constants[OperationInfo] = Constants(
      PredictionTransformation.FilterPrediction,
      PredictionTransformation.ShrinkPrediction,
      PredictionTransformation.Sort,
      PredictionTransformation.GroupPredictions,
      PredictionTransformation.PredictionToDataset,
      PredictionTransformation.PredictionToPredictionTasks,
      PredictionTransformation.CachePrediction,
      ExportPrediction,
      GetPrediction,
      PredictionSize
    )
  }

  sealed trait PredictionTasksTransformation extends Transformation {
    val followingOperations: Constants[OperationInfo] = Constants(
      PredictionTasksTransformation.FilterPredictionTasks,
      PredictionTasksTransformation.ShrinkPredictionTasks,
      PredictionTasksTransformation.SelectCandidates,
      PredictionTasksTransformation.WithModes,
      PredictionTasksTransformation.CachePredictionTasks,
      PredictionTasksTransformation.PredictionTasksToPrediction,
      PredictionTasksTransformation.PredictionTasksToDataset,
      GetPredictionTasks,
      Evaluate,
      PredictionTasksSize
    )
  }

  sealed trait DatasetToDataset extends Transformation {
    def sourceStructure: OperationStructure = OperationStructure.Dataset

    def targetStructure: OperationStructure = OperationStructure.Dataset
  }

  sealed trait RulesetToRuleset extends Transformation {
    def sourceStructure: OperationStructure = OperationStructure.Ruleset

    def targetStructure: OperationStructure = OperationStructure.Ruleset
  }

  sealed trait PredictionToPrediction extends Transformation {
    def sourceStructure: OperationStructure = OperationStructure.Prediction

    def targetStructure: OperationStructure = OperationStructure.Prediction
  }

  sealed trait PredictionTasksToPredictionTasks extends Transformation {
    def sourceStructure: OperationStructure = OperationStructure.PredictionTasks

    def targetStructure: OperationStructure = OperationStructure.PredictionTasks
  }

  object Root extends Transformation {
    val name: String = "root"
    val title: String = ""
    val followingOperations: Constants[OperationInfo] = Constants(
      Loading.LoadGraph,
      Loading.LoadDataset,
      Loading.LoadIndex,
      Loading.LoadRulesetWithoutIndex,
      Loading.LoadPredictionWithoutIndex
    )

    def sourceStructure: OperationStructure = OperationStructure.Empty

    def targetStructure: OperationStructure = OperationStructure.Empty

    def buildOperation(from: Operation): Operation = new Root
  }

  sealed trait LoadGraph extends Transformation {
    val name: String = "LoadGraph"
    val title: String = "Load graph"
  }

  sealed trait LoadDataset extends Transformation {
    val name: String = "LoadDataset"
    val title: String = "Load dataset"
  }

  sealed trait LoadIndex extends Transformation {
    val name: String = "LoadIndex"
    val title: String = "Load index"
  }

  sealed trait LoadRuleset extends Transformation {
    val name: String = "LoadRuleset"
    val title: String = "Load ruleset"
  }

  sealed trait LoadRulesetWithoutIndex extends Transformation {
    val name: String = "LoadRulesetWithoutIndex"
    val title: String = "Load ruleset"
  }

  sealed trait LoadPrediction extends Transformation {
    val name: String = "LoadPrediction"
    val title: String = "Load prediction"
  }

  sealed trait LoadPredictionWithoutIndex extends Transformation {
    val name: String = "LoadPredictionWithoutIndex"
    val title: String = "Load prediction"
  }

  sealed trait AddPrefixes extends Transformation {
    val name: String = "AddPrefixes"
    val title: String = "Add prefixes"
  }

  sealed trait MergeDatasets extends Transformation {
    val name: String = "MergeDatasets"
    val title: String = "Merge datasets"
  }

  sealed trait MapQuads extends Transformation {
    val name: String = "MapQuads"
    val title: String = "Map quads"
  }

  sealed trait FilterQuads extends Transformation {
    val name: String = "FilterQuads"
    val title: String = "Filter quads"
  }

  sealed trait Split extends Transformation {
    val name: String = "Split"
    val title: String = "Split"
  }

  sealed trait ShrinkQuads extends Transformation {
    val name: String = "ShrinkQuads"
    val title: String = "Shrink"
  }

  sealed trait Discretize extends Transformation {
    val name: String = "Discretize"
    val title: String = "Discretize"
  }

  sealed trait DiscretizeInBulk extends Transformation {
    val name: String = "DiscretizeInBulk"
    val title: String = "Discretize in bulk"
  }

  sealed trait CacheDataset extends Transformation {
    val name: String = "CacheDataset"
    val title: String = "Cache"
  }

  sealed trait IndexToDataset extends Transformation {
    val name: String = "IndexToDataset"
    val title: String = "To dataset"
  }

  sealed trait PredictionToDataset extends Transformation {
    val name: String = "PredictionToDataset"
    val title: String = "To dataset"
  }

  sealed trait FilterRules extends Transformation {
    val name: String = "FilterRules"
    val title: String = "Filter"
  }

  sealed trait FilterPrediction extends Transformation {
    val name: String = "FilterPrediction"
    val title: String = "Filter"
  }

  sealed trait FilterPredictionTasks extends Transformation {
    val name: String = "FilterPredictionTasks"
    val title: String = "Filter"
  }

  sealed trait SelectCandidates extends Transformation {
    val name: String = "SelectCandidates"
    val title: String = "Select candidates"
  }

  sealed trait WithModes extends Transformation {
    val name: String = "WithModes"
    val title: String = "With modes"
  }

  sealed trait PredictionTasksToPrediction extends Transformation {
    val name: String = "PredictionTasksToPredictions"
    val title: String = "To prediction"
  }

  sealed trait PredictionTasksToDataset extends Transformation {
    val name: String = "PredictionTasksToDataset"
    val title: String = "To dataset"
  }

  sealed trait GroupPredictions extends Transformation {
    val name: String = "GroupPredictions"
    val title: String = "Group predictions"
  }

  sealed trait PredictionToPredictionTasks extends Transformation {
    val name: String = "ToPredictionTasks"
    val title: String = "Generate prediction tasks"
  }

  sealed trait ShrinkRules extends Transformation {
    val name: String = "ShrinkRuleset"
    val title: String = "Shrink"
  }

  sealed trait ShrinkPrediction extends Transformation {
    val name: String = "ShrinkPrediction"
    val title: String = "Shrink"
  }

  sealed trait ShrinkPredictionTasks extends Transformation {
    val name: String = "ShrinkPredictionTasks"
    val title: String = "Shrink"
  }

  sealed trait Sort extends Transformation {
    val name: String = "SortRuleset"
    val title: String = "Sort"
  }

  sealed trait SortPrediction extends Transformation {
    val name: String = "SortPrediction"
    val title: String = "Sort"
  }

  sealed trait CacheRuleset extends Transformation {
    val name: String = "CacheRuleset"
    val title: String = "Cache"
  }

  sealed trait CachePrediction extends Transformation {
    val name: String = "CachePrediction"
    val title: String = "Cache"
  }

  sealed trait CachePredictionTasks extends Transformation {
    val name: String = "CachePredictionTasks"
    val title: String = "Cache"
  }

  sealed trait Mine extends Transformation {
    val name: String = "Mine"
    val title: String = "Mine rules"
  }

  sealed trait Index extends Transformation {
    val name: String = "Index"
    val title: String = "Index"
  }

  sealed trait CacheIndex extends Transformation {
    val name: String = "CacheIndex"
    val title: String = "Cache"
  }

  sealed trait Predict extends Transformation {
    val name: String = "Predict"
    val title: String = "Predict"
  }

  sealed trait Prune extends Transformation {
    val name: String = "Prune"
    val title: String = "Prune"
  }

  object Loading {
    object LoadGraph extends OperationInfo.LoadGraph with DatasetTransformation {
      override def `type`: Operation.Type = Operation.Type.Loading

      def buildOperation(from: Operation): Operation = new operations.LoadGraph(from, this)

      def sourceStructure: OperationStructure = OperationStructure.Empty

      def targetStructure: OperationStructure = OperationStructure.Dataset
    }

    object LoadDataset extends OperationInfo.LoadDataset with DatasetTransformation {
      override def `type`: Operation.Type = Operation.Type.Loading

      def buildOperation(from: Operation): Operation = new operations.LoadDataset(from, this)

      def sourceStructure: OperationStructure = OperationStructure.Empty

      def targetStructure: OperationStructure = OperationStructure.Dataset
    }

    object LoadIndex extends OperationInfo.LoadIndex with IndexTransformation {
      override def `type`: Operation.Type = Operation.Type.Loading

      def buildOperation(from: Operation): Operation = new operations.LoadIndex(from, this)

      def sourceStructure: OperationStructure = OperationStructure.Empty

      def targetStructure: OperationStructure = OperationStructure.Index
    }

    object LoadPredictionWithoutIndex extends OperationInfo.LoadPredictionWithoutIndex with PredictionTransformation {
      override def `type`: Operation.Type = Operation.Type.Loading

      def buildOperation(from: Operation): Operation = new operations.LoadPrediction(from, this)

      def sourceStructure: OperationStructure = OperationStructure.Empty

      def targetStructure: OperationStructure = OperationStructure.Prediction
    }

    object LoadRulesetWithoutIndex extends OperationInfo.LoadRulesetWithoutIndex with RulesetTransformation {
      override def `type`: Operation.Type = Operation.Type.Loading

      def buildOperation(from: Operation): Operation = new operations.LoadRuleset(from, this)

      def sourceStructure: OperationStructure = OperationStructure.Empty

      def targetStructure: OperationStructure = OperationStructure.Ruleset
    }
  }

  object DatasetTransformation {

    object LoadGraph extends OperationInfo.LoadGraph with DatasetTransformation with DatasetToDataset {
      def buildOperation(from: Operation): Operation = new operations.LoadGraph(from, this)
    }

    object LoadDataset extends OperationInfo.LoadDataset with DatasetTransformation with DatasetToDataset {
      def buildOperation(from: Operation): Operation = new operations.LoadDataset(from, this)
    }

    object AddPrefixes extends OperationInfo.AddPrefixes with DatasetTransformation with DatasetToDataset {
      def buildOperation(from: Operation): Operation = new operations.AddPrefixes(from, this)
    }

    object MergeDatasets extends OperationInfo.MergeDatasets with DatasetTransformation with DatasetToDataset {
      def buildOperation(from: Operation): Operation = new operations.MergeDatasets(from, this)
    }

    object MapQuads extends OperationInfo.MapQuads with DatasetTransformation with DatasetToDataset {
      def buildOperation(from: Operation): Operation = new operations.MapQuads(from, this)
    }

    object FilterQuads extends OperationInfo.FilterQuads with DatasetTransformation with DatasetToDataset {
      def buildOperation(from: Operation): Operation = new operations.FilterQuads(from, this)
    }

    object Split extends OperationInfo.Split with DatasetTransformation with DatasetToDataset {
      def buildOperation(from: Operation): Operation = new operations.Split(from, this)
    }

    object ShrinkQuads extends OperationInfo.ShrinkQuads with DatasetTransformation with DatasetToDataset {
      def buildOperation(from: Operation): Operation = new operations.ShrinkQuads(from, this)
    }

    object Discretize extends OperationInfo.Discretize with DatasetTransformation with DatasetToDataset {
      def buildOperation(from: Operation): Operation = new operations.Discretize(from, this)
    }

    object DiscretizeInBulk extends OperationInfo.DiscretizeInBulk with DatasetTransformation with DatasetToDataset {
      def buildOperation(from: Operation): Operation = new operations.DiscretizeInBulk(from, this)
    }

    object CacheDataset extends OperationInfo.CacheDataset with DatasetTransformation with DatasetToDataset {
      def buildOperation(from: Operation): Operation = buildOperation(from, None)

      def buildOperation(from: Operation, id: Option[String]): Operation = new operations.CacheDataset(from, this, id)

      override def groups: Set[OperationGroup] = super.groups &+ OperationGroup.Caching
    }

    object Index extends OperationInfo.Index with IndexTransformation {
      def buildOperation(from: Operation): Operation = new operations.Index(from, this)

      def sourceStructure: OperationStructure = OperationStructure.Dataset

      def targetStructure: OperationStructure = OperationStructure.Index
    }

  }

  object IndexTransformation {

    object Mine extends OperationInfo.Mine with RulesetTransformation {
      def buildOperation(from: Operation): Operation = new operations.Mine(from, this)

      def sourceStructure: OperationStructure = OperationStructure.Index

      def targetStructure: OperationStructure = OperationStructure.Ruleset
    }

    object CacheIndex extends OperationInfo.CacheIndex with IndexTransformation {
      def buildOperation(from: Operation): Operation = buildOperation(from, None)

      def buildOperation(from: Operation, id: Option[String]): Operation = new operations.CacheIndex(from, this, id)

      def sourceStructure: OperationStructure = OperationStructure.Index

      def targetStructure: OperationStructure = OperationStructure.Index

      override def groups: Set[OperationGroup] = super.groups &+ OperationGroup.Caching
    }

    object IndexToDataset extends OperationInfo.IndexToDataset with DatasetTransformation {
      def buildOperation(from: Operation): Operation = new operations.IndexToDataset(from, this)

      def sourceStructure: OperationStructure = OperationStructure.Index

      def targetStructure: OperationStructure = OperationStructure.Dataset
    }

    object LoadPrediction extends OperationInfo.LoadPrediction with PredictionTransformation {
      def buildOperation(from: Operation): Operation = new operations.LoadPrediction(from, this)

      def sourceStructure: OperationStructure = OperationStructure.Index

      def targetStructure: OperationStructure = OperationStructure.Prediction
    }

    object LoadRuleset extends OperationInfo.LoadRuleset with RulesetTransformation {
      override val subname: Option[String] = Some("FromFile")

      def buildOperation(from: Operation): Operation = new operations.LoadRuleset(from, this)

      def sourceStructure: OperationStructure = OperationStructure.Index

      def targetStructure: OperationStructure = OperationStructure.Ruleset
    }

    object LoadRulesetWithRules extends OperationInfo.LoadRuleset with RulesetTransformation {
      override val subname: Option[String] = Some("FromRules")

      override def isHidden: Boolean = true

      def buildOperation(from: Operation): Operation = new operations.LoadRuleset.WithRules(from, this)

      def sourceStructure: OperationStructure = OperationStructure.Index

      def targetStructure: OperationStructure = OperationStructure.Ruleset
    }

  }

  object PredictionTransformation {

    object FilterPrediction extends OperationInfo.FilterPrediction with PredictionTransformation with PredictionToPrediction {
      def buildOperation(from: Operation): Operation = new operations.FilterPrediction(from, this)
    }

    object GroupPredictions extends OperationInfo.GroupPredictions with PredictionTransformation with PredictionToPrediction {
      def buildOperation(from: Operation): Operation = new operations.GroupPredictions(from, this)
    }

    object ShrinkPrediction extends OperationInfo.ShrinkPrediction with PredictionTransformation with PredictionToPrediction {
      def buildOperation(from: Operation): Operation = new operations.ShrinkPrediction(from, this)
    }

    object Sort extends OperationInfo.SortPrediction with PredictionTransformation with PredictionToPrediction {
      def buildOperation(from: Operation): Operation = new operations.SortPrediction(from, this)
    }

    object CachePrediction extends OperationInfo.CachePrediction with PredictionTransformation with PredictionToPrediction {
      def buildOperation(from: Operation): Operation = buildOperation(from, None)

      def buildOperation(from: Operation, id: Option[String]): Operation = new operations.CachePrediction(from, this, id)

      override def groups: Set[OperationGroup] = super.groups &+ OperationGroup.Caching
    }

    object PredictionToDataset extends OperationInfo.PredictionToDataset with DatasetTransformation {
      def buildOperation(from: Operation): Operation = new operations.PredictionToDataset(from, this)

      def sourceStructure: OperationStructure = OperationStructure.Prediction

      def targetStructure: OperationStructure = OperationStructure.Dataset
    }

    object PredictionToPredictionTasks extends OperationInfo.PredictionToPredictionTasks with PredictionTasksTransformation {
      def buildOperation(from: Operation): Operation = new operations.PredictionToPredictionTasks(from, this)

      def sourceStructure: OperationStructure = OperationStructure.Prediction

      def targetStructure: OperationStructure = OperationStructure.PredictionTasks
    }

  }

  object PredictionTasksTransformation {

    object FilterPredictionTasks extends OperationInfo.FilterPredictionTasks with PredictionTasksTransformation with PredictionTasksToPredictionTasks {
      def buildOperation(from: Operation): Operation = new operations.FilterPredictionTasks(from, this)
    }

    object WithModes extends OperationInfo.WithModes with PredictionTasksTransformation with PredictionTasksToPredictionTasks {
      def buildOperation(from: Operation): Operation = new operations.WithModes(from, this)
    }

    object PredictionTasksToPrediction extends OperationInfo.PredictionTasksToPrediction with PredictionTransformation {
      def buildOperation(from: Operation): Operation = new operations.PredictionTasksToPrediction(from, this)

      def sourceStructure: OperationStructure = OperationStructure.PredictionTasks

      def targetStructure: OperationStructure = OperationStructure.Prediction
    }

    object PredictionTasksToDataset extends OperationInfo.PredictionTasksToDataset with DatasetTransformation {
      def buildOperation(from: Operation): Operation = new operations.PredictionTasksToDataset(from, this)

      def sourceStructure: OperationStructure = OperationStructure.PredictionTasks

      def targetStructure: OperationStructure = OperationStructure.Dataset
    }

    object SelectCandidates extends OperationInfo.SelectCandidates with PredictionTasksTransformation with PredictionTasksToPredictionTasks {
      def buildOperation(from: Operation): Operation = new operations.SelectCandidates(from, this)
    }

    object ShrinkPredictionTasks extends OperationInfo.ShrinkPredictionTasks with PredictionTasksTransformation with PredictionTasksToPredictionTasks {
      def buildOperation(from: Operation): Operation = new operations.ShrinkPredictionTasks(from, this)
    }

    object CachePredictionTasks extends OperationInfo.CachePredictionTasks with PredictionTasksTransformation with PredictionTasksToPredictionTasks {
      def buildOperation(from: Operation): Operation = buildOperation(from, None)

      def buildOperation(from: Operation, id: Option[String]): Operation = new operations.CachePredictionTasks(from, this, id)

      override def groups: Set[OperationGroup] = super.groups &+ OperationGroup.Caching
    }

  }

  object RulesetTransformation {

    object FilterRules extends OperationInfo.FilterRules with RulesetTransformation with RulesetToRuleset {
      def buildOperation(from: Operation): Operation = new operations.FilterRules(from, this)
    }

    object ShrinkRules extends OperationInfo.ShrinkRules with RulesetTransformation with RulesetToRuleset {
      def buildOperation(from: Operation): Operation = new operations.ShrinkRules(from, this)
    }

    object Sort extends OperationInfo.Sort with RulesetTransformation with RulesetToRuleset {
      def buildOperation(from: Operation): Operation = new operations.Sort(from, this)
    }

    object CacheRuleset extends OperationInfo.CacheRuleset with RulesetTransformation with RulesetToRuleset {
      def buildOperation(from: Operation): Operation = buildOperation(from, None)

      def buildOperation(from: Operation, id: Option[String]): Operation = new operations.CacheRuleset(from, this, id)

      override def groups: Set[OperationGroup] = super.groups &+ OperationGroup.Caching
    }

    object Predict extends OperationInfo.Predict with PredictionTransformation {
      def buildOperation(from: Operation): Operation = new operations.Predict(from, this)

      def sourceStructure: OperationStructure = OperationStructure.Ruleset

      def targetStructure: OperationStructure = OperationStructure.Prediction
    }

    object Prune extends OperationInfo.Prune with RulesetTransformation with RulesetToRuleset {
      def buildOperation(from: Operation): Operation = new operations.Prune(from, this)
    }

    object ComputeConfidence extends RulesetTransformation with RulesetToRuleset {
      val name: String = "ComputeConfidence"
      val title: String = "Compute confidence"

      def buildOperation(from: Operation): Operation = new operations.ComputeConfidence(from, this)
    }

    object ComputeSupport extends RulesetTransformation with RulesetToRuleset {
      val name: String = "ComputeSupport"
      val title: String = "Recompute support"

      def buildOperation(from: Operation): Operation = new operations.ComputeSupport(from, this)
    }

    object MakeClusters extends RulesetTransformation with RulesetToRuleset {
      val name: String = "MakeClusters"
      val title: String = "Make clusters"

      def buildOperation(from: Operation): Operation = new operations.MakeClusters(from, this)
    }

    object GraphAwareRules extends RulesetTransformation with RulesetToRuleset {
      val name: String = "GraphAwareRules"
      val title: String = "To graph-aware rules"

      def buildOperation(from: Operation): Operation = new operations.GraphAwareRules(from, this)
    }

  }

  object Instantiate extends Action {
    val name: String = "Instantiate"
    val title: String = "Instantiate"

    override def isHidden: Boolean = true

    def sourceStructure: OperationStructure = OperationStructure.Ruleset

    def buildOperation(from: Operation): Operation = new actions.Instantiate(from)
  }

  object ExportPrediction extends Action {
    val name: String = "ExportPrediction"
    val title: String = "Export"

    def sourceStructure: OperationStructure = OperationStructure.Prediction

    def buildOperation(from: Operation): Operation = new actions.ExportPrediction(from)
  }

  object ExportRules extends Action {
    val name: String = "ExportRules"
    val title: String = "Export"

    def sourceStructure: OperationStructure = OperationStructure.Ruleset

    def buildOperation(from: Operation): Operation = new actions.ExportRules(from)
  }

  object GetPrediction extends Action {
    val name: String = "GetPrediction"
    val title: String = "Get predicted triples"

    def sourceStructure: OperationStructure = OperationStructure.Prediction

    def buildOperation(from: Operation): Operation = new actions.GetPrediction(from)
  }

  object GetPredictionTasks extends Action {
    val name: String = "GetPredictionTasks"
    val title: String = "Get prediction tasks"

    def sourceStructure: OperationStructure = OperationStructure.PredictionTasks

    def buildOperation(from: Operation): Operation = new actions.GetPredictionTasks(from)
  }

  object GetRules extends Action {
    val name: String = "GetRules"
    val title: String = "Get rules"

    def sourceStructure: OperationStructure = OperationStructure.Ruleset

    def buildOperation(from: Operation): Operation = new actions.GetRules(from)
  }

  object PredictionSize extends Action {
    val name: String = "PredictionSize"
    val title: String = "Size"

    def sourceStructure: OperationStructure = OperationStructure.Prediction

    def buildOperation(from: Operation): Operation = new actions.PredictionSize(from)
  }

  object PredictionTasksSize extends Action {
    val name: String = "PredictionTasksSize"
    val title: String = "Size"

    def sourceStructure: OperationStructure = OperationStructure.PredictionTasks

    def buildOperation(from: Operation): Operation = new actions.PredictionTasksSize(from)
  }

  object RulesetSize extends Action {
    val name: String = "RulesetSize"
    val title: String = "Size"

    def sourceStructure: OperationStructure = OperationStructure.Ruleset

    def buildOperation(from: Operation): Operation = new actions.RulesetSize(from)
  }

  object ExportQuads extends Action {
    val name: String = "ExportQuads"
    val title: String = "Export"

    def sourceStructure: OperationStructure = OperationStructure.Dataset

    def buildOperation(from: Operation): Operation = new actions.ExportQuads(from)
  }

  object GetQuads extends Action {
    val name: String = "GetQuads"
    val title: String = "Get quads"

    def sourceStructure: OperationStructure = OperationStructure.Dataset

    def buildOperation(from: Operation): Operation = new actions.GetQuads(from)
  }

  object Prefixes extends Action {
    val name: String = "Prefixes"
    val title: String = "Get prefixes"

    def sourceStructure: OperationStructure = OperationStructure.Dataset

    def buildOperation(from: Operation): Operation = new actions.Prefixes(from)
  }

  object DatasetSize extends Action {
    val name: String = "DatasetSize"
    val title: String = "Size"

    def sourceStructure: OperationStructure = OperationStructure.Dataset

    def buildOperation(from: Operation): Operation = new actions.DatasetSize(from)
  }

  object PropertiesCardinalities extends Action {
    val name: String = "PropertiesCardinalities"
    val title: String = "Properties cardinality"

    def sourceStructure: OperationStructure = OperationStructure.Index

    def buildOperation(from: Operation): Operation = new actions.PropertiesCardinalities(from)
  }

  object Properties extends Action {
    val name: String = "Properties"
    val title: String = "Properties"

    def sourceStructure: OperationStructure = OperationStructure.Dataset

    def buildOperation(from: Operation): Operation = new actions.Properties(from)
  }

  object Histogram extends Action {
    val name: String = "Histogram"
    val title: String = "Histogram"

    def sourceStructure: OperationStructure = OperationStructure.Dataset

    def buildOperation(from: Operation): Operation = new actions.Histogram(from)
  }

  object ExportIndex extends Action {
    val name: String = "ExportIndex"
    val title: String = "Export"

    def sourceStructure: OperationStructure = OperationStructure.Index

    def buildOperation(from: Operation): Operation = new actions.ExportIndex(from)
  }

  object Evaluate extends Action {
    val name: String = "Evaluate"
    val title: String = "Evaluate"

    def sourceStructure: OperationStructure = OperationStructure.PredictionTasks

    def buildOperation(from: Operation): Operation = new actions.Evaluate(from)
  }

  def apply(op: js.Dynamic, parent: Operation): Option[OperationInfo] = {
    val name = op.name.asInstanceOf[String]
    val subname = op.subname.asInstanceOf[js.UndefOr[String]].toOption
    parent.info.followingOperations.value.find(x => x.name == name && x.subname == subname)
  }

}