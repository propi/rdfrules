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
  val `type`: Operation.Type

  //val can not be overrided, therefore, def is here instead of val
  def groups: Set[OperationGroup]

  val description: String
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
      DatasetTransformation.LoadGraph,
      DatasetTransformation.LoadDataset,
      DatasetTransformation.MergeDatasets,
      DatasetTransformation.AddPrefixes,
      DatasetTransformation.MapQuads,
      DatasetTransformation.FilterQuads,
      DatasetTransformation.TakeQuads,
      DatasetTransformation.DropQuads,
      DatasetTransformation.SliceQuads,
      DatasetTransformation.DiscretizeEqualDistance,
      DatasetTransformation.DiscretizeEqualFrequency,
      DatasetTransformation.DiscretizeEqualSize,
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

    def groups: Set[OperationGroup] = OperationGroup.Structure.Dataset
  }

  sealed trait IndexTransformation extends Transformation {
    val followingOperations: Constants[OperationInfo] = Constants(
      IndexTransformation.CacheIndex,
      IndexTransformation.ToDataset,
      IndexTransformation.Mine,
      IndexTransformation.CompleteDataset,
      IndexTransformation.PredictTriples,
      IndexTransformation.LoadRuleset,
      EvaluateIndex,
      CacheIndexAction,
    )

    def groups: Set[OperationGroup] = OperationGroup.Structure.Index
  }

  sealed trait RulesetTransformation extends Transformation {
    val followingOperations: Constants[OperationInfo] = Constants(
      RulesetTransformation.FilterRules,
      RulesetTransformation.TakeRules,
      RulesetTransformation.DropRules,
      RulesetTransformation.SliceRules,
      RulesetTransformation.Sorted,
      RulesetTransformation.Sort,
      RulesetTransformation.ComputeConfidence,
      RulesetTransformation.ComputePcaConfidence,
      RulesetTransformation.ComputeLift,
      RulesetTransformation.MakeClusters,
      RulesetTransformation.GraphBasedRules,
      RulesetTransformation.CacheRuleset,
      RulesetTransformation.CompleteDataset,
      RulesetTransformation.PredictTriples,
      RulesetTransformation.Prune,
      RulesetTransformation.Maximal,
      RulesetTransformation.Closed,
      RulesetTransformation.OnlyBetterDescendant,
      CacheRulesetAction,
      ExportRules,
      GetRules,
      RulesetSize,
      EvaluateRuleset
    )

    def groups: Set[OperationGroup] = OperationGroup.Structure.Ruleset
  }

  sealed trait ModelTransformation extends Transformation {
    val followingOperations: Constants[OperationInfo] = Constants(
      ModelTransformation.FilterRules,
      ModelTransformation.TakeRules,
      ModelTransformation.DropRules,
      ModelTransformation.SliceRules,
      ModelTransformation.Sorted,
      ModelTransformation.Sort,
      ModelTransformation.CacheRuleset,
      CacheRulesetAction,
      ExportRules,
      GetRules,
      RulesetSize
    )

    def groups: Set[OperationGroup] = OperationGroup.Structure.Model
  }

  object Root extends Transformation {
    val name: String = "root"
    val title: String = ""
    val followingOperations: Constants[OperationInfo] = Constants(
      DatasetTransformation.LoadGraph,
      DatasetTransformation.LoadDataset,
      IndexTransformation.LoadIndex,
      ModelTransformation.LoadModel
    )
    val description: String = ""

    def groups: Set[OperationGroup] = OperationGroup.Structure.Dataset

    def buildOperation(from: Operation): Operation = new Root
  }

  sealed trait LoadGraph extends Transformation {
    val name: String = "LoadGraph"
    val title: String = "Load graph"
    val description: String = "Load graph (set of triples) from a file in the workspace or from a remote file available via URL. The source is in some RDF format or serialized format and is supposed as a single graph."
  }

  sealed trait LoadDataset extends Transformation {
    val name: String = "LoadDataset"
    val title: String = "Load dataset"
    val description: String = "Load dataset (set of quads) from a file in the workspace or from a remote file available via URL. The source is in some RDF format or serialized format and can involve several graphs."
  }

  sealed trait LoadIndex extends Transformation {
    val name: String = "LoadIndex"
    val title: String = "Load index"
    val description: String = "Load serialized index from a file in the workspace."
  }

  sealed trait LoadRuleset extends Transformation {
    val name: String = "LoadRuleset"
    val title: String = "Load ruleset"
    val description: String = "Load serialized ruleset from a file in the workspace."
  }

  sealed trait AddPrefixes extends Transformation {
    val name: String = "AddPrefixes"
    val title: String = "Add prefixes"
    val description: String = "Add prefixes to datasets to shorten URIs."
  }

  sealed trait MergeDatasets extends Transformation {
    val name: String = "MergeDatasets"
    val title: String = "Merge datasets"
    val description: String = "Merge all previously loaded graphs and datasets to one dataset."
  }

  sealed trait MapQuads extends Transformation {
    val name: String = "MapQuads"
    val title: String = "Map quads"
    val description: String = "Map/Replace selected quads and their parts by user-defined filters and replacements."
  }

  sealed trait FilterQuads extends Transformation {
    val name: String = "FilterQuads"
    val title: String = "Filter quads"
    val description: String = "Filter all quads by user-defined conditions."
  }

  sealed trait TakeQuads extends Transformation {
    val name: String = "TakeQuads"
    val title: String = "Take"
    val description: String = "Take first N quads from the last loaded dataset."
  }

  sealed trait DropQuads extends Transformation {
    val name: String = "DropQuads"
    val title: String = "Drop"
    val description: String = "Drop first N quads from the last loaded dataset."
  }

  sealed trait SliceQuads extends Transformation {
    val name: String = "SliceQuads"
    val title: String = "Slice"
    val description: String = "Slice the dataset (set of quads) with a specified window."
  }

  sealed trait DiscretizeEqualFrequency extends Transformation {
    val name: String = "Discretize"
    val title: String = "Discretize (equal frequency)"
    val description: String = "Discretize all numeric literals related to filtered quads by the equal frequency strategy."
  }

  sealed trait DiscretizeEqualSize extends Transformation {
    val name: String = "Discretize"
    val title: String = "Discretize (equal size)"
    val description: String = "Discretize all numeric literals related to filtered quads by the equal size (support) strategy."
  }

  sealed trait DiscretizeEqualDistance extends Transformation {
    val name: String = "Discretize"
    val title: String = "Discretize (equal distance)"
    val description: String = "Discretize all numeric literals related to filtered quads by the equal distance strategy."
  }

  sealed trait CacheDataset extends Transformation {
    val name: String = "CacheDataset"
    val title: String = "Cache"
    val description: String = "Serialize loaded dataset into a file in the workspace at the server side for later use."
  }

  sealed trait ToDataset extends Transformation {
    val name: String = "ToDataset"
    val title: String = "To dataset"
    val description: String = "Convert the memory index back to the dataset."
  }

  sealed trait FilterRules extends Transformation {
    val name: String = "FilterRules"
    val title: String = "Filter"
    val description: String = "Filter all rules by patterns or measure conditions."
  }

  sealed trait TakeRules extends Transformation {
    val name: String = "TakeRules"
    val title: String = "Take"
    val description: String = "Take first N rules from the ruleset."
  }

  sealed trait DropRules extends Transformation {
    val name: String = "DropRules"
    val title: String = "Drop"
    val description: String = "Drop first N rules from the ruleset."
  }

  sealed trait SliceRules extends Transformation {
    val name: String = "SliceRules"
    val title: String = "Slice"
    val description: String = "Slice the ruleset (set of rules) with a specified window."
  }

  sealed trait Sorted extends Transformation {
    val name: String = "Sorted"
    val title: String = "Sorted"
    val description: String = "Sort rules by default sorting: Cluster, PcaConfidence, Lift, Confidence, HeadCoverage."
  }

  sealed trait Sort extends Transformation {
    val name: String = "Sort"
    val title: String = "Sort"
    val description: String = "Sort rules by user-defined rules attributes."
  }

  sealed trait CacheRuleset extends Transformation {
    val name: String = "CacheRuleset"
    val title: String = "Cache"
    val description: String = "Serialize loaded ruleset into a file in the workspace at the server side for later use."
  }

  sealed trait Mine extends Transformation {
    val name: String = "Mine"
    val title: String = "Mine"
    val description: String = "Mine rules from the indexed dataset with user-defined threshold, patterns and constraints. Default mining parameters are MinHeadSize=100, MinHeadCoverage=0.01, MaxRuleLength=3, no patterns, no constraints (only logical rules without constants)."
  }

  sealed trait Index extends Transformation {
    val name: String = "Index"
    val title: String = "Index"
    val description: String = "Save dataset into the memory index."
  }

  sealed trait CacheIndex extends Transformation {
    val name: String = "CacheIndex"
    val title: String = "Cache"
    val description: String = "Serialize loaded index into a file in the workspace at the server side for later use."
  }

  sealed trait LoadModel extends Transformation {
    val name: String = "LoadModel"
    val title: String = "Load rules"
    val description: String = "Load serialized rules/model from a file in the workspace."
  }

  sealed trait CompleteDataset extends Transformation {
    val name: String = "CompleteDataset"
    val title: String = "Complete dataset"
    val description: String = "Use a rules model to complete the loaded dataset."
  }

  sealed trait PredictTriples extends Transformation {
    val name: String = "PredictTriples"
    val title: String = "Predict triples"
    val description: String = "Use a rules model to generate/predict triples from the loaded dataset."
  }

  sealed trait Prune extends Transformation {
    val name: String = "Prune"
    val title: String = "Prune"
    val description: String = "From the list of rules take such rules which cover all genereted triples from the input dataset."
  }

  object DatasetTransformation {

    object LoadGraph extends OperationInfo.LoadGraph with DatasetTransformation {
      def buildOperation(from: Operation): Operation = new operations.LoadGraph(from, this)
    }

    object LoadDataset extends OperationInfo.LoadDataset with DatasetTransformation {
      def buildOperation(from: Operation): Operation = new operations.LoadDataset(from, this)
    }

    object AddPrefixes extends OperationInfo.AddPrefixes with DatasetTransformation {
      def buildOperation(from: Operation): Operation = new operations.AddPrefixes(from, this)
    }

    object MergeDatasets extends OperationInfo.MergeDatasets with DatasetTransformation {
      def buildOperation(from: Operation): Operation = new operations.MergeDatasets(from, this)
    }

    object MapQuads extends OperationInfo.MapQuads with DatasetTransformation {
      def buildOperation(from: Operation): Operation = new operations.MapQuads(from, this)
    }

    object FilterQuads extends OperationInfo.FilterQuads with DatasetTransformation {
      def buildOperation(from: Operation): Operation = new operations.FilterQuads(from, this)
    }

    object TakeQuads extends OperationInfo.TakeQuads with DatasetTransformation {
      def buildOperation(from: Operation): Operation = new operations.TakeQuads(from, this)
    }

    object DropQuads extends OperationInfo.DropQuads with DatasetTransformation {
      def buildOperation(from: Operation): Operation = new operations.DropQuads(from, this)
    }

    object SliceQuads extends OperationInfo.SliceQuads with DatasetTransformation {
      def buildOperation(from: Operation): Operation = new operations.SliceQuads(from, this)
    }

    object DiscretizeEqualFrequency extends OperationInfo.DiscretizeEqualFrequency with DatasetTransformation {
      def buildOperation(from: Operation): Operation = new operations.DiscretizeEqualFrequency(from, this)
    }

    object DiscretizeEqualSize extends OperationInfo.DiscretizeEqualSize with DatasetTransformation {
      def buildOperation(from: Operation): Operation = new operations.DiscretizeEqualSize(from, this)
    }

    object DiscretizeEqualDistance extends OperationInfo.DiscretizeEqualDistance with DatasetTransformation {
      def buildOperation(from: Operation): Operation = new operations.DiscretizeEqualDistance(from, this)
    }

    object CacheDataset extends OperationInfo.CacheDataset with DatasetTransformation {
      def buildOperation(from: Operation): Operation = buildOperation(from, None)

      def buildOperation(from: Operation, id: Option[String]): Operation = new operations.CacheDataset(from, this, id)

      override def groups: Set[OperationGroup] = super.groups &+ OperationGroup.Caching
    }

    object Index extends OperationInfo.Index with IndexTransformation {
      def buildOperation(from: Operation): Operation = new operations.Index(from, this)

      override def groups: Set[OperationGroup] = super.groups &+ OperationGroup.Transforming
    }

  }

  object IndexTransformation {

    object LoadIndex extends OperationInfo.LoadIndex with IndexTransformation {
      def buildOperation(from: Operation): Operation = new operations.LoadIndex(from, this)
    }

    object Mine extends OperationInfo.Mine with RulesetTransformation {
      def buildOperation(from: Operation): Operation = new operations.Mine(from, this)

      override def groups: Set[OperationGroup] = super.groups &+ OperationGroup.Transforming
    }

    object CompleteDataset extends OperationInfo.CompleteDataset with DatasetTransformation {
      def buildOperation(from: Operation): Operation = new operations.CompleteDataset(from, this, true)

      override def groups: Set[OperationGroup] = super.groups &+ OperationGroup.Transforming
    }

    object PredictTriples extends OperationInfo.PredictTriples with DatasetTransformation {
      def buildOperation(from: Operation): Operation = new operations.PredictTriples(from, this, true)

      override def groups: Set[OperationGroup] = super.groups &+ OperationGroup.Transforming
    }

    object CacheIndex extends OperationInfo.CacheIndex with IndexTransformation {
      def buildOperation(from: Operation): Operation = buildOperation(from, None)

      def buildOperation(from: Operation, id: Option[String]): Operation = new operations.CacheIndex(from, this, id)

      override def groups: Set[OperationGroup] = super.groups &+ OperationGroup.Caching
    }

    object ToDataset extends OperationInfo.ToDataset with DatasetTransformation {
      def buildOperation(from: Operation): Operation = new operations.ToDataset(from, this)

      override def groups: Set[OperationGroup] = super.groups &+ OperationGroup.Transforming
    }

    object LoadRuleset extends OperationInfo.LoadRuleset with RulesetTransformation {
      def buildOperation(from: Operation): Operation = new operations.LoadRuleset(from, this)

      override def groups: Set[OperationGroup] = super.groups &+ OperationGroup.Transforming
    }

  }

  object RulesetTransformation {

    object FilterRules extends OperationInfo.FilterRules with RulesetTransformation {
      def buildOperation(from: Operation): Operation = new operations.FilterRules(from, this)
    }

    object TakeRules extends OperationInfo.TakeRules with RulesetTransformation {
      def buildOperation(from: Operation): Operation = new operations.TakeRules(from, this)
    }

    object DropRules extends OperationInfo.DropRules with RulesetTransformation {
      def buildOperation(from: Operation): Operation = new operations.DropRules(from, this)
    }

    object SliceRules extends OperationInfo.SliceRules with RulesetTransformation {
      def buildOperation(from: Operation): Operation = new operations.SliceRules(from, this)
    }

    object Sorted extends OperationInfo.Sorted with RulesetTransformation {
      def buildOperation(from: Operation): Operation = new operations.Sorted(from, this)
    }

    object Sort extends OperationInfo.Sort with RulesetTransformation {
      def buildOperation(from: Operation): Operation = new operations.Sort(from, this)
    }

    object CacheRuleset extends OperationInfo.CacheRuleset with RulesetTransformation {
      def buildOperation(from: Operation): Operation = buildOperation(from, None)

      def buildOperation(from: Operation, id: Option[String]): Operation = new operations.CacheRuleset(from, this, id)

      override def groups: Set[OperationGroup] = super.groups &+ OperationGroup.Caching
    }

    object CompleteDataset extends OperationInfo.CompleteDataset with DatasetTransformation {
      def buildOperation(from: Operation): Operation = new operations.CompleteDataset(from, this, false)

      override def groups: Set[OperationGroup] = super.groups &+ OperationGroup.Transforming
    }

    object PredictTriples extends OperationInfo.PredictTriples with DatasetTransformation {
      def buildOperation(from: Operation): Operation = new operations.PredictTriples(from, this, false)

      override def groups: Set[OperationGroup] = super.groups &+ OperationGroup.Transforming
    }

    object Prune extends OperationInfo.Prune with RulesetTransformation {
      def buildOperation(from: Operation): Operation = new operations.Prune(from, this)
    }

    object Maximal extends RulesetTransformation {
      val name: String = "Maximal"
      val title: String = "Maximal"
      val description: String = "Return most specific rules (only rules with no descendants) within the ruleset tree."

      def buildOperation(from: Operation): Operation = new operations.Maximal(from, this)
    }

    object Closed extends RulesetTransformation {
      val name: String = "Closed"
      val title: String = "Closed"
      val description: String = "Return closed rules within the ruleset tree. A rule is closed if its parent has a different measure."

      def buildOperation(from: Operation): Operation = new operations.Closed(from, this)
    }

    object OnlyBetterDescendant extends RulesetTransformation {
      val name: String = "OnlyBetterDescendant"
      val title: String = "Only better descendant"
      val description: String = "This avoids outputting rules that do not improve the chosen measure w.r.t their parents."

      def buildOperation(from: Operation): Operation = new operations.Closed(from, this)
    }

    object Instantiate extends RulesetTransformation {
      val name: String = "Instantiate"
      val title: String = "Instantiate"
      val description: String = "Instantiate a selected rule."

      def buildOperation(from: Operation): Operation = new operations.Instantiate(from, this)
    }

    object ComputeConfidence extends RulesetTransformation {
      val name: String = "ComputeConfidence"
      val title: String = "Compute confidence"
      val description: String = "Compute the standard confidence for all rules and filter them by a minimal threshold value."

      def buildOperation(from: Operation): Operation = new operations.ComputeConfidence(from, this)
    }

    object ComputePcaConfidence extends RulesetTransformation {
      val name: String = "ComputePcaConfidence"
      val title: String = "Compute PCA confidence"
      val description: String = "Compute the PCA confidence for all rules and filter them by a minimal threshold value."

      def buildOperation(from: Operation): Operation = new operations.ComputePcaConfidence(from, this)
    }

    object ComputeLift extends RulesetTransformation {
      val name: String = "ComputeLift"
      val title: String = "Compute lift"
      val description: String = "Compute the standard confidence and lift for all rules and filter them by a minimal threshold value."

      def buildOperation(from: Operation): Operation = new operations.ComputeLift(from, this)
    }

    object MakeClusters extends RulesetTransformation {
      val name: String = "MakeClusters"
      val title: String = "Make clusters"
      val description: String = "Make clusters from the ruleset by DBScan algorithm."

      def buildOperation(from: Operation): Operation = new operations.MakeClusters(from, this)
    }

    object GraphBasedRules extends RulesetTransformation {
      val name: String = "GraphBasedRules"
      val title: String = "To graph-based rules"
      val description: String = "Attach information about graphs belonging to output rules."

      def buildOperation(from: Operation): Operation = new operations.GraphBasedRules(from, this)
    }

  }

  object ModelTransformation {

    object LoadModel extends OperationInfo.LoadModel with ModelTransformation {
      def buildOperation(from: Operation): Operation = new operations.LoadModel(from, this)
    }

    object FilterRules extends OperationInfo.FilterRules with ModelTransformation {
      def buildOperation(from: Operation): Operation = new operations.FilterRules(from, this)
    }

    object TakeRules extends OperationInfo.TakeRules with ModelTransformation {
      def buildOperation(from: Operation): Operation = new operations.TakeRules(from, this)
    }

    object DropRules extends OperationInfo.DropRules with ModelTransformation {
      def buildOperation(from: Operation): Operation = new operations.DropRules(from, this)
    }

    object SliceRules extends OperationInfo.SliceRules with ModelTransformation {
      def buildOperation(from: Operation): Operation = new operations.SliceRules(from, this)
    }

    object Sorted extends OperationInfo.Sorted with ModelTransformation {
      def buildOperation(from: Operation): Operation = new operations.Sorted(from, this)
    }

    object Sort extends OperationInfo.Sort with ModelTransformation {
      def buildOperation(from: Operation): Operation = new operations.Sort(from, this)
    }

    object CacheRuleset extends OperationInfo.CacheRuleset with ModelTransformation {
      def buildOperation(from: Operation): Operation = new operations.CacheRuleset(from, this, None)

      override def groups: Set[OperationGroup] = super.groups &+ OperationGroup.Caching
    }

  }

  object CacheRulesetAction extends Action {
    val name: String = "CacheRuleset"
    val title: String = "Cache"
    val description: String = "Serialize loaded ruleset into a file in the workspace at the server side for later use."

    def groups: Set[OperationGroup] = OperationGroup.Structure.Ruleset &+ OperationGroup.Caching

    def buildOperation(from: Operation): Operation = new actions.CacheRuleset(from)
  }

  object ExportRules extends Action {
    val name: String = "ExportRules"
    val title: String = "Export"
    val description: String = "Export the ruleset into a file in the workspace."

    def groups: Set[OperationGroup] = OperationGroup.Structure.Ruleset

    def buildOperation(from: Operation): Operation = new actions.ExportRules(from)
  }

  object GetRules extends Action {
    val name: String = "GetRules"
    val title: String = "Get rules"
    val description: String = "Get first 10000 rules from the ruleset."

    def groups: Set[OperationGroup] = OperationGroup.Structure.Ruleset

    def buildOperation(from: Operation): Operation = new actions.GetRules(from)
  }

  object RulesetSize extends Action {
    val name: String = "RulesetSize"
    val title: String = "Size"
    val description: String = "Get number of rules from the ruleset."

    def groups: Set[OperationGroup] = OperationGroup.Structure.Ruleset

    def buildOperation(from: Operation): Operation = new actions.RulesetSize(from)
  }

  object CacheDatasetAction extends Action {
    val name: String = "CacheDataset"
    val title: String = "Cache"
    val description: String = "Serialize loaded dataset into a file in the workspace at the server side for later use."

    def groups: Set[OperationGroup] = OperationGroup.Structure.Dataset &+ OperationGroup.Caching

    def buildOperation(from: Operation): Operation = new actions.CacheDataset(from)
  }

  object ExportQuads extends Action {
    val name: String = "ExportQuads"
    val title: String = "Export"
    val description: String = "Export the loaded and transformed dataset into a file in the workspace in an RDF format."

    def groups: Set[OperationGroup] = OperationGroup.Structure.Dataset

    def buildOperation(from: Operation): Operation = new actions.ExportQuads(from)
  }

  object GetQuads extends Action {
    val name: String = "GetQuads"
    val title: String = "Get quads"
    val description: String = "Get first 10000 quads from the loaded dataset."

    def groups: Set[OperationGroup] = OperationGroup.Structure.Dataset

    def buildOperation(from: Operation): Operation = new actions.GetQuads(from)
  }

  object Prefixes extends Action {
    val name: String = "Prefixes"
    val title: String = "Get prefixes"
    val description: String = "Show all prefixes defined in the loaded dataset."

    def groups: Set[OperationGroup] = OperationGroup.Structure.Dataset

    def buildOperation(from: Operation): Operation = new actions.Prefixes(from)
  }

  object DatasetSize extends Action {
    val name: String = "DatasetSize"
    val title: String = "Size"
    val description: String = "Get number of quads from the loaded dataset."

    def groups: Set[OperationGroup] = OperationGroup.Structure.Dataset

    def buildOperation(from: Operation): Operation = new actions.DatasetSize(from)
  }

  object Properties extends Action {
    val name: String = "Properties"
    val title: String = "Properties"
    val description: String = "Get all properties and their ranges with sizes."

    def groups: Set[OperationGroup] = OperationGroup.Structure.Dataset

    def buildOperation(from: Operation): Operation = new actions.Properties(from)
  }

  object Histogram extends Action {
    val name: String = "Histogram"
    val title: String = "Histogram"
    val description: String = "Aggregate triples by their parts and show the histogram."

    def groups: Set[OperationGroup] = OperationGroup.Structure.Dataset

    def buildOperation(from: Operation): Operation = new actions.Histogram(from)
  }

  object CacheIndexAction extends Action {
    val name: String = "CacheIndex"
    val title: String = "Cache"
    val description: String = "Serialize loaded index into a file in the workspace at the server side for later use."

    def groups: Set[OperationGroup] = OperationGroup.Structure.Index &+ OperationGroup.Caching

    def buildOperation(from: Operation): Operation = new actions.CacheIndex(from)
  }

  object EvaluateIndex extends Action {
    val name: String = "Evaluate"
    val title: String = "Evaluate model"
    val description: String = "Evaluate a rules model based on the loaded dataset as the test set."

    def groups: Set[OperationGroup] = OperationGroup.Structure.Index

    def buildOperation(from: Operation): Operation = new actions.Evaluate(from, this, true)
  }

  object EvaluateRuleset extends Action {
    val name: String = "Evaluate"
    val title: String = "Evaluate model"
    val description: String = "Evaluate a rules model based on the loaded index as the test set."

    def groups: Set[OperationGroup] = OperationGroup.Structure.Ruleset

    def buildOperation(from: Operation): Operation = new actions.Evaluate(from, this, false)
  }

  def apply(op: js.Dynamic, parent: Operation): Option[OperationInfo] = {
    val name = op.name.asInstanceOf[String]
    val ops = (parent.info match {
      case _: ModelTransformation => Iterator(
        ModelTransformation.CacheRuleset,
        ModelTransformation.DropRules,
        ModelTransformation.FilterRules,
        ModelTransformation.SliceRules,
        ModelTransformation.Sort,
        ModelTransformation.Sorted,
        ModelTransformation.TakeRules
      )
      case _: DatasetTransformation => Iterator(
        DatasetTransformation.MergeDatasets,
        DatasetTransformation.AddPrefixes,
        DatasetTransformation.MapQuads,
        DatasetTransformation.FilterQuads,
        DatasetTransformation.TakeQuads,
        DatasetTransformation.DropQuads,
        DatasetTransformation.SliceQuads,
        DatasetTransformation.CacheDataset,
        DatasetTransformation.Index
      )
      case _: RulesetTransformation => Iterator(
        RulesetTransformation.FilterRules,
        RulesetTransformation.TakeRules,
        RulesetTransformation.DropRules,
        RulesetTransformation.SliceRules,
        RulesetTransformation.Sorted,
        RulesetTransformation.Sort,
        RulesetTransformation.ComputeConfidence,
        RulesetTransformation.ComputePcaConfidence,
        RulesetTransformation.ComputeLift,
        RulesetTransformation.MakeClusters,
        RulesetTransformation.GraphBasedRules,
        RulesetTransformation.CacheRuleset,
        RulesetTransformation.CompleteDataset,
        RulesetTransformation.PredictTriples,
        RulesetTransformation.Prune,
        RulesetTransformation.Maximal,
        RulesetTransformation.Closed,
        RulesetTransformation.OnlyBetterDescendant,
        EvaluateRuleset
      )
      case _: IndexTransformation => Iterator(
        IndexTransformation.CacheIndex,
        IndexTransformation.CompleteDataset,
        IndexTransformation.Mine,
        IndexTransformation.PredictTriples,
        IndexTransformation.ToDataset,
        EvaluateIndex
      )
      case _ => Iterator()
    }) ++ Iterator(
      DatasetTransformation.LoadGraph,
      DatasetTransformation.LoadDataset,
      IndexTransformation.LoadIndex,
      IndexTransformation.LoadRuleset,
      ModelTransformation.LoadModel,
      CacheDatasetAction,
      ExportQuads,
      GetQuads,
      Prefixes,
      DatasetSize,
      Properties,
      Histogram,
      CacheIndexAction,
      CacheRulesetAction,
      ExportRules,
      GetRules,
      RulesetSize
    )
    if (name == "Discretize") {
      op.parameters.task.name.asInstanceOf[String] match {
        case "EquidistanceDiscretizationTask" => Some(DatasetTransformation.DiscretizeEqualDistance)
        case "EquifrequencyDiscretizationTask" => Some(DatasetTransformation.DiscretizeEqualFrequency)
        case "EquisizeDiscretizationTask" => Some(DatasetTransformation.DiscretizeEqualSize)
        case _ => None
      }
    } else {
      ops.find(_.name == name)
    }
  }

}