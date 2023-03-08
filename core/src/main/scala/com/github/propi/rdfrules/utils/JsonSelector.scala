package com.github.propi.rdfrules.utils

import spray.json.DefaultJsonProtocol.RootJsArrayFormat
import spray.json.{JsArray, JsBoolean, JsNull, JsNumber, JsObject, JsString, JsValue, JsonReader, deserializationError}

import scala.util.Try

class JsonSelector private(val jsValue: Option[JsValue]) {

  def get(key: String): JsonSelector = new JsonSelector(jsValue.collect {
    case JsObject(fields) => fields.get(key)
  }.flatten)

  def get(key: Int): JsonSelector = new JsonSelector(jsValue.collect {
    case JsArray(elements) => elements.lift(key)
  }.flatten)

  def apply(key: String): JsonSelector = new JsonSelector(jsValue.map(_.asJsObject.fields.getOrElse(key, deserializationError(s"Missing key in JSON object: $key"))))

  def apply(key: Int): JsonSelector = new JsonSelector(jsValue.map(_.convertTo[JsArray].elements.lift(key).getOrElse(deserializationError(s"Missing index in JSON array: $key"))))

  def isEmpty: Boolean = jsValue.forall {
    case JsObject(fields) => fields.isEmpty
    case JsArray(elements) => elements.isEmpty
    case JsNull => true
    case _ => false
  }

  def nonEmpty: Boolean = !isEmpty

  def isZero: Boolean = isEmpty || jsValue.forall {
    case JsString(x) => x == "0" || x.isEmpty
    case JsNumber(x) => x == 0
    case JsBoolean(x) => !x
    case _ => false
  }

  def to[T](implicit reader: JsonReader[T]): T = jsValue.getOrElse(deserializationError("Empty JSON value could not be parsed.")).convertTo[T]

  def toOpt[T](implicit reader: JsonReader[T]): Option[T] = jsValue.flatMap(x => Try(x.convertTo[T]).toOption)

  def toIterable: Iterable[JsonSelector] = new Iterable[JsonSelector] {
    def iterator: Iterator[JsonSelector] = jsValue.iterator.flatMap {
      case JsObject(fields) => fields.valuesIterator
      case JsArray(elements) => elements.iterator
      case x => Iterator(x)
    }.map(x => new JsonSelector(Some(x)))
  }

  def toTypedIterable[T](implicit reader: JsonReader[T]): Iterable[T] = toIterable.view.map(_.to[T])

  def toOptTypedIterable[T](implicit reader: JsonReader[T]): Iterable[T] = toIterable.view.flatMap(_.toOpt[T])
}

object JsonSelector {

  implicit class PimpedOptJsValue(jsValue: Option[JsValue]) {
    def toSelector: JsonSelector = new JsonSelector(jsValue)
  }

  implicit class PimpedJsValue(jsValue: JsValue) extends PimpedOptJsValue(Some(jsValue))

}