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
      DatasetTransformation.CacheDataset,
      DatasetTransformation.Index,
      CacheDatasetAction,
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
      PropertiesCardinalities,
      CacheIndexAction,
    )
  }

  sealed trait RulesetTransformation extends Transformation {
    val followingOperations: Constants[OperationInfo] = Constants(
      RulesetTransformation.FilterRules,
      RulesetTransformation.ShrinkRules,
      RulesetTransformation.Sort,
      RulesetTransformation.ComputeConfidence,
      RulesetTransformation.MakeClusters,
      RulesetTransformation.GraphAwareRules,
      RulesetTransformation.CacheRuleset,
      RulesetTransformation.Predict,
      RulesetTransformation.Prune,
      CacheRulesetAction,
      ExportRules,
      GetRules,
      RulesetSize,
      Instantiate
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

  object Root extends Transformation {
    val name: String = "root"
    val title: String = ""
    val followingOperations: Constants[OperationInfo] = Constants(
      Loading.LoadGraph,
      Loading.LoadDataset,
      Loading.LoadIndex
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

  sealed trait ShrinkQuads extends Transformation {
    val name: String = "ShrinkQuads"
    val title: String = "Shrink"
  }

  sealed trait Discretize extends Transformation {
    val name: String = "Discretize"
    val title: String = "Discretize"
  }

  sealed trait CacheDataset extends Transformation {
    val name: String = "CacheDataset"
    val title: String = "Cache"
  }

  sealed trait IndexToDataset extends Transformation {
    val name: String = "IndexToDataset"
    val title: String = "To dataset"
  }

  sealed trait FilterRules extends Transformation {
    val name: String = "FilterRules"
    val title: String = "Filter"
  }

  sealed trait ShrinkRules extends Transformation {
    val name: String = "ShrinkRules"
    val title: String = "Shrink"
  }

  sealed trait Sort extends Transformation {
    val name: String = "SortRuleset"
    val title: String = "Sort"
  }

  sealed trait CacheRuleset extends Transformation {
    val name: String = "CacheRuleset"
    val title: String = "Cache"
  }

  sealed trait Mine extends Transformation {
    val name: String = "Mine"
    val title: String = "Mine"
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

    object ShrinkQuads extends OperationInfo.ShrinkQuads with DatasetTransformation with DatasetToDataset {
      def buildOperation(from: Operation): Operation = new operations.ShrinkQuads(from, this)
    }

    object Discretize extends OperationInfo.Discretize with DatasetTransformation with DatasetToDataset {
      def buildOperation(from: Operation): Operation = new operations.Discretize(from, this)
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

    object Predict extends OperationInfo.Predict with DatasetTransformation {
      def buildOperation(from: Operation): Operation = new operations.Predict(from, this, false)

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

  object CacheRulesetAction extends Action {
    val name: String = "CacheRulesetAction"
    val title: String = "Cache"

    def sourceStructure: OperationStructure = OperationStructure.Ruleset

    override def groups: Set[OperationGroup] = super.groups &+ OperationGroup.Caching

    def buildOperation(from: Operation): Operation = new actions.CacheRuleset(from)
  }

  object ExportRules extends Action {
    val name: String = "ExportRules"
    val title: String = "Export"

    def sourceStructure: OperationStructure = OperationStructure.Ruleset

    def buildOperation(from: Operation): Operation = new actions.ExportRules(from)
  }

  object GetRules extends Action {
    val name: String = "GetRules"
    val title: String = "Get rules"

    def sourceStructure: OperationStructure = OperationStructure.Ruleset

    def buildOperation(from: Operation): Operation = new actions.GetRules(from)
  }

  object RulesetSize extends Action {
    val name: String = "RulesetSize"
    val title: String = "Size"

    def sourceStructure: OperationStructure = OperationStructure.Ruleset

    def buildOperation(from: Operation): Operation = new actions.RulesetSize(from)
  }

  object CacheDatasetAction extends Action {
    val name: String = "CacheDatasetAction"
    val title: String = "Cache"

    def sourceStructure: OperationStructure = OperationStructure.Dataset

    override def groups: Set[OperationGroup] = super.groups &+ OperationGroup.Caching

    def buildOperation(from: Operation): Operation = new actions.CacheDataset(from)
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

  object CacheIndexAction extends Action {
    val name: String = "CacheIndexAction"
    val title: String = "Cache"

    def sourceStructure: OperationStructure = OperationStructure.Index

    override def groups: Set[OperationGroup] = super.groups &+ OperationGroup.Caching

    def buildOperation(from: Operation): Operation = new actions.CacheIndex(from)
  }

  /*object EvaluateIndex extends Action {
    val name: String = "Evaluate"
    val title: String = "Evaluate model"
    val description: String = "Evaluate a rules model based on the loaded dataset as the test set."

    def groups: Set[OperationGroup] = OperationGroup.Structure.Index

    def buildOperation(from: Operation): Operation = new actions.Evaluate(from, this, true)
  }*/

  /*object EvaluateRuleset extends Action {
    val name: String = "Evaluate"
    val title: String = "Evaluate model"
    val description: String = "Evaluate a rules model based on the loaded index as the test set."

    def groups: Set[OperationGroup] = OperationGroup.Structure.Ruleset

    def buildOperation(from: Operation): Operation = new actions.Evaluate(from, this, false)
  }*/

  def apply(op: js.Dynamic, parent: Operation): Option[OperationInfo] = {
    val name = op.name.asInstanceOf[String]
    val subname = op.subname.asInstanceOf[js.UndefOr[String]].toOption
    parent.info.followingOperations.value.find(x => x.name == name && x.subname == subname)
  }

}