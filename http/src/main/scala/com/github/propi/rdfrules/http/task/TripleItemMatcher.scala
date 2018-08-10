package com.github.propi.rdfrules.http.task

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.http.task.TripleItemMatcher.Number.ComparingType
import com.github.propi.rdfrules.utils.BasicExtractors.{AnyToBoolean, AnyToDouble}
import eu.easyminer.discretization.impl.IntervalBound

import scala.util.matching.Regex

/**
  * Created by Vaclav Zeman on 8. 8. 2018.
  */
sealed trait TripleItemMatcher[T <: TripleItem] {
  def matchAll(tripleItem: T): IndexedSeq[String]
}

object TripleItemMatcher {

  class Resource private(regexp: String) extends TripleItemMatcher[TripleItem.Uri] {
    def matchAll(tripleItem: TripleItem.Uri): IndexedSeq[String] = {
      val text = tripleItem.toString
      ("^" + regexp + "$").r.findFirstMatchIn(text).map(x => Resource.group0(tripleItem) :: x.subgroups).getOrElse(Nil).toIndexedSeq
    }
  }

  object Resource {
    def unapply(text: String): Option[Resource] = {
      if (text.matches("<.*>") || text.matches("_:.+") || text.matches("(?:[^\"].*)?:.+")) {
        Some(new Resource(text))
      } else {
        None
      }
    }

    def group0(tripleItem: TripleItem.Uri): String = tripleItem match {
      case TripleItem.LongUri(x) => x
      case _ => tripleItem.toString
    }
  }

  class Text private(regexp: String) extends TripleItemMatcher[TripleItem.Text] {
    def matchAll(tripleItem: TripleItem.Text): IndexedSeq[String] = {
      ("^" + regexp + "$").r.findFirstMatchIn(tripleItem.toString).map(x => Text.group0(tripleItem) :: x.subgroups).getOrElse(Nil).toIndexedSeq
    }
  }

  object Text {
    def unapply(text: String): Option[Text] = {
      if (text.matches("\".*\"")) {
        Some(new Text(text))
      } else {
        None
      }
    }

    def group0(tripleItem: TripleItem.Text): String = tripleItem.value
  }

  class Number private(value1: Double,
                       comparingType1: ComparingType.ComparingType,
                       value2: Option[Double] = None,
                       comparingType2: Option[ComparingType.ComparingType] = None) extends TripleItemMatcher[TripleItem.Number[_]] {
    def matchAll(tripleItem: TripleItem.Number[_]): IndexedSeq[String] = tripleItem match {
      case TripleItem.NumberDouble(x) =>
        val conds: List[(Double, ComparingType.ComparingType) => Boolean] = List(
          (v, ct) => ct == ComparingType.ET && x == v,
          (v, ct) => ct == ComparingType.GT && x > v,
          (v, ct) => ct == ComparingType.GTET && x >= v,
          (v, ct) => ct == ComparingType.LT && x < v,
          (v, ct) => ct == ComparingType.LTET && x <= v
        )
        if (conds.exists(f => f(value1, comparingType1)) && value2.zip(comparingType2).forall(x => conds.exists(f => f(x._1, x._2)))) {
          IndexedSeq(x.toString)
        } else {
          IndexedSeq.empty
        }
      case _ => IndexedSeq.empty
    }
  }

  object Number {

    val IntervalPattern: Regex = "([\\[\\(])\\s*(.+?)\\s*;\\s*(.+?)\\s*([\\]\\)])".r

    object ComparingType extends Enumeration {
      type ComparingType = Value
      val ET, GT, GTET, LT, LTET = Value

      def unapply(arg: String): Option[ComparingType] = arg match {
        case "[" => Some(GTET)
        case "(" => Some(GT)
        case "]" => Some(LTET)
        case ")" => Some(LT)
        case _ => None
      }
    }

    def unapply(text: String): Option[Number] = {
      val GtPattern = ">\\s*(.+)".r
      val GtEtPattern = ">=\\s*(.+)".r
      val LtPattern = "<\\s*(.+)".r
      val LtEtPattern = "<=\\s*(.+)".r
      text match {
        case AnyToDouble(x) => Some(new Number(x, ComparingType.ET))
        case GtPattern(AnyToDouble(x)) => Some(new Number(x, ComparingType.GT))
        case GtEtPattern(AnyToDouble(x)) => Some(new Number(x, ComparingType.GTET))
        case LtPattern(AnyToDouble(x)) => Some(new Number(x, ComparingType.LT))
        case LtEtPattern(AnyToDouble(x)) => Some(new Number(x, ComparingType.LTET))
        case IntervalPattern(ComparingType(ct1), AnyToDouble(x1), AnyToDouble(x2), ComparingType(ct2)) => Some(new Number(x1, ct1, Some(x2), Some(ct2)))
        case _ => None
      }
    }

    def group0(tripleItem: TripleItem.Number[_]): String = TripleItem.NumberDouble.unapply(tripleItem).getOrElse(0).toString
  }

  class BooleanValue private(value: Boolean) extends TripleItemMatcher[TripleItem.BooleanValue] {
    def matchAll(tripleItem: TripleItem.BooleanValue): IndexedSeq[String] = if (tripleItem.value == value) {
      IndexedSeq(BooleanValue.group0(tripleItem))
    } else {
      IndexedSeq.empty
    }
  }

  object BooleanValue {
    def unapply(text: String): Option[BooleanValue] = AnyToBoolean.unapply(text).map(new BooleanValue(_))

    def group0(tripleItem: TripleItem.BooleanValue): String = tripleItem.value.toString
  }

  class Interval private(b1: IntervalBound, b2: IntervalBound) extends TripleItemMatcher[TripleItem.Interval] {
    def matchAll(tripleItem: TripleItem.Interval): IndexedSeq[String] = {
      if (b1 == tripleItem.interval.minValue && b2 == tripleItem.interval.maxValue) {
        IndexedSeq(Interval.group0(tripleItem), tripleItem.interval.minValue.value.toString, tripleItem.interval.maxValue.value.toString)
      } else {
        IndexedSeq()
      }
    }
  }

  object Interval {
    def unapply(text: String): Option[Interval] = {
      if (text.startsWith("i")) {
        text.substring(1) match {
          case Number.IntervalPattern(ComparingType(ct1), AnyToDouble(x1), AnyToDouble(x2), ComparingType(ct2)) =>
            val b1 = ct1 match {
              case ComparingType.GT => Some(IntervalBound.Exclusive(x1))
              case ComparingType.GTET => Some(IntervalBound.Inclusive(x1))
              case _ => None
            }
            val b2 = ct2 match {
              case ComparingType.LT => Some(IntervalBound.Exclusive(x2))
              case ComparingType.LTET => Some(IntervalBound.Inclusive(x2))
              case _ => None
            }
            b1.zip(b2).map(x => new Interval(x._1, x._2)).headOption
          case _ => None
        }
      } else {
        None
      }
    }

    def group0(tripleItem: TripleItem.Interval): String = ""
  }

}
