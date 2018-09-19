package com.github.propi.rdfrules.http.formats

import java.net.URL

import com.github.propi.rdfrules.algorithm.amie.Amie
import com.github.propi.rdfrules.algorithm.dbscan.SimilarityCounting.{Comb, WeightedSimilarityCounting}
import com.github.propi.rdfrules.algorithm.dbscan.{DbScan, SimilarityCounting}
import com.github.propi.rdfrules.algorithm.{Clustering, RulesMining}
import com.github.propi.rdfrules.data.{DiscretizationTask, RdfSource, TripleItem}
import com.github.propi.rdfrules.http.task.{QuadMapper, QuadMatcher, TripleItemMapper}
import com.github.propi.rdfrules.rule.{AtomPattern, Measure, Rule, RuleConstraint, RulePattern, Threshold}
import com.github.propi.rdfrules.ruleset.{ResolvedRule, RulesetSource}
import com.github.propi.rdfrules.utils.{Debugger, TypedKeyMap}
import org.apache.jena.riot.RDFFormat
import spray.json.DefaultJsonProtocol._
import spray.json._

/**
  * Created by Vaclav Zeman on 14. 8. 2018.
  */
object CommonDataJsonReaders {

  implicit val tripleItemReader: RootJsonReader[TripleItem] = {
    case JsString(TripleItemMapper.Resource(uri)) => uri
    case JsString(TripleItemMapper.Text(text)) => text
    case JsString(TripleItemMapper.Number(number)) => number
    case JsNumber(x) => TripleItem.Number(x.doubleValue())
    case JsString(TripleItemMapper.BooleanValue(booleanValue)) => booleanValue
    case JsBoolean(x) => TripleItem.BooleanValue(x)
    case JsString(TripleItemMapper.Interval(interval)) => interval
    case json => deserializationError(s"Json value '$json' can not be deserialized as a triple item.")
  }

  implicit val tripleItemUriReader: RootJsonReader[TripleItem.Uri] = {
    case JsString(TripleItemMapper.Resource(uri)) => uri
    case json => deserializationError(s"Json value '$json' can not be deserialized as the URI.")
  }

  implicit val urlReader: RootJsonReader[URL] = {
    case JsString(x) => new URL(x)
    case json => deserializationError(s"Json value '$json' can not be deserialized as the URL.")
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
      case "MinHeadCoverage" => Threshold.MinHeadCoverage(fields("value").convertTo[Double])
      case "MaxRuleLength" => Threshold.MaxRuleLength(fields("value").convertTo[Int])
      case "TopK" => Threshold.TopK(fields("value").convertTo[Int])
      case "Timeout" => Threshold.Timeout(fields("value").convertTo[Int])
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
      case "OneOf" => AtomPattern.AtomItemPattern.OneOf(fields("value").convertTo[JsArray].elements.map(_.convertTo[AtomPattern.AtomItemPattern]))
      case "NoneOf" => AtomPattern.AtomItemPattern.NoneOf(fields("value").convertTo[JsArray].elements.map(_.convertTo[AtomPattern.AtomItemPattern]))
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
      fields.get("exact").exists(_.convertTo[Boolean])
    )
  }

  implicit val constraintReader: RootJsonReader[RuleConstraint] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    fields("name").convertTo[String] match {
      case "WithInstances" => RuleConstraint.WithInstances(false)
      case "WithInstancesOnlyObjects" => RuleConstraint.WithInstances(true)
      case "WithoutDuplicitPredicates" => RuleConstraint.WithoutDuplicitPredicates()
      case "OnlyPredicates" => RuleConstraint.OnlyPredicates(fields("values").convertTo[JsArray].elements.map(_.convertTo[TripleItem.Uri]).toSet)
      case "WithoutPredicates" => RuleConstraint.WithoutPredicates(fields("values").convertTo[JsArray].elements.map(_.convertTo[TripleItem.Uri]).toSet)
      case x => deserializationError(s"Invalid rule constraint name: $x")
    }
  }

  implicit def rulesMiningReader(implicit debugger: Debugger): RootJsonReader[RulesMining] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    Function.chain[RulesMining](List(
      rm => fields.get("thresholds").collect {
        case JsArray(x) => x.map(_.convertTo[Threshold]).foldLeft(rm)(_ addThreshold _)
      }.getOrElse(rm),
      rm => fields.get("patterns").collect {
        case JsArray(x) => x.map(_.convertTo[RulePattern]).foldLeft(rm)(_ addPattern _)
      }.getOrElse(rm),
      rm => fields.get("constraints").collect {
        case JsArray(x) => x.map(_.convertTo[RuleConstraint]).foldLeft(rm)(_ addConstraint _)
      }.getOrElse(rm)
    ))(Amie())
  }

  implicit val measureKeyReader: RootJsonReader[TypedKeyMap.Key[Measure]] = (json: JsValue) => json.convertTo[String] match {
    case "HeadSize" => Measure.HeadSize
    case "Support" => Measure.Support
    case "HeadCoverage" => Measure.HeadCoverage
    case "BodySize" => Measure.BodySize
    case "Confidence" => Measure.Confidence
    case "PcaConfidence" => Measure.PcaConfidence
    case "PcaBodySize" => Measure.PcaBodySize
    case "HeadConfidence" => Measure.HeadConfidence
    case "Lift" => Measure.Lift
    case "Cluster" => Measure.Cluster
    case x => deserializationError(s"Invalid measure name: $x")
  }

  implicit val weightedSimilarityCountingReader: RootJsonReader[WeightedSimilarityCounting[Rule.Simple]] = (json: JsValue) => {
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

  implicit val similarityCountingReader: RootJsonReader[SimilarityCounting[Rule.Simple]] = (json: JsValue) => json.convertTo[JsArray].elements
    .map(_.convertTo[WeightedSimilarityCounting[Rule.Simple]])
    .foldLeft(Option.empty[Comb[Rule.Simple]]) {
      case (None, x) => Some(x)
      case (Some(x), y) => Some(x ~ y)
    }.getOrElse(implicitly[SimilarityCounting[Rule.Simple]])

  implicit def clusteringReader(implicit debugger: Debugger): RootJsonReader[Clustering[Rule.Simple]] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    DbScan(
      fields.get("minNeighbours").map(_.convertTo[Int]).getOrElse(5),
      fields.get("minSimilarity").map(_.convertTo[Double]).getOrElse(0.9)
    )(fields.get("features").map(_.convertTo[SimilarityCounting[Rule.Simple]]).getOrElse(implicitly[SimilarityCounting[Rule.Simple]]), debugger)
  }

  implicit val resolvedRuleAtomItemReader: RootJsonReader[ResolvedRule.Atom.Item] = {
    case JsObject(fields) => fields("type").convertTo[String] match {
      case "variable" => ResolvedRule.Atom.Item(fields("value").convertTo[String])
      case "constant" => ResolvedRule.Atom.Item(fields("value").convertTo[TripleItem])
    }
    case JsString(x) if x.startsWith("?") => ResolvedRule.Atom.Item(x)
    case x: JsValue => ResolvedRule.Atom.Item(x.convertTo[TripleItem])
  }

  implicit val resolvedRuleAtomReader: RootJsonReader[ResolvedRule.Atom] = (json: JsValue) => {
    val fields = json.asJsObject.fields
    ResolvedRule.Atom(
      fields("subject").convertTo[ResolvedRule.Atom.Item],
      fields("predicate").convertTo[TripleItem.Uri],
      fields("object").convertTo[ResolvedRule.Atom.Item]
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
      fields("body").convertTo[JsArray].elements.map(_.convertTo[ResolvedRule.Atom]),
      fields("head").convertTo[ResolvedRule.Atom]
    )(TypedKeyMap(fields.get("measures").toIterable.flatMap(_.convertTo[JsArray].elements).map(_.convertTo[Measure])))
  }

  implicit val rulesetSourceReader: RootJsonReader[RulesetSource] = (json: JsValue) => json.convertTo[String] match {
    case "txt" => RulesetSource.Text
    case "json" => RulesetSource.Json
    case x => deserializationError(s"Invalid ruleset format name: $x")
  }

}