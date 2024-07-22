package com.github.propi.rdfrules.index

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.rule.Threshold.{MaxRuleLength, MinHeadCoverage, MinHeadSize}

trait AutoDiscretizationTask {
  def minSupportLowerBoundOn: Boolean

  def minSupportUpperBoundOn: Boolean

  def minHeadSize: MinHeadSize

  def minHeadCoverage: MinHeadCoverage

  def maxRuleLength: MaxRuleLength

  def predicates: Iterator[TripleItem.Uri]
}

object AutoDiscretizationTask {

  case class Common(minSupportLowerBoundOn: Boolean = true,
                    minSupportUpperBoundOn: Boolean = true,
                    minHeadSize: MinHeadSize = MinHeadSize(100),
                    minHeadCoverage: MinHeadCoverage = MinHeadCoverage(0.01),
                    maxRuleLength: MaxRuleLength = MaxRuleLength(3),
                    _predicates: Iterable[TripleItem.Uri] = Nil) extends AutoDiscretizationTask {
    def predicates: Iterator[TripleItem.Uri] = _predicates.iterator
  }

}
