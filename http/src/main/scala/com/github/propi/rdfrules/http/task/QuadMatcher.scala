package com.github.propi.rdfrules.http.task

import com.github.propi.rdfrules.data.{Quad, TripleItem}
import com.github.propi.rdfrules.http.task.QuadMatcher.CapturedQuadItems

/**
  * Created by Vaclav Zeman on 8. 8. 2018.
  */
class QuadMatcher private(s: Option[TripleItemMatcher.Resource],
                          p: Option[TripleItemMatcher.Resource],
                          o: Option[TripleItemMatcher[_]],
                          g: Option[TripleItemMatcher.Resource]) {

  def matchAll(quad: Quad): CapturedQuadItems = new CapturedQuadItems(
    s.map(_.matchAll(quad.triple.subject)).getOrElse(IndexedSeq(TripleItemMatcher.Resource.group0(quad.triple.subject))),
    p.map(_.matchAll(quad.triple.predicate)).getOrElse(IndexedSeq(TripleItemMatcher.Resource.group0(quad.triple.predicate))),
    quad.triple.`object` match {
      case x: TripleItem.Uri => o.map {
        case m: TripleItemMatcher.Resource => m.matchAll(x)
        case _ => IndexedSeq.empty
      }.getOrElse(IndexedSeq(TripleItemMatcher.Resource.group0(x)))
      case x: TripleItem.Text => o.map {
        case m: TripleItemMatcher.Text => m.matchAll(x)
        case _ => IndexedSeq.empty
      }.getOrElse(IndexedSeq(TripleItemMatcher.Text.group0(x)))
      case x: TripleItem.Number[_] => o.map {
        case m: TripleItemMatcher.Number => m.matchAll(x)
        case _ => IndexedSeq.empty
      }.getOrElse(IndexedSeq(TripleItemMatcher.Number.group0(x)))
      case x: TripleItem.BooleanValue => o.map {
        case m: TripleItemMatcher.BooleanValue => m.matchAll(x)
        case _ => IndexedSeq.empty
      }.getOrElse(IndexedSeq(TripleItemMatcher.BooleanValue.group0(x)))
      case x: TripleItem.Interval => o.map {
        case m: TripleItemMatcher.Interval => m.matchAll(x)
        case _ => IndexedSeq.empty
      }.getOrElse(IndexedSeq(TripleItemMatcher.Interval.group0(x)))
    },
    g.map(_.matchAll(quad.graph)).getOrElse(IndexedSeq(TripleItemMatcher.Resource.group0(quad.graph)))
  )

}

object QuadMatcher {

  class CapturedQuadItems(val s: IndexedSeq[String],
                          val p: IndexedSeq[String],
                          val o: IndexedSeq[String],
                          val g: IndexedSeq[String]) {
    def matched: Boolean = List(s, p, o, g).forall(_.nonEmpty)
  }

  def apply(s: Option[String],
            p: Option[String],
            o: Option[String],
            g: Option[String]): QuadMatcher = new QuadMatcher(
    s.collect { case TripleItemMatcher.Resource(x) => x },
    p.collect { case TripleItemMatcher.Resource(x) => x },
    o.collect {
      case TripleItemMatcher.Resource(x) => x
      case TripleItemMatcher.Text(x) => x
      case TripleItemMatcher.Number(x) => x
      case TripleItemMatcher.BooleanValue(x) => x
      case TripleItemMatcher.Interval(x) => x
    },
    g.collect { case TripleItemMatcher.Resource(x) => x }
  )

}