package com.github.propi.rdfrules.http.formats

import java.net.URL

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.github.propi.rdfrules.algorithm.dbscan.SimilarityCounting
import com.github.propi.rdfrules.algorithm.{Clustering, RulesMining}
import com.github.propi.rdfrules.data.{DiscretizationTask, Prefix, RdfSource, TripleItem}
import com.github.propi.rdfrules.http.formats.CommonDataJsonFormats._
import com.github.propi.rdfrules.http.formats.CommonDataJsonReaders._
import com.github.propi.rdfrules.http.task.Task.MergeDatasets
import com.github.propi.rdfrules.http.task._
import com.github.propi.rdfrules.rule.{Measure, Rule, RulePattern}
import com.github.propi.rdfrules.ruleset.{ResolvedRule, RulesetSource}
import com.github.propi.rdfrules.utils.{Debugger, TypedKeyMap}
import org.apache.jena.riot.RDFFormat
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.reflect.ClassTag

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
      json.convertTo[QuadMatcher],
      fields.get("inverse").exists(_.convertTo[Boolean])
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
    new data.Cache(fields("path").convertTo[String])
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

  implicit val typesReader: RootJsonReader[data.Types] = (_: JsValue) => {
    new data.Types()
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
    new index.Cache(fields("path").convertTo[String])
  }

  implicit val toDatasetReader: RootJsonReader[index.ToDataset] = (_: JsValue) => {
    new index.ToDataset()
  }

  implicit def mineReader(implicit debugger: Debugger): RootJsonReader[index.Mine] = (json: JsValue) => {
    new index.Mine(json.convertTo[RulesMining])
  }

  implicit val loadRulesetReader: RootJsonReader[ruleset.LoadRuleset] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new ruleset.LoadRuleset(fields("path").convertTo[String])
  }

  implicit val filterRulesReader: RootJsonReader[ruleset.FilterRules] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new ruleset.FilterRules(
      fields.get("measures").iterator.flatMap(_.convertTo[JsArray].elements).map { json =>
        val fields = json.asJsObject.fields
        fields("measure") -> fields("value").convertTo[String]
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

  implicit val dropRulesReader: RootJsonReader[ruleset.Drop] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new ruleset.Drop(fields("value").convertTo[Int])
  }

  implicit val sliceRulesReader: RootJsonReader[ruleset.Slice] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new ruleset.Slice(fields("start").convertTo[Int], fields("end").convertTo[Int])
  }

  implicit val sortedReader: RootJsonReader[ruleset.Sorted] = (_: JsValue) => {
    new ruleset.Sorted()
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

  implicit def computeConfidenceReader(implicit debugger: Debugger): RootJsonReader[ruleset.ComputeConfidence] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new ruleset.ComputeConfidence(fields.get("min").map(_.convertTo[Double]))
  }

  implicit def computePcaConfidenceReader(implicit debugger: Debugger): RootJsonReader[ruleset.ComputePcaConfidence] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new ruleset.ComputePcaConfidence(fields.get("min").map(_.convertTo[Double]))
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
    implicit val sc: SimilarityCounting[Rule.Simple] = fields.get("features").map(_.convertTo[SimilarityCounting[Rule.Simple]]).getOrElse(Rule.ruleSimilarityCounting)
    new ruleset.FindSimilar(fields("rule").convertTo[ResolvedRule], fields("take").convertTo[Int])
  }

  implicit val findDissimilarReader: RootJsonReader[ruleset.FindDissimilar] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    implicit val sc: SimilarityCounting[Rule.Simple] = fields.get("features").map(_.convertTo[SimilarityCounting[Rule.Simple]]).getOrElse(Rule.ruleSimilarityCounting)
    new ruleset.FindDissimilar(fields("rule").convertTo[ResolvedRule], fields("take").convertTo[Int])
  }

  implicit val cacheRulesetReader: RootJsonReader[ruleset.Cache] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new ruleset.Cache(fields("path").convertTo[String])
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

  implicit val getRulesReader: RootJsonReader[ruleset.GetRules] = (_: JsValue) => {
    new ruleset.GetRules()
  }

  implicit val rulesetSizeReader: RootJsonReader[ruleset.Size] = (_: JsValue) => {
    new ruleset.Size()
  }

  implicit def pipelineObjectReader(implicit debugger: Debugger): RootJsonReader[Task[_, _]] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    val params = fields("parameters")
    fields("name").convertTo[String] match {
      case data.AddPrefixes.name => params.convertTo[data.AddPrefixes]
      case data.Cache.name => params.convertTo[data.Cache]
      case data.Discretize.name => params.convertTo[data.Discretize]
      case data.Drop.name => params.convertTo[data.Drop]
      case data.ExportQuads.name => params.convertTo[data.ExportQuads]
      case data.FilterQuads.name => params.convertTo[data.FilterQuads]
      case data.GetQuads.name => params.convertTo[data.GetQuads]
      case data.Histogram.name => params.convertTo[data.Histogram]
      case data.LoadDataset.name => params.convertTo[data.LoadDataset]
      case data.LoadGraph.name => params.convertTo[data.LoadGraph]
      case data.MapQuads.name => params.convertTo[data.MapQuads]
      case data.Prefixes.name => params.convertTo[data.Prefixes]
      case data.Size.name => params.convertTo[data.Size]
      case data.Slice.name => params.convertTo[data.Slice]
      case data.Take.name => params.convertTo[data.Take]
      case data.Types.name => params.convertTo[data.Types]
      case data.Index.name => params.convertTo[data.Index]
      case index.Cache.name => params.convertTo[index.Cache]
      case index.LoadIndex.name => params.convertTo[index.LoadIndex]
      case index.Mine.name => params.convertTo[index.Mine]
      case index.ToDataset.name => params.convertTo[index.ToDataset]
      case ruleset.Cache.name => params.convertTo[ruleset.Cache]
      case ruleset.ComputeConfidence.name => params.convertTo[ruleset.ComputeConfidence]
      case ruleset.ComputeLift.name => params.convertTo[ruleset.ComputeLift]
      case ruleset.ComputePcaConfidence.name => params.convertTo[ruleset.ComputePcaConfidence]
      case ruleset.Drop.name => params.convertTo[ruleset.Drop]
      case ruleset.ExportRules.name => params.convertTo[ruleset.ExportRules]
      case ruleset.FilterRules.name => params.convertTo[ruleset.FilterRules]
      case ruleset.FindSimilar.name => params.convertTo[ruleset.FindSimilar]
      case ruleset.FindDissimilar.name => params.convertTo[ruleset.FindDissimilar]
      case ruleset.GetRules.name => params.convertTo[ruleset.GetRules]
      case ruleset.GraphBasedRules.name => params.convertTo[ruleset.GraphBasedRules]
      case ruleset.LoadRuleset.name => params.convertTo[ruleset.LoadRuleset]
      case ruleset.MakeClusters.name => params.convertTo[ruleset.MakeClusters]
      case ruleset.Size.name => params.convertTo[ruleset.Size]
      case ruleset.Slice.name => params.convertTo[ruleset.Slice]
      case ruleset.Sort.name => params.convertTo[ruleset.Sort]
      case ruleset.Sorted.name => params.convertTo[ruleset.Sorted]
      case ruleset.Take.name => params.convertTo[ruleset.Take]
      case MergeDatasets.name => params.convertTo[MergeDatasets]
      case x => throw deserializationError(s"Unknown task name: $x")
    }
  }

  implicit val pipelineReader: RootJsonReader[Debugger => Pipeline[Source[JsValue, NotUsed]]] = (json: JsValue) => { implicit debugger =>
    val `Task[NoInput, _]` = implicitly[ClassTag[Task[Task.NoInput.type, _]]]

    def addPipeline(tasks: List[Task[_, _]], pipeline: Option[Pipeline[_]]): Pipeline[Source[JsValue, NotUsed]] = tasks match {
      case head :: tail => addPipeline(tail, pipeline.map(_ + head).orElse(`Task[NoInput, _]`.unapply(head).map(Pipeline(_))))
      case _ => pipeline.map(_ + new ToJsonTask).getOrElse(throw deserializationError("No task defines"))
    }

    json match {
      case JsArray(x) => addPipeline(x.iterator.map(_.convertTo[Task[_, _]]).toList, None)
      case _ => throw deserializationError("No tasks defined")
    }
  }

}
