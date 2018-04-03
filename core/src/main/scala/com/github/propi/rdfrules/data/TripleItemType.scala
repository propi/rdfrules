package com.github.propi.rdfrules.data

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 2. 2. 2018.
  */
sealed trait TripleItemType

object TripleItemType {

  case object Resource extends TripleItemType

  case object Number extends TripleItemType

  case object Boolean extends TripleItemType

  case object Text extends TripleItemType

  case object Interval extends TripleItemType

  implicit def apply(tripleItem: TripleItem): TripleItemType = tripleItem match {
    case _: TripleItem.Uri => Resource
    case _: TripleItem.Text => Text
    case _: TripleItem.BooleanValue => Boolean
    case _: TripleItem.Number[_] => Number
    case _: TripleItem.Interval => Interval
  }

}