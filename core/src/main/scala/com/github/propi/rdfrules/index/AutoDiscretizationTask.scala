package com.github.propi.rdfrules.index

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.rule.Threshold.{MinHeadCoverage, MinHeadSize}

trait AutoDiscretizationTask {
  def minSupportLowerBoundOn: Boolean

  def minSupportUpperBoundOn: Boolean

  def minHeadSize: MinHeadSize

  def minHeadCoverage: MinHeadCoverage

  def maxRuleLength: Int

  def predicates: Iterator[TripleItem.Uri]
}
