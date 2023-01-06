package com.github.propi.rdfrules.experiments

import amie.data.KB
import amie.rules.Rule
import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.rule.{Measure, ResolvedAtom, ResolvedRule}

import scala.jdk.CollectionConverters._
import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 8. 5. 2019.
  */
object AmieRuleOps {

  implicit def amieAtomItemToItem(x: String): ResolvedAtom.ResolvedItem = {
    val UriString = "<(.+)>".r
    val TextString = "\"(.+)\"".r
    val VarString = "([?].)".r
    x match {
      case UriString(x) => ResolvedAtom.ResolvedItem(TripleItem.LongUri(x))
      case TextString(x) => ResolvedAtom.ResolvedItem(TripleItem.Text(x))
      case VarString(x) => ResolvedAtom.ResolvedItem(x)
      case x => ResolvedAtom.ResolvedItem(TripleItem.LongUri(x))
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
      val body = amieRule.getBody.asScala.map(x => ResolvedAtom(KB.unmap(x(0)), KB.unmap(x(1)), KB.unmap(x(2)))).toIndexedSeq
      val head = {
        val x = amieRule.getHead
        ResolvedAtom(KB.unmap(x(0)), KB.unmap(x(1)), KB.unmap(x(2)))
      }
      val measures = List(
        Measure.Support(amieRule.getSupport.toInt),
        Measure.HeadCoverage(amieRule.getHeadCoverage),
        Measure.BodySize(amieRule.getBodySize.toInt),
        Measure.PcaBodySize(amieRule.getPcaBodySize.toInt),
        Measure.CwaConfidence(amieRule.getStdConfidence),
        Measure.PcaConfidence(amieRule.getPcaConfidence)
      )
      ResolvedRule(body, head, measures: _*)
    }
  }

}
