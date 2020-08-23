package com.github.propi.rdfrules.http.task

import com.github.propi.rdfrules.data.{Prefix, TripleItem}
import com.github.propi.rdfrules.http.task.TripleItemMatcher.Number.ComparingType
import com.github.propi.rdfrules.http.util.ArithmeticEval
import com.github.propi.rdfrules.utils.BasicExtractors.{AnyToBoolean, AnyToDouble}
import eu.easyminer.discretization.impl.IntervalBound

/**
  * Created by Vaclav Zeman on 8. 8. 2018.
  */
object TripleItemMapper {

  object Resource {
    def unapply(arg: String): Option[TripleItem.Uri] = {
      val LongUriPattern = "<(.*)>".r
      val BlankNodePattern = "_:(.+)".r
      val PrefixedUriPattern = "(\\w*):(.*)".r
      arg match {
        case LongUriPattern(x) => Some(TripleItem.LongUri(x))
        case BlankNodePattern(x) => Some(TripleItem.BlankNode(x))
        case PrefixedUriPattern(prefix, localName) => Some(TripleItem.PrefixedUri(Prefix(prefix, ""), localName))
        case _ => None
      }
    }
  }

  object Text {
    def unapply(arg: String): Option[TripleItem.Text] = {
      val TextPattern = "\"(.*)\"".r
      arg match {
        case TextPattern(x) => Some(TripleItem.Text(x))
        case _ => None
      }
    }
  }

  object Number {
    def unapply(arg: String): Option[TripleItem.Number[_]] = ArithmeticEval(arg).map(TripleItem.Number(_))
  }

  object BooleanValue {
    def unapply(arg: String): Option[TripleItem.BooleanValue] = {
      arg match {
        case AnyToBoolean(x) => Some(TripleItem.BooleanValue(x))
        case _ => None
      }
    }
  }

  object Interval {
    def unapply(arg: String): Option[TripleItem.Interval] = {
      arg match {
        case TripleItemMatcher.Number.IntervalPattern(ComparingType(ct1), AnyToDouble(x1), AnyToDouble(x2), ComparingType(ct2)) =>
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
          b1.zip(b2).map(x => TripleItem.Interval(eu.easyminer.discretization.impl.Interval(x._1, x._2))).headOption
        case _ => None
      }
    }
  }

}
