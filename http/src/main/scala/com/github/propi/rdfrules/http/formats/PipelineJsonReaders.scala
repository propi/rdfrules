package com.github.propi.rdfrules.http.formats

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.github.propi.rdfrules.algorithm.clustering.SimilarityCounting
import com.github.propi.rdfrules.algorithm.consumer.InMemoryRuleConsumer
import com.github.propi.rdfrules.algorithm.{Clustering, RuleConsumer, RulesMining}
import com.github.propi.rdfrules.data.ops.Sampleable
import com.github.propi.rdfrules.data.{Dataset, DiscretizationTask, Prefix, RdfSource, TripleItem}
import com.github.propi.rdfrules.http.formats.CommonDataJsonFormats._
import com.github.propi.rdfrules.http.formats.CommonDataJsonReaders._
import com.github.propi.rdfrules.http.task.Task.MergeDatasets
import com.github.propi.rdfrules.http.task._
import com.github.propi.rdfrules.http.task.prediction.{GetPrediction, LoadPredictionWithoutIndex}
import com.github.propi.rdfrules.http.task.predictionTasks.Evaluate
import com.github.propi.rdfrules.http.task.predictionTasks.Evaluate.RankingStrategy
import com.github.propi.rdfrules.http.task.predictionTasks.Select.SelectionStrategy
import com.github.propi.rdfrules.http.task.ruleset.ComputeConfidence.ConfidenceType
import com.github.propi.rdfrules.http.task.ruleset.{LoadRuleset, LoadRulesetWithoutIndex}
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.prediction._
import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.rule.{DefaultConfidence, Measure, ResolvedRule, Rule, RulePattern}
import com.github.propi.rdfrules.ruleset.{Ruleset, RulesetSource}
import com.github.propi.rdfrules.utils.JsonSelector.PimpedJsValue
import com.github.propi.rdfrules.utils.{Debugger, TypedKeyMap}
import spray.json.DefaultJsonProtocol._
import spray.json._

import java.net.URL

/**
  * Created by Vaclav Zeman on 13. 8. 2018.
  */
object PipelineJsonReaders {

  implicit def loadGraphReader(implicit debugger: Debugger): RootJsonReader[data.LoadGraph] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    implicit val settings: RdfSource.Settings = fields.get("settings").map(_.convertTo[RdfSource.Settings]).getOrElse(RdfSource.NoSettings)
    new data.LoadGraph(
      fields.get("graphName").map(_.convertTo[TripleItem.Uri]),
      fields.get("path").map(_.convertTo[String]),
      fields.get("url").map(_.convertTo[URL])
    )
  }

  implicit def loadDatasetReader(implicit debugger: Debugger): RootJsonReader[data.LoadDataset] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    implicit val settings: RdfSource.Settings = fields.get("settings").map(_.convertTo[RdfSource.Settings]).getOrElse(RdfSource.NoSettings)
    new data.LoadDataset(
      fields.get("path").map(_.convertTo[String]),
      fields.get("url").map(_.convertTo[URL])
    )
  }

  implicit def loadRulesetWithoutIndexReader(implicit debugger: Debugger): RootJsonReader[ruleset.LoadRulesetWithoutIndex] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    val format = fields("format").convertTo[RulesetSource]
    new LoadRulesetWithoutIndex(fields("path").convertTo[String], format, fields.get("parallelism").map(_.convertTo[Int]))
  }

  implicit def loadPredictionWithoutIndexReader(implicit debugger: Debugger): RootJsonReader[prediction.LoadPredictionWithoutIndex] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new LoadPredictionWithoutIndex(
      fields("path").convertTo[String],
      fields("format").convertTo[PredictionSource]
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
    new data.Shrink(json.convertTo[ShrinkSetup])
  }

  implicit val splitReader: RootJsonReader[data.Split] = (json: JsValue) => {
    val selector = json.toSelector
    val train = selector("train")
    val test = selector("test")
    val trainUri = train("uri").to[TripleItem.Uri]
    val trainPart = train("part").to[Sampleable.Part]
    val testUri = test("uri").to[TripleItem.Uri]
    val testPart = test("part").to[Sampleable.Part]
    new data.Split(trainUri -> trainPart, testUri -> testPart)
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

  implicit def cacheDatasetReader(implicit debugger: Debugger): RootJsonReader[data.Cache] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new data.Cache(fields("path").convertTo[String], fields("inMemory").convertTo[Boolean], fields("revalidate").convertTo[Boolean])
  }

  implicit def indexReader(implicit debugger: Debugger): RootJsonReader[data.Index] = (json: JsValue) => {
    val selector = json.toSelector
    new data.Index(selector.get("train").toTypedIterable[TripleItem.Uri].toSet, selector.get("test").toTypedIterable[TripleItem.Uri].toSet)
    //(fields("prefixedUris").convertTo[Boolean])
  }

  implicit val exportQuadsReader: RootJsonReader[data.ExportQuads] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new data.ExportQuads(fields("path").convertTo[String])
  }

  implicit val exportIndexReader: RootJsonReader[index.ExportIndex] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new index.ExportIndex(fields("path").convertTo[String])
  }

  implicit val exportPredictionReader: RootJsonReader[prediction.ExportPrediction] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new prediction.ExportPrediction(
      fields("path").convertTo[String],
      fields("format").convertTo[PredictionSource]
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

  implicit def indexToDatasetReader(implicit debugger: Debugger): RootJsonReader[index.ToDataset] = (_: JsValue) => {
    new index.ToDataset
  }

  implicit val predictionToDatasetReader: RootJsonReader[prediction.ToDataset] = (_: JsValue) => {
    new prediction.ToDataset
  }

  implicit val predictionTasksToDatasetReader: RootJsonReader[predictionTasks.ToDataset] = (_: JsValue) => {
    new predictionTasks.ToDataset
  }

  implicit val predictionTasksToPredictionsReader: RootJsonReader[predictionTasks.ToPredictions] = (_: JsValue) => {
    new predictionTasks.ToPredictions
  }

  implicit def predictionToPredictionTasksReader(implicit debugger: Debugger): RootJsonReader[prediction.ToPredictionTasks] = (json: JsValue) => {
    val selector = json.toSelector
    implicit val defaultConfidence: DefaultConfidence = selector("confidence").to[DefaultConfidence]
    new prediction.ToPredictionTasks(
      selector("generator").to[PredictionTasksBuilder],
      selector.get("limit").toOpt[Int].getOrElse(-1),
      selector.get("topK").toOpt[Int].getOrElse(-1)
    )
  }

  implicit def mineReader(implicit debugger: Debugger): RootJsonReader[index.Mine] = (json: JsValue) => {
    val selector = json.toSelector
    new index.Mine(json.convertTo[RulesMining], selector.get("ruleConsumers").toOptTypedIterable[RuleConsumer.Invoker[Ruleset]].reduceOption(_ ~> _).getOrElse(RuleConsumer(InMemoryRuleConsumer())))
  }

  implicit def loadRulesetReader(implicit debugger: Debugger): RootJsonReader[ruleset.LoadRuleset] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    val source = fields.get("rules") match {
      case Some(rules) =>
        LoadRuleset.RulesetSource.Rules(rules.convertTo[Iterable[ResolvedRule]])
      case None =>
        LoadRuleset.RulesetSource.File(
          fields("path").convertTo[String],
          fields("format").convertTo[RulesetSource]
        )
    }

    new ruleset.LoadRuleset(source, fields.get("parallelism").map(_.convertTo[Int]))
  }

  implicit def loadPredictionReader(implicit debugger: Debugger): RootJsonReader[prediction.LoadPrediction] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new prediction.LoadPrediction(fields("path").convertTo[String], fields("format").convertTo[PredictionSource])
  }

  implicit def predictReader(implicit debugger: Debugger): RootJsonReader[ruleset.Predict] = (json: JsValue) => {
    val selector = json.toSelector
    new ruleset.Predict(
      selector.get("testPath").toOpt[String],
      selector.get("mergeTestAndTrainForPrediction").toOpt[Boolean].getOrElse(true),
      selector.get("onlyTestCoveredPredictions").toOpt[Boolean].getOrElse(true),
      selector.get("predictedResults").toTypedIterable[PredictedResult].toSet,
      selector.get("injectiveMapping").toOpt[Boolean].getOrElse(true)
    )
  }

  implicit def pruneReader(implicit debugger: Debugger): RootJsonReader[ruleset.Prune] = (json: JsValue) => {
    new ruleset.Prune(json.convertTo[ruleset.Prune.PruningStrategy])
  }

  implicit val filterRulesReader: RootJsonReader[ruleset.FilterRules] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    val selector = json.toSelector
    new ruleset.FilterRules(
      fields.get("measures").iterator.flatMap(_.convertTo[JsArray].elements).map { json =>
        val fields = json.asJsObject.fields
        fields("name") -> fields("value").convertTo[String]
      }.collect {
        case (JsString("RuleLength"), TripleItemMatcher.Number(x)) => None -> x
        case (measure, TripleItemMatcher.Number(x)) => Some(measure.convertTo[TypedKeyMap.Key[Measure]]) -> x
      }.toSeq,
      fields.get("patterns").map(_.convertTo[JsArray].elements.map(_.convertTo[RulePattern])).getOrElse(Nil),
      selector.get("tripleMatchers").toIterable.flatMap { selector =>
        selector.toOpt[TripleMatcher].zip(selector.get("inverse").toOpt[Boolean].orElse(Some(false)))
      }.toSeq,
      fields.get("indices").map(_.convertTo[JsArray].elements.map(_.convertTo[Int]).toSet).getOrElse(Set.empty)
    )
  }

  implicit val filterPredictionReader: RootJsonReader[prediction.Filter] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    val selector = json.toSelector
    new prediction.Filter(
      selector.get("predictedResults").toTypedIterable[PredictedResult].toSet,
      selector.get("tripleMatchers").toIterable.flatMap { selector =>
        selector.toOpt[TripleMatcher].zip(selector.get("inverse").toOpt[Boolean].orElse(Some(false)))
      }.toSeq,
      fields.get("measures").iterator.flatMap(_.convertTo[JsArray].elements).map { json =>
        val fields = json.asJsObject.fields
        fields("name") -> fields("value").convertTo[String]
      }.collect {
        case (JsString("RuleLength"), TripleItemMatcher.Number(x)) => None -> x
        case (measure, TripleItemMatcher.Number(x)) => Some(measure.convertTo[TypedKeyMap.Key[Measure]]) -> x
      }.toSeq,
      fields.get("patterns").map(_.convertTo[JsArray].elements.map(_.convertTo[RulePattern])).getOrElse(Nil),
      selector.get("distinctPredictions").toOpt[Boolean].getOrElse(false),
      selector.get("withoutTrainTriples").toOpt[Boolean].getOrElse(false),
      selector.get("onlyCoveredTestPredictionTasks").toOpt[Boolean].getOrElse(false),
      fields.get("indices").map(_.convertTo[JsArray].elements.map(_.convertTo[Int]).toSet).getOrElse(Set.empty)
    )
  }

  implicit val filterPredictionTasksReader: RootJsonReader[predictionTasks.Filter] = (json: JsValue) => {
    val selector = json.toSelector
    new predictionTasks.Filter(
      selector.get("nonEmptyTest").toOpt[Boolean].getOrElse(false),
      selector.get("nonEmptyPredictions").toOpt[Boolean].getOrElse(false)
    )
  }

  implicit val selectPredictionsReader: RootJsonReader[predictionTasks.Select] = (json: JsValue) => {
    val selector = json.toSelector
    new predictionTasks.Select(selector("strategy").to[SelectionStrategy])
  }

  implicit val withModesReader: RootJsonReader[predictionTasks.WithModes] = (_: JsValue) => {
    new predictionTasks.WithModes()
  }

  implicit def groupPredictionsReader(implicit debugger: Debugger): RootJsonReader[prediction.Group] = (json: JsValue) => {
    val selector = json.toSelector
    new prediction.Group(
      selector.get("scorer").toOpt[PredictedTriplesAggregator.ScoreFactory].getOrElse(PredictedTriplesAggregator.EmptyScoreFactory),
      selector.get("consumer").toOpt[PredictedTriplesAggregator.RulesFactory].getOrElse(PredictedTriplesAggregator.EmptyRulesFactory),
      selector.get("limit").toOpt[Int].getOrElse(-1)
    )
  }

  implicit val evaluateReader: RootJsonReader[Evaluate] = (json: JsValue) => {
    val selector = json.toSelector
    new Evaluate(selector("ranking").to[RankingStrategy])
  }

  implicit val rulesetShrinkReader: RootJsonReader[ruleset.Shrink] = (json: JsValue) => {
    new ruleset.Shrink(json.convertTo[ShrinkSetup])
  }

  implicit val predictionShrinkReader: RootJsonReader[prediction.Shrink] = (json: JsValue) => {
    new prediction.Shrink(json.convertTo[ShrinkSetup])
  }

  implicit val predictionTasksShrinkReader: RootJsonReader[predictionTasks.Shrink] = (json: JsValue) => {
    new predictionTasks.Shrink(json.convertTo[ShrinkSetup])
  }

  implicit val rulesetSortReader: RootJsonReader[ruleset.Sort] = (json: JsValue) => {
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

  implicit val predictionSortReader: RootJsonReader[prediction.Sort] = (_: JsValue) => {
    new prediction.Sort
  }

  implicit def computeConfidenceReader(implicit debugger: Debugger): RootJsonReader[ruleset.ComputeConfidence] = (json: JsValue) => {
    new ruleset.ComputeConfidence(json.convertTo[ConfidenceType])
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

  implicit def cachePredictionReader(implicit debugger: Debugger): RootJsonReader[prediction.Cache] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new prediction.Cache(fields("path").convertTo[String], fields("inMemory").convertTo[Boolean], fields("revalidate").convertTo[Boolean])
  }

  implicit def cachePredictionTasksReader(implicit debugger: Debugger): RootJsonReader[predictionTasks.Cache] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new predictionTasks.Cache(fields("path").convertTo[String], fields("revalidate").convertTo[Boolean])
  }

  implicit val instantiateReader: RootJsonReader[ruleset.Instantiate] = (json: JsValue) => {
    val selector = json.toSelector
    new ruleset.Instantiate(selector.get("predictedResults").toTypedIterable[PredictedResult].toSet, selector("injectiveMapping").to[Boolean])
  }

  implicit val graphAwareRulesReader: RootJsonReader[ruleset.GraphAwareRules] = (_: JsValue) => {
    new ruleset.GraphAwareRules()
  }

  implicit val exportRulesReader: RootJsonReader[ruleset.ExportRules] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new ruleset.ExportRules(
      fields("path").convertTo[String],
      fields("format").convertTo[RulesetSource]
    )
  }

  implicit val propertiesCardinalitiesReader: RootJsonReader[index.PropertiesCardinalities] = (json: JsValue) => {
    val selector = json.toSelector
    new index.PropertiesCardinalities(selector.get("filter").toTypedIterable[TripleItem.Uri].toSet)
  }

  implicit val getRulesReader: RootJsonReader[ruleset.GetRules] = (_: JsValue) => {
    new ruleset.GetRules
  }

  implicit val getPredictionReader: RootJsonReader[prediction.GetPrediction] = (_: JsValue) => {
    new GetPrediction
  }

  implicit val getPredictionTasksReader: RootJsonReader[predictionTasks.GetPredictionTasks] = (_: JsValue) => {
    new predictionTasks.GetPredictionTasks
  }

  implicit val rulesetSizeReader: RootJsonReader[ruleset.Size] = (_: JsValue) => {
    new ruleset.Size
  }

  implicit val predictionSizeReader: RootJsonReader[prediction.Size] = (_: JsValue) => {
    new prediction.Size
  }

  implicit val predictionTasksSizeReader: RootJsonReader[predictionTasks.Size] = (_: JsValue) => {
    new predictionTasks.Size
  }

  implicit val pipelineReader: RootJsonReader[Debugger => Pipeline[Source[JsValue, NotUsed]]] = (json: JsValue) => { implicit debugger =>
    def addInput(head: JsValue, tail: Seq[JsValue]): Pipeline[Source[JsValue, NotUsed]] = {
      val fields = head.asJsObject.fields
      val params = fields("parameters")
      fields("name").convertTo[String] match {
        case data.LoadDataset.name => addTaskFromDataset(Pipeline(params.convertTo[data.LoadDataset]), tail)
        case data.LoadGraph.name => addTaskFromDataset(Pipeline(params.convertTo[data.LoadGraph]), tail)
        case index.LoadIndex.name => addTaskFromIndex(Pipeline(params.convertTo[index.LoadIndex]), tail)
        case ruleset.LoadRulesetWithoutIndex.name => addTaskFromRuleset(Pipeline(params.convertTo[ruleset.LoadRulesetWithoutIndex]), tail)
        case prediction.LoadPredictionWithoutIndex.name => addTaskFromPrediction(Pipeline(params.convertTo[prediction.LoadPredictionWithoutIndex]), tail)
        case x => deserializationError(s"Invalid first task: $x")
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
          case data.Split.name => addTaskFromDataset(pipeline ~> params.convertTo[data.Split], tail)
          case data.Properties.name => pipeline ~> params.convertTo[data.Properties] ~> ToJsonTask.FromTypes
          case data.Index.name => addTaskFromIndex(pipeline ~> params.convertTo[data.Index], tail)
          case MergeDatasets.name => addTaskFromDataset(pipeline |~> params.convertTo[MergeDatasets], tail)
          case x => deserializationError(s"Invalid task '$x' can not be bound to Dataset")
        }
      case _ => pipeline ~> new ToJsonTask.From[Dataset]
    }

    @scala.annotation.tailrec
    def addTaskFromPrediction(pipeline: Pipeline[PredictedTriples], tail: Seq[JsValue]): Pipeline[Source[JsValue, NotUsed]] = tail match {
      case Seq(head, tail@_*) =>
        val fields = head.asJsObject.fields
        val params = fields("parameters")
        fields("name").convertTo[String] match {
          case prediction.Cache.name => addTaskFromPrediction(pipeline ~> params.convertTo[prediction.Cache], tail)
          case prediction.Filter.name => addTaskFromPrediction(pipeline ~> params.convertTo[prediction.Filter], tail)
          case prediction.Group.name => addTaskFromPrediction(pipeline ~> params.convertTo[prediction.Group], tail)
          case prediction.GetPrediction.name => pipeline ~> params.convertTo[prediction.GetPrediction] ~> ToJsonTask.FromPredictedTriple
          case prediction.ToDataset.name => addTaskFromDataset(pipeline ~> params.convertTo[prediction.ToDataset], tail)
          case prediction.Size.name => pipeline ~> params.convertTo[prediction.Size] ~> ToJsonTask.FromInt
          case prediction.ExportPrediction.name => pipeline ~> params.convertTo[prediction.ExportPrediction] ~> ToJsonTask.FromUnit
          case prediction.Shrink.name => addTaskFromPrediction(pipeline ~> params.convertTo[prediction.Shrink], tail)
          case prediction.Sort.name => addTaskFromPrediction(pipeline ~> params.convertTo[prediction.Sort], tail)
          case prediction.ToPredictionTasks.name => addTaskFromPredictionTasks(pipeline ~> params.convertTo[prediction.ToPredictionTasks], tail)
          case x => deserializationError(s"Invalid task '$x' can not be bound to Prediction")
        }
      case _ => pipeline ~> new ToJsonTask.From[PredictedTriples]
    }

    @scala.annotation.tailrec
    def addTaskFromPredictionTasks(pipeline: Pipeline[PredictionTasksResults], tail: Seq[JsValue]): Pipeline[Source[JsValue, NotUsed]] = tail match {
      case Seq(head, tail@_*) =>
        val fields = head.asJsObject.fields
        val params = fields("parameters")
        fields("name").convertTo[String] match {
          case predictionTasks.Cache.name => addTaskFromPredictionTasks(pipeline ~> params.convertTo[predictionTasks.Cache], tail)
          case predictionTasks.Filter.name => addTaskFromPredictionTasks(pipeline ~> params.convertTo[predictionTasks.Filter], tail)
          case predictionTasks.Select.name => addTaskFromPredictionTasks(pipeline ~> params.convertTo[predictionTasks.Select], tail)
          case predictionTasks.WithModes.name => addTaskFromPredictionTasks(pipeline ~> params.convertTo[predictionTasks.WithModes], tail)
          case predictionTasks.Evaluate.name => pipeline ~> params.convertTo[predictionTasks.Evaluate] ~> ToJsonTask.FromEvaluationResult
          case predictionTasks.GetPredictionTasks.name => pipeline ~> params.convertTo[predictionTasks.GetPredictionTasks] ~> ToJsonTask.FromPredictionTaskResult
          case predictionTasks.ToDataset.name => addTaskFromDataset(pipeline ~> params.convertTo[predictionTasks.ToDataset], tail)
          case predictionTasks.ToPredictions.name => addTaskFromPrediction(pipeline ~> params.convertTo[predictionTasks.ToPredictions], tail)
          case predictionTasks.Size.name => pipeline ~> params.convertTo[predictionTasks.Size] ~> ToJsonTask.FromInt
          case predictionTasks.Shrink.name => addTaskFromPredictionTasks(pipeline ~> params.convertTo[predictionTasks.Shrink], tail)
          case x => deserializationError(s"Invalid task '$x' can not be bound to Prediction")
        }
      case _ => pipeline ~> new ToJsonTask.From[PredictionTasksResults]
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
          case index.PropertiesCardinalities.name => pipeline ~> params.convertTo[index.PropertiesCardinalities] ~> ToJsonTask.FromPropertiesCardinalities
          case index.ExportIndex.name => pipeline ~> params.convertTo[index.ExportIndex] ~> ToJsonTask.FromUnit
          case ruleset.LoadRuleset.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.LoadRuleset], tail)
          case prediction.LoadPrediction.name => addTaskFromPrediction(pipeline ~> params.convertTo[prediction.LoadPrediction], tail)
          case x => deserializationError(s"Invalid task '$x' can not be bound to Index")
        }
      case _ => pipeline ~> new ToJsonTask.From[Index]
    }

    @scala.annotation.tailrec
    def addTaskFromRuleset(pipeline: Pipeline[Ruleset], tail: Seq[JsValue]): Pipeline[Source[JsValue, NotUsed]] = tail match {
      case Seq(head, tail@_*) =>
        val fields = head.asJsObject.fields
        val params = fields("parameters")
        fields("name").convertTo[String] match {
          case ruleset.Cache.name | s"${ruleset.Cache.name}Action" => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.Cache], tail)
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
          case ruleset.Predict.name => addTaskFromPrediction(pipeline ~> params.convertTo[ruleset.Predict], tail)
          case ruleset.Prune.name => addTaskFromRuleset(pipeline ~> params.convertTo[ruleset.Prune], tail)
          case ruleset.Instantiate.name => pipeline ~> params.convertTo[ruleset.Instantiate] ~> ToJsonTask.FromInstantiatedRules
          case x => deserializationError(s"Invalid task '$x' can not be bound to Ruleset")
        }
      case _ => pipeline ~> new ToJsonTask.From[Ruleset]
    }

    json match {
      case JsArray(Vector(head, tail@_*)) => addInput(head, tail)
      case _ => deserializationError("No tasks defined")
    }
  }

}