package com.github.propi.rdfrules.http.formats

import java.net.URL

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.github.propi.rdfrules.algorithm.dbscan.SimilarityCounting
import com.github.propi.rdfrules.algorithm.{Clustering, RulesMining}
import com.github.propi.rdfrules.data.{Dataset, DiscretizationTask, Prefix, RdfSource, TripleItem}
import com.github.propi.rdfrules.http.formats.CommonDataJsonFormats._
import com.github.propi.rdfrules.http.formats.CommonDataJsonReaders._
import com.github.propi.rdfrules.http.task.Task.MergeDatasets
import com.github.propi.rdfrules.http.task._
import com.github.propi.rdfrules.http.task.ruleset.{Closed, Instantiate}
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.model.Model
import com.github.propi.rdfrules.model.Model.PredictionType
import com.github.propi.rdfrules.rule.{Measure, Rule, RulePattern}
import com.github.propi.rdfrules.ruleset.{CoveredPaths, ResolvedRule, Ruleset, RulesetSource}
import com.github.propi.rdfrules.utils.{Debugger, TypedKeyMap}
import org.apache.jena.riot.RDFFormat
import spray.json.DefaultJsonProtocol._
import spray.json._

/**
  * Created by Vaclav Zeman on 13. 8. 2018.
  */
object PipelineJsonReaders {

  implicit val loadGraphReader: RootJsonReader[data.LoadGraph] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new data.LoadGraph(
      fields.get("graphName").map(_.convertTo[TripleItem.Uri]),
      fields.get("path").map(_.convertTo[String]),
      fields.get("url").map(_.convertTo[URL]),
      fields.get("format").map {
        case JsString("cache") => None
        case x => Some(x.convertTo[RdfSource])
      }
    )
  }

  implicit val loadDatasetReader: RootJsonReader[data.LoadDataset] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new data.LoadDataset(
      fields.get("path").map(_.convertTo[String]),
      fields.get("url").map(_.convertTo[URL]),
      fields.get("format").map {
        case JsString("cache") => None
        case x => Some(x.convertTo[RdfSource])
      }
    )
  }

  implicit val mergeDatasetsReader: RootJsonReader[MergeDatasets] = (_: JsValue) => {
    new MergeDatasets()
  }

  implicit val mapQuadsReader: RootJsonReader[data.MapQuads] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new data.MapQuads(
      fields("search").convertTo[QuadMatcher],
      fields("replacement").convertTo[QuadMapper],
      fields("search").asJsObject.fields.get("inverse").exists(_.convertTo[Boolean])
    )
  }

  implicit val filterQuadsReader: RootJsonReader[data.FilterQuads] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new data.FilterQuads(
      fields("or").convertTo[JsArray].elements.map(json => json.convertTo[QuadMatcher] -> json.asJsObject.fields.get("inverse").exists(_.convertTo[Boolean]))
    )
  }

  implicit val takeQuadsReader: RootJsonReader[data.Take] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new data.Take(fields("value").convertTo[Int])
  }

  implicit val dropQuadsReader: RootJsonReader[data.Drop] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new data.Drop(fields("value").convertTo[Int])
  }

  implicit val sliceQuadsReader: RootJsonReader[data.Slice] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new data.Slice(fields("start").convertTo[Int], fields("end").convertTo[Int])
  }

  implicit val addPrefixesReader: RootJsonReader[data.AddPrefixes] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new data.AddPrefixes(
      fields.get("path").map(_.convertTo[String]),
      fields.get("url").map(_.convertTo[URL]),
      fields.get("prefixes").map(_.convertTo[Seq[Prefix]]).getOrElse(Nil)
    )
  }

  implicit val prefixesReader: RootJsonReader[data.Prefixes] = (_: JsValue) => new data.Prefixes()

  implicit val discretizeReader: RootJsonReader[data.Discretize] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new data.Discretize(
      json.convertTo[QuadMatcher],
      fields.get("inverse").exists(_.convertTo[Boolean]),
      fields("task").convertTo[DiscretizationTask]
    )
  }

  implicit val cacheDatasetReader: RootJsonReader[data.Cache] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new data.Cache(fields("path").convertTo[String], fields("inMemory").convertTo[Boolean], fields("revalidate").convertTo[Boolean])
  }

  implicit def indexReader(implicit debugger: Debugger): RootJsonReader[data.Index] = (_: JsValue) => {
    new data.Index()
  }

  implicit val exportQuadsReader: RootJsonReader[data.ExportQuads] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new data.ExportQuads(
      fields("path").convertTo[String],
      fields.get("format").map {
        case JsString("tsv") => Left(RdfSource.Tsv)
        case x => Right(x.convertTo[RDFFormat])
      }
    )
  }

  implicit val getQuadsReader: RootJsonReader[data.GetQuads] = (_: JsValue) => {
    new data.GetQuads()
  }

  implicit val datasetSizeReader: RootJsonReader[data.Size] = (_: JsValue) => {
    new data.Size()
  }

  implicit val typesReader: RootJsonReader[data.Properties] = (_: JsValue) => {
    new data.Properties()
  }

  implicit val histogramReader: RootJsonReader[data.Histogram] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new data.Histogram(
      fields.get("subject").exists(_.convertTo[Boolean]),
      fields.get("predicate").exists(_.convertTo[Boolean]),
      fields.get("object").exists(_.convertTo[Boolean])
    )
  }

  implicit def loadIndexReader(implicit debugger: Debugger): RootJsonReader[index.LoadIndex] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new index.LoadIndex(fields("path").convertTo[String])
  }

  implicit val cacheIndexReader: RootJsonReader[index.Cache] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new index.Cache(fields("path").convertTo[String], fields("inMemory").convertTo[Boolean], fields("revalidate").convertTo[Boolean])
  }

  implicit val toDatasetReader: RootJsonReader[index.ToDataset] = (_: JsValue) => {
    new index.ToDataset()
  }

  implicit def mineReader(implicit debugger: Debugger): RootJsonReader[index.Mine] = (json: JsValue) => {
    new index.Mine(json.convertTo[RulesMining])
  }

  implicit val loadRulesetReader: RootJsonReader[ruleset.LoadRuleset] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    val format = fields.get("format").map {
      case JsString("cache") => Left(true)
      case JsString("modelCache") => Left(false)
      case x => Right(x.convertTo[RulesetSource])
    }
    new ruleset.LoadRuleset(fields("path").convertTo[String], format)
  }

  private def getModelPathFormat(json: JsValue) = {
    val fields = json.asJsObject.fields
    val path = fields("path").convertTo[String]
    val format = fields.get("format").map {
      case JsString("cache") => None
      case x => Some(x.convertTo[RulesetSource])
    }
    path -> format
  }

  implicit val loadModelReader: RootJsonReader[model.LoadModel] = (json: JsValue) => {
    val (path, format) = getModelPathFormat(json)
    new model.LoadModel(path, format)
  }

  implicit val completeDatasetReader: RootJsonReader[index.CompleteDataset] = (json: JsValue) => {
    val (path, format) = getModelPathFormat(json)
    val fields = json.asJsObject.fields
    new index.CompleteDataset(path, format, fields.get("onlyNewPredicates").forall(_.convertTo[Boolean]), fields.get("onlyFunctionalProperties").forall(_.convertTo[Boolean]))
  }

  implicit val rulesetCompleteDatasetReader: RootJsonReader[ruleset.CompleteDataset] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new ruleset.CompleteDataset(fields.get("onlyFunctionalProperties").forall(_.convertTo[Boolean]), fields.get("onlyNewPredicates").forall(_.convertTo[Boolean]))
  }

  implicit val predictTriplesReader: RootJsonReader[index.PredictTriples] = (json: JsValue) => {
    val (path, format) = getModelPathFormat(json)
    val fields = json.asJsObject.fields
    new index.PredictTriples(path, format, fields("predictionType").convertTo[PredictionType], fields.get("onlyFunctionalProperties").forall(_.convertTo[Boolean]))
  }

  implicit val rulesetPredictTriplesReader: RootJsonReader[ruleset.PredictTriples] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new ruleset.PredictTriples(fields.get("onlyFunctionalProperties").forall(_.convertTo[Boolean]), fields("predictionType").convertTo[PredictionType])
  }

  implicit val pruneReader: RootJsonReader[ruleset.Prune] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new ruleset.Prune(fields.get("onlyFunctionalProperties").forall(_.convertTo[Boolean]), fields.get("onlyExistingTriples").forall(_.convertTo[Boolean]))
  }

  implicit val evaluateReader: RootJsonReader[index.Evaluate] = (json: JsValue) => {
    val (path, format) = getModelPathFormat(json)
    val fields = json.asJsObject.fields
    new index.Evaluate(path, format, fields.get("onlyFunctionalProperties").forall(_.convertTo[Boolean]))
  }

  implicit val rulesetEvaluateReader: RootJsonReader[ruleset.Evaluate] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new ruleset.Evaluate(fields.get("onlyFunctionalProperties").forall(_.convertTo[Boolean]))
  }

  implicit val filterRulesReader: RootJsonReader[ruleset.FilterRules] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new ruleset.FilterRules(
      fields.get("measures").iterator.flatMap(_.convertTo[JsArray].elements).map { json =>
        val fields = json.asJsObject.fields
        fields("name") -> fields("value").convertTo[String]
      }.collect {
        case (JsString("RuleLength"), TripleItemMatcher.Number(x)) => None -> x
        case (measure, TripleItemMatcher.Number(x)) => Some(measure.convertTo[TypedKeyMap.Key[Measure]]) -> x
      }.toSeq,
      fields.get("patterns").map(_.convertTo[JsArray].elements.map(_.convertTo[RulePattern])).getOrElse(Nil)
    )
  }

  implicit val modelFilterRulesReader: RootJsonReader[model.FilterRules] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new model.FilterRules(
      fields.get("measures").iterator.flatMap(_.convertTo[JsArray].elements).map { json =>
        val fields = json.asJsObject.fields
        fields("name") -> fields("value").convertTo[String]
      }.collect {
        case (JsString("RuleLength"), TripleItemMatcher.Number(x)) => None -> x
        case (measure, TripleItemMatcher.Number(x)) => Some(measure.convertTo[TypedKeyMap.Key[Measure]]) -> x
      }.toSeq,
      fields.get("patterns").map(_.convertTo[JsArray].elements.map(_.convertTo[RulePattern])).getOrElse(Nil)
    )
  }

  implicit val takeRulesReader: RootJsonReader[ruleset.Take] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new ruleset.Take(fields("value").convertTo[Int])
  }

  implicit val modelTakeRulesReader: RootJsonReader[model.Take] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new model.Take(fields("value").convertTo[Int])
  }

  implicit val dropRulesReader: RootJsonReader[ruleset.Drop] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new ruleset.Drop(fields("value").convertTo[Int])
  }

  implicit val modelDropRulesReader: RootJsonReader[model.Drop] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new model.Drop(fields("value").convertTo[Int])
  }

  implicit val sliceRulesReader: RootJsonReader[ruleset.Slice] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new ruleset.Slice(fields("start").convertTo[Int], fields("end").convertTo[Int])
  }

  implicit val modelSliceRulesReader: RootJsonReader[model.Slice] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new model.Slice(fields("start").convertTo[Int], fields("end").convertTo[Int])
  }

  implicit val sortedReader: RootJsonReader[ruleset.Sorted] = (_: JsValue) => {
    new ruleset.Sorted()
  }

  implicit val modelSortedReader: RootJsonReader[model.Sorted] = (_: JsValue) => {
    new model.Sorted()
  }

  implicit val sortReader: RootJsonReader[ruleset.Sort] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new ruleset.Sort(
      fields.get("by").collect {
        case JsArray(x) => x.map { json =>
          val fields = json.asJsObject.fields
          (fields("measure") match {
            case JsString("RuleLength") => None
            case x => Some(x.convertTo[TypedKeyMap.Key[Measure]])
          }) -> fields.get("reversed").exists(_.convertTo[Boolean])
        }
      }.getOrElse(Nil)
    )
  }

  implicit val modelSortReader: RootJsonReader[model.Sort] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new model.Sort(
      fields.get("by").collect {
        case JsArray(x) => x.map { json =>
          val fields = json.asJsObject.fields
          (fields("measure") match {
            case JsString("RuleLength") => None
            case x => Some(x.convertTo[TypedKeyMap.Key[Measure]])
          }) -> fields.get("reversed").exists(_.convertTo[Boolean])
        }
      }.getOrElse(Nil)
    )
  }

  implicit def computeConfidenceReader(implicit debugger: Debugger): RootJsonReader[ruleset.ComputeConfidence] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new ruleset.ComputeConfidence(fields.get("min").map(_.convertTo[Double]), fields.get("topk").map(_.convertTo[Int]))
  }

  implicit def computePcaConfidenceReader(implicit debugger: Debugger): RootJsonReader[ruleset.ComputePcaConfidence] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new ruleset.ComputePcaConfidence(fields.get("min").map(_.convertTo[Double]), fields.get("topk").map(_.convertTo[Int]))
  }

  implicit def computeLiftReader(implicit debugger: Debugger): RootJsonReader[ruleset.ComputeLift] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new ruleset.ComputeLift(fields.get("min").map(_.convertTo[Double]))
  }

  implicit def makeClustersReader(implicit debugger: Debugger): RootJsonReader[ruleset.MakeClusters] = (json: JsValue) => {
    new ruleset.MakeClusters(json.convertTo[Clustering[Rule.Simple]])
  }

  implicit val findSimilarReader: RootJsonReader[ruleset.FindSimilar] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    implicit val sc: SimilarityCounting[Rule] = fields.get("features").map(_.convertTo[SimilarityCounting[Rule]]).getOrElse(Rule.ruleSimilarityCounting)
    new ruleset.FindSimilar(fields("rule").convertTo[ResolvedRule], fields("take").convertTo[Int])
  }

  implicit val findDissimilarReader: RootJsonReader[ruleset.FindDissimilar] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    implicit val sc: SimilarityCounting[Rule] = fields.get("features").map(_.convertTo[SimilarityCounting[Rule]]).getOrElse(Rule.ruleSimilarityCounting)
    new ruleset.FindDissimilar(fields("rule").convertTo[ResolvedRule], fields("take").convertTo[Int])
  }

  implicit val cacheRulesetReader: RootJsonReader[ruleset.Cache] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new ruleset.Cache(fields("path").convertTo[String], fields("inMemory").convertTo[Boolean], fields("revalidate").convertTo[Boolean])
  }

  implicit val cacheModelReader: RootJsonReader[model.Cache] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new model.Cache(fields("path").convertTo[String], fields("inMemory").convertTo[Boolean], fields("revalidate").convertTo[Boolean])
  }

  implicit val instantiateReader: RootJsonReader[ruleset.Instantiate] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new ruleset.Instantiate(fields("index").convertTo[Int], fields("index").convertTo[CoveredPaths.Part])
  }

  implicit val maximalReader: RootJsonReader[ruleset.Maximal] = (_: JsValue) => {
    new ruleset.Maximal()
  }

  implicit val closedReader: RootJsonReader[ruleset.Closed] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new ruleset.Closed(fields("measure").convertTo[TypedKeyMap.Key[Measure]])
  }

  implicit val onlyBetterDescendantReader: RootJsonReader[ruleset.OnlyBetterDescendant] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new ruleset.OnlyBetterDescendant(fields("measure").convertTo[TypedKeyMap.Key[Measure]])
  }

  implicit val graphBasedRulesReader: RootJsonReader[ruleset.GraphBasedRules] = (_: JsValue) => {
    new ruleset.GraphBasedRules()
  }

  implicit val exportRulesReader: RootJsonReader[ruleset.ExportRules] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new ruleset.ExportRules(
      fields("path").convertTo[String],
      fields.get("format").map(_.convertTo[RulesetSource])
    )
  }

  implicit val modelExportRulesReader: RootJsonReader[model.ExportRules] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new model.ExportRules(
      fields("path").convertTo[String],
      fields.get("format").map(_.convertTo[RulesetSource])
    )
  }

  implicit val getRulesReader: RootJsonReader[ruleset.GetRules] = (_: JsValue) => {
    new ruleset.GetRules()
  }

  implicit val modelGetRulesReader: RootJsonReader[model.GetRules] = (_: JsValue) => {
    new model.GetRules()
  }

  implicit val rulesetSizeReader: RootJsonReader[ruleset.Size] = (_: JsValue) => {
    new ruleset.Size()
  }

  implicit val modelSizeReader: RootJsonReader[model.Size] = (_: JsValue) => {
    new model.Size()
  }

  implicit val pipelineReader: RootJsonReader[Debugger => Pipeline[Source[JsValue, NotUsed]]] = (json: JsValue) => { implicit debugger =>
    def addInput(head: JsValue, tail: Seq[JsValue]): Pipeline[Source[JsValue, NotUsed]] = {
      val fields = head.asJsObject.fields
      val params = fields("parameters")
      fields("name").convertTo[String] match {
        case data.LoadDataset.name => addTaskFromDataset(Pipeline(params.convertTo[data.LoadDataset]), tail)
        case data.LoadGraph.name => addTaskFromDataset(Pipeline(params.convertTo[data.LoadGraph]), tail)
        case index.LoadIndex.name => addTaskFromIndex(Pipeline(params.convertTo[index.LoadIndex]), tail)
        case model.LoadModel.name => addTaskFromModel(Pipeline(params.convertTo[model.LoadModel]), tail)
        case x => throw deserializationError(s"Invalid first task: $x")
      }
    }

    @scala.annotation.tailrec
    def addTaskFromDataset(pipeline: Pipeline[Dataset], tail: Seq[JsValue]): Pipeline[Source[JsValue, NotUsed]] = tail match {
      case Seq(head, tail@_*) =>
        val fields = head.asJsObject.fields
        val params = fields("parameters")
        fields("name").convertTo[String] match {
          case data.AddPrefixes.name => addTaskFromDataset(pipeline ~> params.convertTo[data.AddPrefixes], tail)
          case data.Cache.name => addTaskFromDataset(pipeline ~> params.convertTo[data.Cache], tail)
          case data.Discretize.name => addTaskFromDataset(pipeline ~> params.convertTo[data.Discretize], tail)
          case data.Drop.name => addTaskFromDataset(pipeline ~> params.convertTo[data.Drop], tail)
          case data.ExportQuads.name => pipeline ~> params.convertTo[data.ExportQuads] ~> ToJsonTask.FromUnit
          case data.FilterQuads.name => addTaskFromDataset(pipeline ~> params.convertTo[data.FilterQuads], tail)
          case data.GetQuads.name => pipeline ~> params.convertTo[data.GetQuads] ~> ToJsonTask.FromQuads
          case data.Histogram.name => pipeline ~> params.convertTo[data.Histogram] ~> ToJsonTask.FromHistogram
          case data.LoadDataset.name => addTaskFromDataset(pipeline |~> params.convertTo[data.LoadDataset], tail)
          case data.LoadGraph.name => addTaskFromDataset(pipeline |~> params.convertTo[data.LoadGraph], tail)
          case data.MapQuads.name => addTaskFromDataset(pipeline ~> params.convertTo[data.MapQuads], tail)
          case data.Prefixes.name => pipeline ~> params.convertTo[data.Prefixes] ~> ToJsonTask.FromPrefixes
          case data.Size.name => pipeline ~> params.convertTo[data.Size] ~> ToJsonTask.FromInt
          case data.Slice.name => addTaskFromDataset(pipeline ~> params.convertTo[data.Slice], tail)
          case data.Take.name => addTaskFromDataset(pipeline ~> params.convertTo[data.Take], tail)
          case data.Properties.name => pipeline ~> params.convertTo[data.Properties] ~> ToJsonTask.FromTypes
          case data.Index.name => addTaskFromIndex(pipeline ~> params.convertTo[data.Index], tail)
          case MergeDatasets.name => addTaskFromDataset(pipeline |~> params.convertTo[MergeDatasets], tail)
          case x => throw deserializationError(s"Invalid task '$x' can not be bound to Dataset")
        }
      case _ => pipeline ~> new ToJsonTask.From[Dataset]
    }

    @scala.annotation.tailrec
    def addTaskFromModel(pipeline: Pipeline[Model], tail: Seq[JsValue]): Pipeline[Source[JsValue, NotUsed]] = tail match {
      case Seq(head, tail@_*) =>
        val fields = head.asJsObject.fields
        val params = fields("parameters")
        fields("name").convertTo[String] match {
          case model.Cache.name => addTaskFromModel(pipeline ~> params.convertTo[model.Cache], tail)
          case model.Drop.name => addTaskFromModel(pipeline ~> params.convertTo[model.Drop], tail)
          case model.ExportRules.name => pipeline ~> params.convertTo[model.ExportRules] ~> ToJsonTask.FromUnit
          case model.FilterRules.name => addTaskFromModel(pipeline ~> params.convertTo[model.FilterRules], tail)
          case model.GetRules.name => pipeline ~> params.convertTo[model.GetRules] ~> ToJsonTask.FromRules
          case model.Size.name => pipeline ~> params.convertTo[model.Size] ~> ToJsonTask.FromInt
          case model.Slice.name => addTaskFromModel(pipeline ~> params.convertTo[model.Slice], tail)
          case model.Sort.name => addTaskFromModel(pipeline ~> params.convertTo[model.Sort], tail)
          case model.Sorted.name => addTaskFromModel(pipeline ~> params.convertTo[model.Sorted], tail)
          case model.Take.name => addTaskFromModel(pipeline ~> params.convertTo[model.Take], tail)
          case x => throw deserializationError(s"Invalid task '$x' can not be bound to Model")
        }
      case _ => pipeline ~> new ToJsonTask.From[Model]
    }

    @scala.annotation.tailrec
    def addTaskFromIndex(pipeline: Pipeline[Index], tail: Seq[JsValue]): Pipeline[Source[JsValue, NotUsed]] = tail match {
      case Seq(head, tail@_*) =>
        val fields = head.asJsObject.fields
        val params = fields("parameters")
        fields("name").convertTo[String] match {
          case index.Cache.name => addTaskFromIndex(pipeline ~> params.convertTo[index.Cache], tail)
          case index.Mine.name => addTaskFromRuleset(pipeline ~> params.convertTo[index.Mine], tail)
          case index.ToDataset.name => addTaskFromDataset(pipeline ~> params.convertTo[index.ToDataset], tail)
          case ruleset.LoadRuleset.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.LoadRuleset], tail)
          case index.CompleteDataset.name => addTaskFromDataset(pipeline ~> params.convertTo[index.CompleteDataset], tail)
          case index.PredictTriples.name => addTaskFromDataset(pipeline ~> params.convertTo[index.PredictTriples], tail)
          case index.Evaluate.name => pipeline ~> params.convertTo[index.Evaluate] ~> ToJsonTask.FromEvaluationResult
          case x => throw deserializationError(s"Invalid task '$x' can not be bound to Index")
        }
      case _ => pipeline ~> new ToJsonTask.From[Index]
    }

    @scala.annotation.tailrec
    def addTaskFromRuleset(pipeline: Pipeline[Ruleset], tail: Seq[JsValue]): Pipeline[Source[JsValue, NotUsed]] = tail match {
      case Seq(head, tail@_*) =>
        val fields = head.asJsObject.fields
        val params = fields("parameters")
        fields("name").convertTo[String] match {
          case ruleset.Cache.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.Cache], tail)
          case ruleset.ComputeConfidence.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.ComputeConfidence], tail)
          case ruleset.ComputeLift.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.ComputeLift], tail)
          case ruleset.ComputePcaConfidence.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.ComputePcaConfidence], tail)
          case ruleset.Drop.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.Drop], tail)
          case ruleset.ExportRules.name => pipeline ~> params.convertTo[ruleset.ExportRules] ~> ToJsonTask.FromUnit
          case ruleset.FilterRules.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.FilterRules], tail)
          case ruleset.FindSimilar.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.FindSimilar], tail)
          case ruleset.FindDissimilar.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.FindDissimilar], tail)
          case ruleset.GetRules.name => pipeline ~> params.convertTo[ruleset.GetRules] ~> ToJsonTask.FromRules
          case ruleset.GraphBasedRules.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.GraphBasedRules], tail)
          case ruleset.MakeClusters.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.MakeClusters], tail)
          case ruleset.Size.name => pipeline ~> params.convertTo[ruleset.Size] ~> ToJsonTask.FromInt
          case ruleset.Slice.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.Slice], tail)
          case ruleset.Sort.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.Sort], tail)
          case ruleset.Sorted.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.Sorted], tail)
          case ruleset.Take.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.Take], tail)
          case ruleset.CompleteDataset.name => addTaskFromDataset(pipeline ~> params.convertTo[ruleset.CompleteDataset], tail)
          case ruleset.PredictTriples.name => addTaskFromDataset(pipeline ~> params.convertTo[ruleset.PredictTriples], tail)
          case ruleset.Prune.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.Prune], tail)
          case ruleset.Evaluate.name => pipeline ~> params.convertTo[ruleset.Evaluate] ~> ToJsonTask.FromEvaluationResult
          case ruleset.Instantiate.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.Instantiate], tail)
          case ruleset.Maximal.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.Maximal], tail)
          case ruleset.Closed.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.Closed], tail)
          case ruleset.OnlyBetterDescendant.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.OnlyBetterDescendant], tail)
          case x => throw deserializationError(s"Invalid task '$x' can not be bound to Ruleset")
        }
      case _ => pipeline ~> new ToJsonTask.From[Ruleset]
    }

    json match {
      case JsArray(Vector(head, tail@_*)) => addInput(head, tail)
      case _ => throw deserializationError("No tasks defined")
    }
  }

}