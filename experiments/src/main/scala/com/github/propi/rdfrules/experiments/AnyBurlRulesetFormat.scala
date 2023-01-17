package com.github.propi.rdfrules.experiments

import com.github.propi.rdfrules.data.RdfSource.Tsv.ParsingMode
import com.github.propi.rdfrules.data.formats.Tsv
import com.github.propi.rdfrules.rule.{Measure, ResolvedAtom, ResolvedRule}

import scala.jdk.CollectionConverters.IteratorHasAsScala

object AnyBurlRulesetFormat {

  private val tripleParser = Tsv.tripleParser(ParsingMode.Raw)

  private def atomToResolvedAtom(atom: anyburl.Atom): ResolvedAtom = {
    ResolvedAtom(
      if (atom.isLeftC) ResolvedAtom.ResolvedItem(tripleParser.parseUri(atom.getLeft)) else ResolvedAtom.ResolvedItem(atom.getLeft.head.toLower),
      tripleParser.parseUri(atom.getRelation),
      if (atom.isRightC) ResolvedAtom.ResolvedItem(tripleParser.parseUri(atom.getRight)) else ResolvedAtom.ResolvedItem(atom.getRight.head.toLower)
    )
  }

  private def ruleToResolvedRule(rule: anyburl.Rule): ResolvedRule = {
    val body = (0 until rule.bodysize()).iterator.map(rule.getBodyAtom).map(atomToResolvedAtom).toVector
    val head = atomToResolvedAtom(rule.getHead)
    ResolvedRule(body, head, Measure.CwaConfidence(rule.getConfidence), Measure.Support(rule.getCorrectlyPredicted), Measure.BodySize(rule.getPredicted))
  }

  def readRules(path: String): Iterator[ResolvedRule] = {
    val rr = new anyburl.RuleReader
    val rules = rr.read(path)
    rules.iterator().asScala.map(ruleToResolvedRule)
  }

}