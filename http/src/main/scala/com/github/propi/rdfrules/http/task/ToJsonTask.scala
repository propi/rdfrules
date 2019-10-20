package com.github.propi.rdfrules.http.task

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.github.propi.rdfrules.data.{Histogram, Prefix, Quad, TripleItem, TripleItemType}
import com.github.propi.rdfrules.http.formats.CommonDataJsonFormats._
import com.github.propi.rdfrules.http.formats.CommonDataJsonWriters._
import com.github.propi.rdfrules.http.util.TraversablePublisher._
import com.github.propi.rdfrules.model.EvaluationResult
import com.github.propi.rdfrules.ruleset.ResolvedRule
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

  object FromQuads extends ToJsonTask[Traversable[Quad]] {
    def execute(input: Traversable[Quad]): Source[JsValue, NotUsed] = Source.fromPublisher(input.view.map(_.toJson))
  }

  object FromPrefixes extends ToJsonTask[Traversable[Prefix]] {
    def execute(input: Traversable[Prefix]): Source[JsValue, NotUsed] = Source.fromPublisher(input.view.map(_.toJson))
  }

  object FromRules extends ToJsonTask[Traversable[ResolvedRule]] {
    def execute(input: Traversable[ResolvedRule]): Source[JsValue, NotUsed] = Source.fromPublisher(input.view.map(_.toJson))
  }

  object FromTypes extends ToJsonTask[Seq[(TripleItem.Uri, collection.Map[TripleItemType, Int])]] {
    def execute(input: Seq[(TripleItem.Uri, collection.Map[TripleItemType, Int])]): Source[JsValue, NotUsed] = Source.fromIterator(() => input.iterator.map(_.toJson))
  }

  object FromHistogram extends ToJsonTask[Seq[(Histogram.Key, Int)]] {
    def execute(input: Seq[(Histogram.Key, Int)]): Source[JsValue, NotUsed] = Source.fromIterator(() => input.iterator.map(_.toJson))
  }

}