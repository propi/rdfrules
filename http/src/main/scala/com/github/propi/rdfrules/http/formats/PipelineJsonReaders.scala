package com.github.propi.rdfrules.http.formats

import java.net.URL
import akka.NotUsed
import akka.stream.scaladsl.Source
import com.github.propi.rdfrules.algorithm.dbscan.SimilarityCounting
import com.github.propi.rdfrules.algorithm.{Clustering, RuleConsumer, RulesMining}
import com.github.propi.rdfrules.data.{Dataset, DiscretizationTask, Prefix, RdfSource, TripleItem}
import com.github.propi.rdfrules.http.formats.CommonDataJsonFormats._
import com.github.propi.rdfrules.http.formats.CommonDataJsonReaders._
import com.github.propi.rdfrules.http.task.Task.MergeDatasets
import com.github.propi.rdfrules.http.task._
import com.github.propi.rdfrules.http.task.ruleset.ComputeConfidence.ConfidenceType
import com.github.propi.rdfrules.http.task.ruleset.Instantiate
import com.github.propi.rdfrules.http.util.JsonSelector.PimpedJsValue
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.model.Model
import com.github.propi.rdfrules.model.Model.PredictionType
import com.github.propi.rdfrules.prediction.{Instantiation, PredictedTriples}
import com.github.propi.rdfrules.rule.InstantiatedRule.PredictedResult
import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.rule.{Measure, ResolvedRule, Rule, RulePattern}
import com.github.propi.rdfrules.ruleset.{Ruleset, RulesetSource}
import com.github.propi.rdfrules.utils.{Debugger, TypedKeyMap}
import org.apache.jena.riot.RDFFormat
import spray.json.DefaultJsonProtocol._
import spray.json._

/**
  * Created by Vaclav Zeman on 13. 8. 2018.
  */
object PipelineJsonReaders {

  implicit def loadGraphReader(implicit debugger: Debugger): RootJsonReader[data.LoadGraph] = (json: JsValue) => {
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

  implicit def loadDatasetReader(implicit debugger: Debugger): RootJsonReader[data.LoadDataset] = (json: JsValue) => {
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

  implicit val shrinkQuadsReader: RootJsonReader[data.Shrink] = (json: JsValue) => {
    new data.Shrink(json.convertTo[data.Shrink.ShrinkSetup])
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

  implicit def indexReader(implicit debugger: Debugger): RootJsonReader[data.Index] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new data.Index(fields("prefixedUris").convertTo[Boolean])
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
    new index.LoadIndex(fields("path").convertTo[String], fields("partially").convertTo[Boolean])
  }

  implicit def cacheIndexReader(implicit debugger: Debugger): RootJsonReader[index.Cache] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new index.Cache(fields("path").convertTo[String], fields("inMemory").convertTo[Boolean], fields("revalidate").convertTo[Boolean])
  }

  implicit val toDatasetReader: RootJsonReader[index.ToDataset] = (_: JsValue) => {
    new index.ToDataset()
  }

  implicit def mineReader(implicit debugger: Debugger): RootJsonReader[index.Mine] = (json: JsValue) => {
    new index.Mine(json.convertTo[RulesMining], json.convertTo[RuleConsumer.Invoker[Ruleset]])
  }

  implicit def loadRulesetReader(implicit debugger: Debugger): RootJsonReader[ruleset.LoadRuleset] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    val format = fields.get("format").map {
      case JsString("cache") => Left(true)
      case JsString("modelCache") => Left(false)
      case x => Right(x.convertTo[RulesetSource])
    }
    new ruleset.LoadRuleset(fields("path").convertTo[String], format, fields.get("parallelism").map(_.convertTo[Int]))
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

  implicit def loadModelReader(implicit debugger: Debugger): RootJsonReader[model.LoadModel] = (json: JsValue) => {
    val (path, format) = getModelPathFormat(json)
    new model.LoadModel(path, format)
  }

  implicit val predictReader: RootJsonReader[ruleset.Predict] = (json: JsValue) => {
    val selector = json.toSelector
    new ruleset.Predict(selector("predictedResults").toTypedIterable[PredictedResult].toSet, selector("injectiveMapping").to[Boolean].get)
  }

  implicit val pruneReader: RootJsonReader[ruleset.Prune] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new ruleset.Prune(fields("strategy").convertTo[ruleset.Prune.PruningStrategy])
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
      fields.get("patterns").map(_.convertTo[JsArray].elements.map(_.convertTo[RulePattern])).getOrElse(Nil),
      fields.get("indices").map(_.convertTo[JsArray].elements.map(_.convertTo[Int]).toSet).getOrElse(Set.empty)
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
      fields.get("patterns").map(_.convertTo[JsArray].elements.map(_.convertTo[RulePattern])).getOrElse(Nil),
      fields.get("indices").map(_.convertTo[JsArray].elements.map(_.convertTo[Int]).toSet).getOrElse(Set.empty)
    )
  }

  implicit val rulesetShrinkReader: RootJsonReader[ruleset.Shrink] = (json: JsValue) => {
    new ruleset.Shrink(json.convertTo[data.Shrink.ShrinkSetup])
  }

  implicit val modelShrinkReader: RootJsonReader[model.Shrink] = (json: JsValue) => {
    new model.Shrink(json.convertTo[data.Shrink.ShrinkSetup])
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
    new ruleset.ComputeConfidence(fields("type").convertTo[ConfidenceType])
  }

  implicit def makeClustersReader(implicit debugger: Debugger): RootJsonReader[ruleset.MakeClusters] = (json: JsValue) => {
    new ruleset.MakeClusters(json.convertTo[Clustering[FinalRule]])
  }

  implicit val findSimilarReader: RootJsonReader[ruleset.FindSimilar] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    implicit val sc: SimilarityCounting[Rule] = fields.get("features").map(_.convertTo[SimilarityCounting[Rule]]).getOrElse(Rule.ruleSimilarityCounting)
    new ruleset.FindSimilar(fields("rule").convertTo[ResolvedRule], fields("take").convertTo[Int], fields("dissimilar").convertTo[Boolean])
  }

  implicit def cacheRulesetReader(implicit debugger: Debugger): RootJsonReader[ruleset.Cache] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new ruleset.Cache(fields("path").convertTo[String], fields("inMemory").convertTo[Boolean], fields("revalidate").convertTo[Boolean])
  }

  implicit val cacheModelReader: RootJsonReader[model.Cache] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new model.Cache(fields("path").convertTo[String], fields("inMemory").convertTo[Boolean], fields("revalidate").convertTo[Boolean])
  }

  implicit val instantiateReader: RootJsonReader[ruleset.Instantiate] = (json: JsValue) => {
    val selector = json.toSelector
    new ruleset.Instantiate(selector("predictedResults").toTypedIterable[PredictedResult].toSet, selector("injectiveMapping").to[Boolean].get)
  }

  implicit val graphAwareRulesReader: RootJsonReader[ruleset.GraphAwareRules] = (_: JsValue) => {
    new ruleset.GraphAwareRules()
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
          case data.ExportQuads.name => pipeline ~> params.convertTo[data.ExportQuads] ~> ToJsonTask.FromUnit
          case data.FilterQuads.name => addTaskFromDataset(pipeline ~> params.convertTo[data.FilterQuads], tail)
          case data.GetQuads.name => pipeline ~> params.convertTo[data.GetQuads] ~> ToJsonTask.FromQuads
          case data.Histogram.name => pipeline ~> params.convertTo[data.Histogram] ~> ToJsonTask.FromHistogram
          case data.LoadDataset.name => addTaskFromDataset(pipeline |~> params.convertTo[data.LoadDataset], tail)
          case data.LoadGraph.name => addTaskFromDataset(pipeline |~> params.convertTo[data.LoadGraph], tail)
          case data.MapQuads.name => addTaskFromDataset(pipeline ~> params.convertTo[data.MapQuads], tail)
          case data.Prefixes.name => pipeline ~> params.convertTo[data.Prefixes] ~> ToJsonTask.FromPrefixes
          case data.Size.name => pipeline ~> params.convertTo[data.Size] ~> ToJsonTask.FromInt
          case data.Shrink.name => addTaskFromDataset(pipeline ~> params.convertTo[data.Shrink], tail)
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
          case model.ExportRules.name => pipeline ~> params.convertTo[model.ExportRules] ~> ToJsonTask.FromUnit
          case model.FilterRules.name => addTaskFromModel(pipeline ~> params.convertTo[model.FilterRules], tail)
          case model.GetRules.name => pipeline ~> params.convertTo[model.GetRules] ~> ToJsonTask.FromRules
          case model.Size.name => pipeline ~> params.convertTo[model.Size] ~> ToJsonTask.FromInt
          case model.Shrink.name => addTaskFromModel(pipeline ~> params.convertTo[model.Shrink], tail)
          case model.Sort.name => addTaskFromModel(pipeline ~> params.convertTo[model.Sort], tail)
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
          case ruleset.ExportRules.name => pipeline ~> params.convertTo[ruleset.ExportRules] ~> ToJsonTask.FromUnit
          case ruleset.FilterRules.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.FilterRules], tail)
          case ruleset.FindSimilar.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.FindSimilar], tail)
          case ruleset.GetRules.name => pipeline ~> params.convertTo[ruleset.GetRules] ~> ToJsonTask.FromRules
          case ruleset.GraphAwareRules.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.GraphAwareRules], tail)
          case ruleset.MakeClusters.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.MakeClusters], tail)
          case ruleset.Size.name => pipeline ~> params.convertTo[ruleset.Size] ~> ToJsonTask.FromInt
          case ruleset.Shrink.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.Shrink], tail)
          case ruleset.Sort.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.Sort], tail)
          case ruleset.Predict.name => addTaskFromDataset(pipeline ~> params.convertTo[ruleset.Predict], tail)
          case ruleset.Prune.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.Prune], tail)
          case ruleset.Instantiate.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.Instantiate], tail)
          case x => throw deserializationError(s"Invalid task '$x' can not be bound to Ruleset")
        }
      case _ => pipeline ~> new ToJsonTask.From[Ruleset]
    }

    @scala.annotation.tailrec
    def addTaskFromPredictedTriples(pipeline: Pipeline[PredictedTriples], tail: Seq[JsValue]): Pipeline[Source[JsValue, NotUsed]] = tail match {
      case Seq(head, tail@_*) =>
        val fields = head.asJsObject.fields
        val params = fields("parameters")
        fields("name").convertTo[String] match {
          case ruleset.Cache.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.Cache], tail)
          case ruleset.ComputeConfidence.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.ComputeConfidence], tail)
          case ruleset.ExportRules.name => pipeline ~> params.convertTo[ruleset.ExportRules] ~> ToJsonTask.FromUnit
          case ruleset.FilterRules.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.FilterRules], tail)
          case ruleset.FindSimilar.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.FindSimilar], tail)
          case ruleset.GetRules.name => pipeline ~> params.convertTo[ruleset.GetRules] ~> ToJsonTask.FromRules
          case ruleset.GraphAwareRules.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.GraphAwareRules], tail)
          case ruleset.MakeClusters.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.MakeClusters], tail)
          case ruleset.Size.name => pipeline ~> params.convertTo[ruleset.Size] ~> ToJsonTask.FromInt
          case ruleset.Shrink.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.Shrink], tail)
          case ruleset.Sort.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.Sort], tail)
          case ruleset.Predict.name => addTaskFromDataset(pipeline ~> params.convertTo[ruleset.Predict], tail)
          case ruleset.Prune.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.Prune], tail)
          case ruleset.Instantiate.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.Instantiate], tail)
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