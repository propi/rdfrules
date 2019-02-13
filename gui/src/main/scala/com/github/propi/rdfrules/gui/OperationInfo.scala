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
    val description: String = ""

    def buildOperation(from: Operation): Operation = new Root
  }

  object LoadGraph extends DatasetTransformation {
    val name: String = "LoadGraph"
    val title: String = "Load graph"
    val description: String = "Load graph (set of triples) from a file in the workspace or from a remote file available via URL. The source is in some RDF format or serialized format and is supposed as a single graph."

    def buildOperation(from: Operation): Operation = new LoadGraph(from)
  }

  object LoadDataset extends DatasetTransformation {
    val name: String = "LoadDataset"
    val title: String = "Load dataset"
    val description: String = "Load dataset (set of quads) from a file in the workspace or from a remote file available via URL. The source is in some RDF format or serialized format and can involve several graphs."

    def buildOperation(from: Operation): Operation = new LoadDataset(from)
  }

  object LoadIndex extends IndexTransformation {
    val name: String = "LoadIndex"
    val title: String = "Load index"
    val description: String = "Load serialized index from a file in the workspace."

    def buildOperation(from: Operation): Operation = new LoadIndex(from)
  }

  object LoadRuleset extends RulesetTransformation {
    val name: String = "LoadRuleset"
    val title: String = "Load ruleset"
    val description: String = "Load serialized ruleset from a file in the workspace."

    def buildOperation(from: Operation): Operation = new LoadRuleset(from)
  }

  object AddPrefixes extends DatasetTransformation {
    val name: String = "AddPrefixes"
    val title: String = "Add prefixes"
    val description: String = "Add prefixes to datasets to shorten URIs."

    def buildOperation(from: Operation): Operation = new AddPrefixes(from)
  }

  object MergeDatasets extends DatasetTransformation {
    val name: String = "MergeDatasets"
    val title: String = "Merge datasets"
    val description: String = "Merge all previously loaded graphs and datasets to one dataset."

    def buildOperation(from: Operation): Operation = new MergeDatasets(from)
  }

  object MapQuads extends DatasetTransformation {
    val name: String = "MapQuads"
    val title: String = "Map quads"
    val description: String = "Map/Replace selected quads and their parts by user-defined filters and replacements."

    def buildOperation(from: Operation): Operation = new MapQuads(from)
  }

  object FilterQuads extends DatasetTransformation {
    val name: String = "FilterQuads"
    val title: String = "Filter quads"
    val description: String = "Filter all quads by user-defined conditions."

    def buildOperation(from: Operation): Operation = new FilterQuads(from)
  }

  object TakeQuads extends DatasetTransformation {
    val name: String = "TakeQuads"
    val title: String = "Take"
    val description: String = "Take first N quads from the last loaded dataset."

    def buildOperation(from: Operation): Operation = new TakeQuads(from)
  }

  object DropQuads extends DatasetTransformation {
    val name: String = "DropQuads"
    val title: String = "Drop"
    val description: String = "Drop first N quads from the last loaded dataset."

    def buildOperation(from: Operation): Operation = new DropQuads(from)
  }

  object SliceQuads extends DatasetTransformation {
    val name: String = "SliceQuads"
    val title: String = "Slice"
    val description: String = "Slice the dataset (set of quads) with a specified window."

    def buildOperation(from: Operation): Operation = new SliceQuads(from)
  }

  object DiscretizeEqualFrequency extends DatasetTransformation {
    val name: String = "Discretize"
    val title: String = "Discretize (equal frequency)"
    val description: String = "Discretize all numeric literals related to filtered quads by the equal frequency strategy."

    def buildOperation(from: Operation): Operation = new DiscretizeEqualFrequency(from)
  }

  object DiscretizeEqualSize extends DatasetTransformation {
    val name: String = "Discretize"
    val title: String = "Discretize (equal size)"
    val description: String = "Discretize all numeric literals related to filtered quads by the equal size (support) strategy."

    def buildOperation(from: Operation): Operation = new DiscretizeEqualSize(from)
  }

  object DiscretizeEqualDistance extends DatasetTransformation {
    val name: String = "Discretize"
    val title: String = "Discretize (equal distance)"
    val description: String = "Discretize all numeric literals related to filtered quads by the equal distance strategy."

    def buildOperation(from: Operation): Operation = new DiscretizeEqualDistance(from)
  }

  object CacheDataset extends DatasetTransformation {
    val name: String = "CacheDataset"
    val title: String = "Cache"
    val description: String = "Serialize loaded dataset into a file in the workspace at the server side for later use."

    def buildOperation(from: Operation): Operation = new CacheDataset(from)
  }

  object CacheDatasetAction extends Action {
    val name: String = "CacheDataset"
    val title: String = "Cache"
    val description: String = "Serialize loaded dataset into a file in the workspace at the server side for later use."

    def buildOperation(from: Operation): Operation = new actions.CacheDataset(from)
  }

  object ExportQuads extends Action {
    val name: String = "ExportQuads"
    val title: String = "Export"
    val description: String = "Export the loaded and transformed dataset into a file in the workspace in an RDF format."

    def buildOperation(from: Operation): Operation = new actions.ExportQuads(from)
  }

  object GetQuads extends Action {
    val name: String = "GetQuads"
    val title: String = "Get quads"
    val description: String = "Get first 10000 quads from the loaded dataset."

    def buildOperation(from: Operation): Operation = new actions.GetQuads(from)
  }

  object Prefixes extends Action {
    val name: String = "Prefixes"
    val title: String = "Get prefixes"
    val description: String = "Show all prefixes defined in the loaded dataset."

    def buildOperation(from: Operation): Operation = new actions.Prefixes(from)
  }

  object DatasetSize extends Action {
    val name: String = "DatasetSize"
    val title: String = "Size"
    val description: String = "Get number of quads from the loaded dataset."

    def buildOperation(from: Operation): Operation = new actions.DatasetSize(from)
  }

  object Types extends Action {
    val name: String = "Types"
    val title: String = "Types"
    val description: String = "Get all predicates and their ranges with sizes."

    def buildOperation(from: Operation): Operation = new actions.Types(from)
  }

  object Histogram extends Action {
    val name: String = "Histogram"
    val title: String = "Histogram"
    val description: String = "Aggregate triples by their parts and show the histogram."

    def buildOperation(from: Operation): Operation = new actions.Histogram(from)
  }

  object Index extends IndexTransformation {
    val name: String = "Index"
    val title: String = "Index"
    val description: String = "Save dataset into the memory index."

    def buildOperation(from: Operation): Operation = new Index(from)
  }

  object CacheIndex extends IndexTransformation {
    val name: String = "CacheIndex"
    val title: String = "Cache"
    val description: String = "Serialize loaded index into a file in the workspace at the server side for later use."

    def buildOperation(from: Operation): Operation = new CacheIndex(from)
  }

  object CacheIndexAction extends Action {
    val name: String = "CacheIndex"
    val title: String = "Cache"
    val description: String = "Serialize loaded index into a file in the workspace at the server side for later use."

    def buildOperation(from: Operation): Operation = new actions.CacheIndex(from)
  }

  object ToDataset extends DatasetTransformation {
    val name: String = "ToDataset"
    val title: String = "To dataset"
    val description: String = "Convert the memory index back to the dataset."

    def buildOperation(from: Operation): Operation = new ToDataset(from)
  }

  object Mine extends RulesetTransformation {
    val name: String = "Mine"
    val title: String = "Mine"
    val description: String = "Mine rules from the indexed dataset with user-defined threshold, patterns and constraints. Default mining parameters are MinHeadSize=100, MinHeadCoverage=0.01, MaxRuleLength=3, no patterns, no constraints (only logical rules without constants)."

    def buildOperation(from: Operation): Operation = new Mine(from)
  }

  object FilterRules extends RulesetTransformation {
    val name: String = "FilterRules"
    val title: String = "Filter"
    val description: String = "Filter all rules by patterns or measure conditions."

    def buildOperation(from: Operation): Operation = new FilterRules(from)
  }

  object TakeRules extends RulesetTransformation {
    val name: String = "TakeRules"
    val title: String = "Take"
    val description: String = "Take first N rules from the ruleset."

    def buildOperation(from: Operation): Operation = new TakeRules(from)
  }

  object DropRules extends RulesetTransformation {
    val name: String = "DropRules"
    val title: String = "Drop"
    val description: String = "Drop first N rules from the ruleset."

    def buildOperation(from: Operation): Operation = new DropRules(from)
  }

  object SliceRules extends RulesetTransformation {
    val name: String = "SliceRules"
    val title: String = "Slice"
    val description: String = "Slice the ruleset (set of rules) with a specified window."

    def buildOperation(from: Operation): Operation = new SliceRules(from)
  }

  object Sorted extends RulesetTransformation {
    val name: String = "Sorted"
    val title: String = "Sorted"
    val description: String = "Sort rules by default sorting: Cluster, PcaConfidence, Lift, Confidence, HeadCoverage."

    def buildOperation(from: Operation): Operation = new Sorted(from)
  }

  object Sort extends RulesetTransformation {
    val name: String = "Sort"
    val title: String = "Sort"
    val description: String = "Sort rules by user-defined rules attributes."

    def buildOperation(from: Operation): Operation = new Sort(from)
  }

  object ComputeConfidence extends RulesetTransformation {
    val name: String = "ComputeConfidence"
    val title: String = "Compute confidence"
    val description: String = "Compute the standard confidence for all rules and filter them by a minimal threshold value."

    def buildOperation(from: Operation): Operation = new ComputeConfidence(from)
  }

  object ComputePcaConfidence extends RulesetTransformation {
    val name: String = "ComputePcaConfidence"
    val title: String = "Compute PCA confidence"
    val description: String = "Compute the PCA confidence for all rules and filter them by a minimal threshold value."

    def buildOperation(from: Operation): Operation = new ComputePcaConfidence(from)
  }

  object ComputeLift extends RulesetTransformation {
    val name: String = "ComputeLift"
    val title: String = "Compute lift"
    val description: String = "Compute the standard confidence and lift for all rules and filter them by a minimal threshold value."

    def buildOperation(from: Operation): Operation = new ComputeLift(from)
  }

  object MakeClusters extends RulesetTransformation {
    val name: String = "MakeClusters"
    val title: String = "Make clusters"
    val description: String = "Make clusters from the ruleset by DBScan algorithm."

    def buildOperation(from: Operation): Operation = new MakeClusters(from)
  }

  object GraphBasedRules extends RulesetTransformation {
    val name: String = "GraphBasedRules"
    val title: String = "To graph-based rules"
    val description: String = "Attach information about graphs belonging to output rules."

    def buildOperation(from: Operation): Operation = new GraphBasedRules(from)
  }

  object CacheRuleset extends RulesetTransformation {
    val name: String = "CacheRuleset"
    val title: String = "Cache"
    val description: String = "Serialize loaded ruleset into a file in the workspace at the server side for later use."

    def buildOperation(from: Operation): Operation = new CacheRuleset(from)
  }

  object CacheRulesetAction extends Action {
    val name: String = "CacheRuleset"
    val title: String = "Cache"
    val description: String = "Serialize loaded ruleset into a file in the workspace at the server side for later use."

    def buildOperation(from: Operation): Operation = new actions.CacheRuleset(from)
  }

  object ExportRules extends Action {
    val name: String = "ExportRules"
    val title: String = "Export"
    val description: String = "Export the ruleset into a file in the workspace."

    def buildOperation(from: Operation): Operation = new actions.ExportRules(from)
  }

  object GetRules extends Action {
    val name: String = "GetRules"
    val title: String = "Get rules"
    val description: String = "Get first 10000 rules from the ruleset."

    def buildOperation(from: Operation): Operation = new actions.GetRules(from)
  }

  object RulesetSize extends Action {
    val name: String = "RulesetSize"
    val title: String = "Size"
    val description: String = "Get number of rules from the ruleset."

    def buildOperation(from: Operation): Operation = new actions.RulesetSize(from)
  }

  def apply(op: js.Dynamic): Option[OperationInfo] = {
    val name = op.name.asInstanceOf[String]
    val ops = Iterator(
      LoadGraph,
      LoadDataset,
      LoadIndex,
      MergeDatasets,
      AddPrefixes,
      MapQuads,
      FilterQuads,
      TakeQuads,
      DropQuads,
      SliceQuads,
      CacheDataset,
      Index,
      CacheDatasetAction,
      ExportQuads,
      GetQuads,
      Prefixes,
      DatasetSize,
      Types,
      Histogram,
      CacheIndex,
      ToDataset,
      Mine,
      CacheIndexAction,
      LoadRuleset,
      FilterRules,
      TakeRules,
      DropRules,
      SliceRules,
      Sorted,
      Sort,
      ComputeConfidence,
      ComputePcaConfidence,
      ComputeLift,
      MakeClusters,
      GraphBasedRules,
      CacheRuleset,
      CacheRulesetAction,
      ExportRules,
      GetRules,
      RulesetSize
    )
    if (name == "Discretize") {
      op.parameters.task.name.asInstanceOf[String] match {
        case "EquidistanceDiscretizationTask" => Some(DiscretizeEqualDistance)
        case "EquifrequencyDiscretizationTask" => Some(DiscretizeEqualFrequency)
        case "EquisizeDiscretizationTask" => Some(DiscretizeEqualSize)
        case _ => None
      }
    } else {
      ops.find(_.name == name)
    }
  }

}