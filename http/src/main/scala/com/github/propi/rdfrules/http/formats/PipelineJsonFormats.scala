package com.github.propi.rdfrules.http.formats

import java.net.URL

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.github.propi.rdfrules.algorithm.RulesMining
import com.github.propi.rdfrules.data.{DiscretizationTask, Prefix, RdfSource, TripleItem}
import com.github.propi.rdfrules.http.task._
import com.github.propi.rdfrules.http.task.Task.MergeDatasets
import com.github.propi.rdfrules.rule.{Measure, RulePattern}
import com.github.propi.rdfrules.utils.TypedKeyMap
import com.typesafe.scalalogging.Logger
import org.apache.jena.riot.RDFFormat
import spray.json.{JsArray, JsString, JsValue, RootJsonReader}

/**
  * Created by Vaclav Zeman on 13. 8. 2018.
  */
object PipelineJsonFormats {

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

  implicit val indexReader: RootJsonReader[data.Index] = (_: JsValue) => {
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

  implicit val loadIndexReader: RootJsonReader[index.LoadIndex] = (json: JsValue) => {
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

  implicit def mineReader(implicit logger: Logger): RootJsonReader[index.Mine] = (json: JsValue) => {
    new index.Mine(json.convertTo[RulesMining])
  }

  implicit val loadRulesetReader: RootJsonReader[ruleset.LoadRuleset] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new ruleset.LoadRuleset(fields("path").convertTo[String])
  }

  implicit val filterRulesReader: RootJsonReader[ruleset.FilterRules] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new ruleset.FilterRules(
      fields.get("measures").collect {
        case JsArray(x) => x.map { json =>
          val fields = json.asJsObject.fields
          fields("name").convertTo[TypedKeyMap.Key[Measure]] -> fields("value").convertTo[TripleItemMatcher[TripleItem.Number[Double]]]
        }
      }.getOrElse(Nil),
      fields.get("patterns").map(_.convertTo[Seq[RulePattern]]).getOrElse(Nil)
    )
  }

  private val taskDefinitions = List(
    data.AddPrefixes,
    data.Cache,
    data.Discretize,
    data.Drop,
    data.ExportQuads,
    data.FilterQuads,
    data.GetQuads,
    data.Histogram,
    data.LoadDataset,
    data.LoadGraph,
    data.MapQuads,
    data.Prefixes,
    data.Size,
    data.Slice,
    data.Take,
    data.Types,
    index.Cache,
    index.LoadIndex,
    index.Mine,
    index.ToDataset,
    ruleset.Cache,
    ruleset.ComputeConfidence,
    ruleset.ComputeLift,
    ruleset.ComputePcaConfidence,
    ruleset.Drop,
    ruleset.ExportRules,
    ruleset.FilterRules,
    ruleset.FindSimilar,
    ruleset.FindDissimilar,
    ruleset.GetRules,
    ruleset.GraphBasedRules,
    ruleset.LoadRuleset,
    ruleset.MakeClusters,
    ruleset.Size,
    ruleset.Slice,
    ruleset.Sort,
    ruleset.Sorted,
    ruleset.Take
  )

  implicit def pipelineObjectReader(implicit logger: Logger): RootJsonReader[Pipeline[Any]] = new RootJsonReader[Pipeline[Any]] {
    def read(json: JsValue): Pipeline[Any] = json.asJsObject.fields
  }

  implicit val pipelineReader: RootJsonReader[Logger => Pipeline[Source[JsValue, NotUsed]]] = new RootJsonReader[Logger => Pipeline[Source[JsValue, NotUsed]]] {
    def read(json: JsValue): Logger => Pipeline[Source[JsValue, NotUsed]] = { implicit logger =>

    }
  }

}
