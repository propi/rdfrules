package com.github.propi.rdfrules.experiments

import amie.rules.Rule
import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.rule.{Measure, ResolvedRule}

import scala.collection.JavaConverters._
import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 8. 5. 2019.
  */
object AmieRuleOps {

  implicit def amieAtomItemToItem(x: String): ResolvedRule.Atom.Item = {
    val UriString = "<(.+)>".r
    val TextString = "\"(.+)\"".r
    val VarString = "([?].)".r
    x match {
      case UriString(x) => ResolvedRule.Atom.Item(TripleItem.LongUri(x))
      case TextString(x) => ResolvedRule.Atom.Item(TripleItem.Text(x))
      case VarString(x) => ResolvedRule.Atom.Item(x)
      case x => ResolvedRule.Atom.Item(TripleItem.LongUri(x))
    }
  }

  implicit def amieAtomItemToUri(x: String): TripleItem.Uri = {
    val UriString = "<(.+)>".r
    x match {
      case UriString(x) => TripleItem.LongUri(x)
      case x => TripleItem.LongUri(x)
    }
  }

  implicit class PimpedAmieRule(amieRule: Rule) {
    def toResolvedRule: ResolvedRule = {
      val body = amieRule.getBody.asScala.map(x => ResolvedRule.Atom(x(0).toString, x(1).toString, x(2).toString)).toIndexedSeq
      val head = {
        val x = amieRule.getHead
        ResolvedRule.Atom(x(0).toString, x(1).toString, x(2).toString)
      }
      val measures = List(
        Measure.Support(amieRule.getSupport.toInt),
        Measure.HeadCoverage(amieRule.getHeadCoverage),
        Measure.BodySize(amieRule.getBodySize.toInt),
        Measure.PcaBodySize(amieRule.getPcaBodySize.toInt),
        Measure.Confidence(amieRule.getStdConfidence),
        Measure.PcaConfidence(amieRule.getPcaConfidence)
      )
      ResolvedRule(body, head, measures: _*)
    }
  }

}
