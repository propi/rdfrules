package com.github.propi.rdfrules.http.task

import com.github.propi.rdfrules.data.{Quad, Triple}
import com.github.propi.rdfrules.http.task.QuadMatcher.CapturedQuadItems
import com.github.propi.rdfrules.utils.BasicExtractors.AnyToInt

import scala.util.matching.Regex

/**
  * Created by Vaclav Zeman on 8. 8. 2018.
  */
class QuadMapper(s: Option[String], p: Option[String], o: Option[String], g: Option[String]) {

  def map(quad: Quad, capturedQuadItems: CapturedQuadItems): Quad = {
    val GroupPattern = "\\$([spog]?)(\\d+)".r

    def replace(item: IndexedSeq[String])(m: Regex.Match): String = {
      val desiredItem = m.group(1) match {
        case null => item
        case "s" => capturedQuadItems.s
        case "p" => capturedQuadItems.p
        case "o" => capturedQuadItems.o
        case "g" => capturedQuadItems.g
        case _ => IndexedSeq.empty
      }
      AnyToInt.unapply(m.group(2)).filter(desiredItem.isDefinedAt).map(desiredItem.apply).getOrElse(m.toString())
    }

    def toTripleItemUri(x: String) = TripleItemMapper.Resource.unapply(x)

    def toTripleItem(x: String) = toTripleItemUri(x)
      .orElse(TripleItemMapper.Text.unapply(x))
      .orElse(TripleItemMapper.Number.unapply(x))
      .orElse(TripleItemMapper.BooleanValue.unapply(x))
      .orElse(TripleItemMapper.Interval.unapply(x))

    Quad(
      Triple(
        s.flatMap(x => toTripleItemUri(GroupPattern.replaceAllIn(x, replace(capturedQuadItems.s) _))).getOrElse(quad.triple.subject),
        p.flatMap(x => toTripleItemUri(GroupPattern.replaceAllIn(x, replace(capturedQuadItems.p) _))).getOrElse(quad.triple.predicate),
        o.flatMap(x => toTripleItem(GroupPattern.replaceAllIn(x, replace(capturedQuadItems.o) _))).getOrElse(quad.triple.`object`)
      ),
      g.flatMap(x => toTripleItemUri(GroupPattern.replaceAllIn(x, replace(capturedQuadItems.g) _))).getOrElse(quad.graph)
    )
  }

}