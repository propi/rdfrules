package com.github.propi.rdfrules.http.formats

import com.github.propi.rdfrules.algorithm.amie.Amie
import com.github.propi.rdfrules.algorithm.consumer.{InMemoryRuleConsumer, OnDiskRuleConsumer, TopKRuleConsumer}
import com.github.propi.rdfrules.algorithm.clustering.SimilarityCounting.{Comb, WeightedSimilarityCounting}
import com.github.propi.rdfrules.algorithm.clustering.{DbScan, SimilarityCounting}
import com.github.propi.rdfrules.algorithm.{Clustering, RuleConsumer, RulesMining}
import com.github.propi.rdfrules.data.{Compression, DiscretizationTask, Prefix, RdfSource, TripleItem}
import com.github.propi.rdfrules.http.formats.CommonDataJsonFormats._
import com.github.propi.rdfrules.http.task._
import com.github.propi.rdfrules.http.task.ruleset.ComputeConfidence.ConfidenceType
import com.github.propi.rdfrules.http.task.ruleset.Prune.PruningStrategy
import com.github.propi.rdfrules.http.util.Conf
import com.github.propi.rdfrules.http.{Main, Workspace}
import com.github.propi.rdfrules.prediction.PredictionSource
import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.rule.RuleConstraint.ConstantsAtPosition.ConstantsPosition
import com.github.propi.rdfrules.rule.{AtomPattern, Measure, Rule, RuleConstraint, RulePattern, Threshold}
import com.github.propi.rdfrules.ruleset.{Ruleset, RulesetSource}
import com.github.propi.rdfrules.utils.JsonSelector.PimpedJsValue
import com.github.propi.rdfrules.utils.{Debugger, TypedKeyMap}
import org.apache.jena.riot.RDFFormat
import spray.json.DefaultJsonProtocol._
import spray.json._

import java.io.File
import java.net.URL
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try

/**
  * Created by Vaclav Zeman on 14. 8. 2018.
  */
object CommonDataJsonReaders {

  private val defaultMaxMiningTime = Conf[Duration](Main.confPrefix + ".default-max-mining-time").toOption.getOrElse(0 seconds)

  implicit val tripleItemReader: RootJsonReader[TripleItem] = (json: JsValue) => Try(tripleItemUriReader.read(json)).getOrElse(json match {
    case JsString(TripleItemMapper.Resource(uri)) => uri
    case JsString(TripleItemMapper.Text(text)) => text
    case JsString(TripleItemMapper.Number(number)) => number
    case JsNumber(x) => TripleItem.Number(x.doubleValue)
    case JsString(TripleItemMapper.BooleanValue(booleanValue)) => booleanValue
    case JsBoolean(x) => TripleItem.BooleanValue(x)
    case JsString(TripleItemMapper.Interval(interval)) => interval
    case json => deserializationError(s"Json value '$json' can not be deserialized as a triple item.")
  })

  implicit val tripleItemUriReader: RootJsonReader[TripleItem.Uri] = {
    case JsString(TripleItemMapper.Resource(uri)) => uri
    case JsObject(fields) if List("prefix", "nameSpace", "localName").forall(fields.contains) =>
      val shortPrefix = fields("prefix").convertTo[String]
      val prefix = if (shortPrefix.isEmpty) Prefix(fields("nameSpace").convertTo[String]) else Prefix(shortPrefix, fields("nameSpace").convertTo[String])
      TripleItem.PrefixedUri(prefix, fields("localName").convertTo[String])
    case json => deserializationError(s"Json value '$json' can not be deserialized as the URI.")
  }

  implicit val urlReader: RootJsonReader[URL] = {
    case JsString(x) => new URL(x)
    case json => deserializationError(s"Json value '$json' can not be deserialized as the URL.")
  }

  implicit val compressionReader: RootJsonReader[Compression] = {
    case JsString("gz") => Compression.GZ
    case JsString("bz2") => Compression.BZ2
    case json => deserializationError(s"Json value '$json' can not be deserialized as the compression type.")
  }

  implicit val rdfSourceReader: RootJsonReader[RdfSource] = {
    case JsString(x) => RdfSource(x)
    case json => deserializationError(s"Json value '$json' can not be deserialized as the RDF source.")
  }

  implicit val quadMatcherReader: RootJsonReader[QuadMatcher] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    QuadMatcher(
      fields.get("subject").map(_.convertTo[String]),
      fields.get("predicate").map(_.convertTo[String]),
      fields.get("object").map(_.convertTo[String]),
      fields.get("graph").map(_.convertTo[String])
    )
  }

  implicit val tripleMatcherReader: RootJsonReader[TripleMatcher] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    TripleMatcher(
      fields.get("subject").map(_.convertTo[String]),
      fields.get("predicate").map(_.convertTo[String]),
      fields.get("object").map(_.convertTo[String])
    )
  }

  implicit val quadMapperReader: RootJsonReader[QuadMapper] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    new QuadMapper(
      fields.get("subject").map(_.convertTo[String]),
      fields.get("predicate").map(_.convertTo[String]),
      fields.get("object").map(_.convertTo[String]),
      fields.get("graph").map(_.convertTo[String])
    )
  }

  implicit val discretizationTaskModeReader: RootJsonReader[DiscretizationTask.Mode] = (json: JsValue) => json.convertTo[String].toLowerCase match {
    case "inmemory" => DiscretizationTask.Mode.InMemory
    case "external" => DiscretizationTask.Mode.External
    case x => deserializationError(s"Invalid discretization task mode: $x")
  }

  implicit val discretizationTaskReader: RootJsonReader[DiscretizationTask] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    fields("name").convertTo[String] match {
      case "EquidistanceDiscretizationTask" => DiscretizationTask.Equidistance(fields("bins").convertTo[Int])
      case "EquifrequencyDiscretizationTask" => DiscretizationTask.Equifrequency(
        fields("bins").convertTo[Int],
        fields.get("buffer").map(_.convertTo[Int]).getOrElse(15000000),
        fields.get("mode").map(_.convertTo[DiscretizationTask.Mode]).getOrElse(DiscretizationTask.Mode.External)
      )
      case "EquisizeDiscretizationTask" => DiscretizationTask.Equisize(
        fields("support").convertTo[Double],
        fields.get("buffer").map(_.convertTo[Int]).getOrElse(15000000),
        fields.get("mode").map(_.convertTo[DiscretizationTask.Mode]).getOrElse(DiscretizationTask.Mode.External)
      )
      case x => deserializationError(s"Invalid discretization task name: $x")
    }
  }

  implicit val rdfFormatReader: RootJsonReader[RDFFormat] = (json: JsValue) => json.convertTo[String] match {
    case "nt" => RDFFormat.NTRIPLES_UTF8
    case "nq" => RDFFormat.NQUADS_UTF8
    case "ttl" => RDFFormat.TURTLE_FLAT
    case "trig" => RDFFormat.TRIG_BLOCKS
    case "trix" => RDFFormat.TRIX
    case x => deserializationError(s"Invalid RDF output format: $x")
  }

  implicit val thresholdReader: RootJsonReader[Threshold] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    fields("name").convertTo[String] match {
      case "MinHeadSize" => Threshold.MinHeadSize(fields("value").convertTo[Int])
      case "MinAtomSize" => Threshold.MinAtomSize(fields("value").convertTo[Int])
      case "MinHeadCoverage" => Threshold.MinHeadCoverage(fields("value").convertTo[Double])
      case "MinSupport" => Threshold.MinSupport(fields("value").convertTo[Int])
      case "MaxRuleLength" => Threshold.MaxRuleLength(fields("value").convertTo[Int])
      //case "TopK" => Threshold.TopK(fields("value").convertTo[Int])
      case "Timeout" => Threshold.Timeout(fields("value").convertTo[Int])
      case "LocalTimeout" => Threshold.LocalTimeout(FiniteDuration(fields("value").convertTo[Int], MILLISECONDS), fields("me").convertTo[Double], fields("dme").convertTo[Boolean])
      case x => deserializationError(s"Invalid threshold name: $x")
    }
  }

  implicit val atomItemPatternReader: RootJsonReader[AtomPattern.AtomItemPattern] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    fields("name").convertTo[String] match {
      case "Any" => AtomPattern.AtomItemPattern.Any
      case "AnyConstant" => AtomPattern.AtomItemPattern.AnyConstant
      case "AnyVariable" => AtomPattern.AtomItemPattern.AnyVariable
      case "Constant" => AtomPattern.AtomItemPattern(fields("value").convertTo[TripleItem])
      case "Variable" => AtomPattern.AtomItemPattern(fields("value").convertTo[Char])
      case "OneOf" => AtomPattern.AtomItemPattern.OneOf(fields("value").convertTo[JsArray].elements.map(x => AtomPattern.AtomItemPattern.Constant(x.convertTo[TripleItem])))
      case "NoneOf" => AtomPattern.AtomItemPattern.NoneOf(fields("value").convertTo[JsArray].elements.map(x => AtomPattern.AtomItemPattern.Constant(x.convertTo[TripleItem])))
      case x => deserializationError(s"Invalid atom item pattern name: $x")
    }
  }

  implicit val atomPatternReader: RootJsonReader[AtomPattern] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    AtomPattern(
      fields.get("subject").map(_.convertTo[AtomPattern.AtomItemPattern]).getOrElse(AtomPattern.AtomItemPattern.Any),
      fields.get("predicate").map(_.convertTo[AtomPattern.AtomItemPattern]).getOrElse(AtomPattern.AtomItemPattern.Any),
      fields.get("object").map(_.convertTo[AtomPattern.AtomItemPattern]).getOrElse(AtomPattern.AtomItemPattern.Any),
      fields.get("graph").map(_.convertTo[AtomPattern.AtomItemPattern]).getOrElse(AtomPattern.AtomItemPattern.Any)
    )
  }

  implicit val patternReader: RootJsonReader[RulePattern] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    RulePattern.apply(
      fields.get("body").collect {
        case JsArray(x) => x.map(_.convertTo[AtomPattern])
      }.getOrElse(Vector.empty),
      fields.get("head").map(_.convertTo[AtomPattern]),
      fields.get("exact").exists(_.convertTo[Boolean]),
      fields.get("orderless").exists(_.convertTo[Boolean])
    )
  }

  implicit val constraintReader: RootJsonReader[RuleConstraint] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    fields("name").convertTo[String] match {
      case "WithoutConstants" => RuleConstraint.ConstantsAtPosition(ConstantsPosition.Nowhere)
      case "OnlyObjectConstants" => RuleConstraint.ConstantsAtPosition(ConstantsPosition.Object)
      case "OnlySubjectConstants" => RuleConstraint.ConstantsAtPosition(ConstantsPosition.Subject)
      case "OnlyLowerCardinalitySideConstants" => RuleConstraint.ConstantsAtPosition(ConstantsPosition.LowerCardinalitySide())
      case "OnlyLowerCardinalitySideAtHeadConstants" => RuleConstraint.ConstantsAtPosition(ConstantsPosition.LowerCardinalitySide(true))
      case "WithoutDuplicitPredicates" => RuleConstraint.WithoutDuplicatePredicates()
      case "OnlyPredicates" => RuleConstraint.OnlyPredicates(fields("values").convertTo[JsArray].elements.map(_.convertTo[TripleItem.Uri]).toSet)
      case "WithoutPredicates" => RuleConstraint.WithoutPredicates(fields("values").convertTo[JsArray].elements.map(_.convertTo[TripleItem.Uri]).toSet)
      case x => deserializationError(s"Invalid rule constraint name: $x")
    }
  }

  implicit def rulesMiningReader(implicit debugger: Debugger): RootJsonReader[RulesMining] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    Function.chain[RulesMining](List(
      rm => if (defaultMaxMiningTime.toMinutes > 0) {
        rm.addThreshold(Threshold.Timeout(defaultMaxMiningTime.toMinutes.toInt))
      } else {
        rm
      },
      rm => fields.get("thresholds").collect {
        case JsArray(x) => x.map(_.convertTo[Threshold]).foldLeft(rm)(_ addThreshold _)
      }.getOrElse(rm),
      rm => fields.get("patterns").collect {
        case JsArray(x) => x.map(_.convertTo[RulePattern]).foldLeft(rm)(_ addPattern _)
      }.getOrElse(rm),
      rm => fields.get("constraints").collect {
        case JsArray(x) => x.map(_.convertTo[RuleConstraint]).foldLeft(rm)(_ addConstraint _)
      }.getOrElse(rm),
      rm => fields.get("parallelism").map(x => rm.setParallelism(x.convertTo[Int])).getOrElse(rm)
    ))(Amie())
  }

  implicit val ruleConsumerReader: RootJsonReader[RuleConsumer.Invoker[Ruleset]] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    fields("name").convertTo[String] match {
      case "inMemory" => RuleConsumer(InMemoryRuleConsumer())
      case "onDisk" =>
        val file = new File(Workspace.path(fields("file").convertTo[String]))
        val format = fields("format").convertTo[RulesetSource]
        RuleConsumer.withMapper[Ruleset](implicit mapper => OnDiskRuleConsumer(format(file)))
      case "topK" =>
        val k = fields("k").convertTo[Int]
        val allowOverflow = fields.get("allowOverflow").exists(_.convertTo[Boolean])
        RuleConsumer(TopKRuleConsumer(k, allowOverflow))
      case x => deserializationError(s"Invalid type of rule consumer: $x")
    }
  }

  implicit val measureKeyReader: RootJsonReader[TypedKeyMap.Key[Measure]] = (json: JsValue) => json.convertTo[String] match {
    case "HeadSize" => Measure.HeadSize
    case "Support" => Measure.Support
    case "HeadCoverage" => Measure.HeadCoverage
    case "BodySize" => Measure.BodySize
    case "Confidence" => Measure.CwaConfidence
    case "PcaConfidence" => Measure.PcaConfidence
    case "PcaBodySize" => Measure.PcaBodySize
    case "QpcaConfidence" => Measure.QpcaConfidence
    case "QpcaBodySize" => Measure.QpcaBodySize
    case "Lift" => Measure.Lift
    case "Cluster" => Measure.Cluster
    case x => deserializationError(s"Invalid measure name: $x")
  }

  implicit val weightedSimilarityCountingReader: RootJsonReader[WeightedSimilarityCounting[Rule]] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    val weight = fields("weight").convertTo[Double]
    fields("name").convertTo[String] match {
      case "Atoms" => weight * SimilarityCounting.AtomsSimilarityCounting
      case "Support" => weight * SimilarityCounting.SupportSimilarityCounting
      case "Confidence" => weight * SimilarityCounting.ConfidenceSimilarityCounting
      case "PcaConfidence" => weight * SimilarityCounting.PcaConfidenceSimilarityCounting
      case "Lift" => weight * SimilarityCounting.LiftSimilarityCounting
      case "Length" => weight * SimilarityCounting.LengthSimilarityCounting
      case x => deserializationError(s"Invalid feature name for clustering: $x")
    }
  }

  implicit val similarityCountingReader: RootJsonReader[SimilarityCounting[Rule]] = (json: JsValue) => json.convertTo[JsArray].elements
    .map(_.convertTo[WeightedSimilarityCounting[Rule]])
    .foldLeft(Option.empty[Comb[Rule]]) {
      case (None, x) => Some(x)
      case (Some(x), y) => Some(x ~ y)
    }.getOrElse(implicitly[SimilarityCounting[Rule]])

  implicit def clusteringReader(implicit debugger: Debugger): RootJsonReader[Clustering[FinalRule]] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    DbScan(
      fields.get("minNeighbours").map(_.convertTo[Int]).getOrElse(2),
      fields.get("minSimilarity").map(_.convertTo[Double]).getOrElse(0.85)
    )(fields.get("features").map(_.convertTo[SimilarityCounting[Rule]]).getOrElse(implicitly[SimilarityCounting[Rule]]), debugger)
  }

  /*implicit val resolvedRuleAtomItemReader: RootJsonReader[ResolvedItem] = {
    case JsObject(fields) => fields("type").convertTo[String] match {
      case "variable" => ResolvedItem(fields("value").convertTo[String])
      case "constant" => ResolvedItem(fields("value").convertTo[TripleItem])
    }
    case JsString(x) if x.startsWith("?") => ResolvedItem(x)
    case x: JsValue => ResolvedItem(x.convertTo[TripleItem])
  }

  implicit val resolvedRuleAtomReader: RootJsonReader[ResolvedAtom] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    ResolvedAtom(
      fields("subject").convertTo[ResolvedItem],
      fields("predicate").convertTo[TripleItem.Uri],
      fields("object").convertTo[ResolvedItem]
    )
  }

  implicit val measureReader: RootJsonReader[Measure] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    fields("name").convertTo[TypedKeyMap.Key[Measure]] match {
      case Measure.HeadSize => Measure.HeadSize(fields("value").convertTo[Int])
      case Measure.Support => Measure.Support(fields("value").convertTo[Int])
      case Measure.HeadCoverage => Measure.HeadCoverage(fields("value").convertTo[Double])
      case Measure.BodySize => Measure.BodySize(fields("value").convertTo[Int])
      case Measure.Confidence => Measure.Confidence(fields("value").convertTo[Double])
      case Measure.PcaConfidence => Measure.PcaConfidence(fields("value").convertTo[Double])
      case Measure.PcaBodySize => Measure.PcaBodySize(fields("value").convertTo[Int])
      case Measure.HeadConfidence => Measure.HeadConfidence(fields("value").convertTo[Double])
      case Measure.Lift => Measure.Lift(fields("value").convertTo[Double])
      case Measure.Cluster => Measure.Cluster(fields("value").convertTo[Int])
      case x => deserializationError(s"Invalid measure: $x")
    }
  }

  implicit val resolvedRuleReader: RootJsonReader[ResolvedRule] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    ResolvedRule(
      fields("body").convertTo[JsArray].elements.map(_.convertTo[ResolvedAtom]),
      fields("head").convertTo[ResolvedAtom]
    )(TypedKeyMap(fields.get("measures").iterator.flatMap(_.convertTo[JsArray].elements).map(_.convertTo[Measure])))
  }*/

  implicit val predictionSourceReader: RootJsonReader[PredictionSource] = (json: JsValue) => json.convertTo[String] match {
    case "ndjson" => PredictionSource.NDJson
    case "json" => PredictionSource.Json
    case "cache" => PredictionSource.Cache
    case x => deserializationError(s"Invalid prediction format name: $x")
  }

  implicit val rulesetSourceReader: RootJsonReader[RulesetSource] = (json: JsValue) => json.convertTo[String] match {
    case "txt" => RulesetSource.Text
    case "json" => RulesetSource.Json
    case "ndjson" => RulesetSource.NDJson
    case "cache" => RulesetSource.Cache
    case x => deserializationError(s"Invalid ruleset format name: $x")
  }

  implicit val shrinkSetupReader: RootJsonReader[ShrinkSetup] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    fields.get("take").map(x => ShrinkSetup.Take(x.convertTo[Int]))
      .orElse(fields.get("drop").map(x => ShrinkSetup.Drop(x.convertTo[Int])))
      .getOrElse(ShrinkSetup.Slice(fields("start").convertTo[Int], fields("end").convertTo[Int]))
  }

  implicit val pruningStrategyReader: RootJsonReader[PruningStrategy] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    val selector = json.toSelector
    selector("strategy").to[String].map {
      case "DataCoveragePruning" => json.convertTo[PruningStrategy.DataCoveragePruning]
      case "Maximal" => PruningStrategy.Maximal
      case "Closed" => PruningStrategy.Closed(fields("measure").convertTo[TypedKeyMap.Key[Measure]])
      case "OnlyBetterDescendant" => PruningStrategy.OnlyBetterDescendant(fields("measure").convertTo[TypedKeyMap.Key[Measure]])
      case "WithoutQuasiBinding" => PruningStrategy.WithoutQuasiBinding(fields("injectiveMapping").convertTo[Boolean])
      case x => deserializationError(s"Invalid name of pruning strategy: $x")
    }.get
  }

  implicit val confidenceTypeReader: RootJsonReader[ConfidenceType] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    val selector = json.toSelector
    selector("name").to[String].map {
      case "StandardConfidence" => ConfidenceType.StandardConfidence(fields("min").convertTo[Double], fields.get("topk").map(_.convertTo[Int]).getOrElse(0))
      case "PcaConfidence" => ConfidenceType.PcaConfidence(fields("min").convertTo[Double], fields.get("topk").map(_.convertTo[Int]).getOrElse(0))
      case "QpcaConfidence" => ConfidenceType.QpcaConfidence(fields("min").convertTo[Double], fields.get("topk").map(_.convertTo[Int]).getOrElse(0))
      case "Lift" => ConfidenceType.Lift(fields("min").convertTo[Double])
      case x => deserializationError(s"Invalid name of confidence type: $x")
    }.get
  }

}