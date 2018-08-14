package com.github.propi.rdfrules.http.task

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.github.propi.rdfrules.data.{Histogram, Prefix, Quad, TripleItem, TripleItemType}
import com.github.propi.rdfrules.http.util.TraversablePublisher._
import com.github.propi.rdfrules.ruleset.ResolvedRule
import spray.json._

import scala.reflect.ClassTag

/**
  * Created by Vaclav Zeman on 14. 8. 2018.
  */
class ToJsonTask extends Task[Any, Source[JsValue, NotUsed]] {
  val companion: TaskDefinition = ToJsonTask

  def execute(input: Any): Source[JsValue, NotUsed] = {
    val `Traversable[Quad]` = implicitly[ClassTag[Traversable[Quad]]]
    val `Traversable[Prefix]` = implicitly[ClassTag[Traversable[Prefix]]]
    val `Traversable[ResolvedRule]` = implicitly[ClassTag[Traversable[ResolvedRule]]]
    val `Map[Uri, Map[TripleItemType, Int]]` = implicitly[ClassTag[collection.Map[TripleItem.Uri, collection.Map[TripleItemType, Int]]]]
    val `Map[Key, Int]` = implicitly[ClassTag[collection.Map[Histogram.Key, Int]]]
    input match {
      case x: Int => Source.single(JsNumber(x))
      case `Traversable[Quad]`(quads) => Source.fromPublisher(quads.view.map(_.toJson))
      case `Traversable[Prefix]`(prefixes) => Source.fromPublisher(prefixes.view.map(_.toJson))
      case `Map[Uri, Map[TripleItemType, Int]]`(types) => Source.fromIterator(() => types.iterator.map(_.toJson))
      case `Map[Key, Int]`(histogram) => Source.fromIterator(() => histogram.iterator.map(_.toJson))
      case `Traversable[ResolvedRule]`(rules) => Source.fromPublisher(rules.view.map(_.toJson))
      case _ => Source.single(JsNull)
    }
  }
}

object ToJsonTask extends TaskDefinition {
  val name: String = "ToJson"
}