package com.github.propi.rdfrules.http.task

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.github.propi.rdfrules.data.Properties.PropertyStats
import com.github.propi.rdfrules.data.Quad.QuadTraversableView
import com.github.propi.rdfrules.data.{Histogram, Prefix, TripleItem}
import com.github.propi.rdfrules.http.formats.CommonDataJsonFormats._
import com.github.propi.rdfrules.http.formats.CommonDataJsonWriters._
import com.github.propi.rdfrules.http.util.TraversablePublisher._
import com.github.propi.rdfrules.index.PropertyCardinalities
import com.github.propi.rdfrules.prediction.EvaluationResult
import com.github.propi.rdfrules.rule.{ResolvedInstantiatedRule, ResolvedRule}
import com.github.propi.rdfrules.utils.ForEach
import spray.json.DefaultJsonProtocol._
import spray.json._

/**
  * Created by Vaclav Zeman on 14. 8. 2018.
  */
sealed trait ToJsonTask[T] extends Task[T, Source[JsValue, NotUsed]] {
  val companion: TaskDefinition = ToJsonTask
}

object ToJsonTask extends TaskDefinition {
  val name: String = "ToJson"

  class From[T] extends ToJsonTask[T] {
    def execute(input: T): Source[JsValue, NotUsed] = Source.single(JsNull)
  }

  object FromUnit extends ToJsonTask[Unit] {
    def execute(input: Unit): Source[JsValue, NotUsed] = Source.single(JsNull)
  }

  object FromInt extends ToJsonTask[Int] {
    def execute(input: Int): Source[JsValue, NotUsed] = Source.single(JsNumber(input))
  }

  object FromEvaluationResult extends ToJsonTask[EvaluationResult] {
    def execute(input: EvaluationResult): Source[JsValue, NotUsed] = Source.single(input.toJson)
  }

  object FromQuads extends ToJsonTask[QuadTraversableView] {
    def execute(input: QuadTraversableView): Source[JsValue, NotUsed] = Source.fromPublisher(input.map(_.toJson))
  }

  object FromPrefixes extends ToJsonTask[ForEach[Prefix]] {
    def execute(input: ForEach[Prefix]): Source[JsValue, NotUsed] = Source.fromPublisher(input.map(_.toJson))
  }

  object FromRules extends ToJsonTask[Seq[ResolvedRule]] {
    def execute(input: Seq[ResolvedRule]): Source[JsValue, NotUsed] = Source.fromPublisher(input.view.map(_.toJson))
  }

  object FromPropertiesCardinalities extends ToJsonTask[Seq[PropertyCardinalities.Resolved]] {
    def execute(input: Seq[PropertyCardinalities.Resolved]): Source[JsValue, NotUsed] = Source.fromIterator(() => input.iterator.map(_.toJson))
  }

  object FromTypes extends ToJsonTask[Seq[(TripleItem.Uri, PropertyStats)]] {
    def execute(input: Seq[(TripleItem.Uri, PropertyStats)]): Source[JsValue, NotUsed] = Source.fromIterator(() => input.iterator.map(x => JsObject("predicate" -> x._1.toJson, "types" -> x._2.toJson)).map(_.toJson))
  }

  object FromHistogram extends ToJsonTask[Seq[(Histogram.Key, Int)]] {
    def execute(input: Seq[(Histogram.Key, Int)]): Source[JsValue, NotUsed] = Source.fromIterator(() => input.iterator.map(_.toJson))
  }

  object FromGroupedPredictedTriple extends ToJsonTask[Seq[GroupedPredictedTriple]] {
    def execute(input: Seq[GroupedPredictedTriple]): Source[JsValue, NotUsed] = Source.fromIterator(() => input.iterator.map(_.toJson))
  }

  object FromInstantiatedRules extends ToJsonTask[Seq[ResolvedInstantiatedRule]] {
    def execute(input: Seq[ResolvedInstantiatedRule]): Source[JsValue, NotUsed] = Source.fromIterator(() => input.iterator.map(_.toJson))
  }



}