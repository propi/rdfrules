package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.utils.TypedKeyMap.{Key, Value}

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
sealed trait Threshold extends Value {
  def companion: Key[Threshold]
}

object Threshold {

  case class MinHeadSize(value: Int) extends Threshold {
    def companion: MinHeadSize.type = MinHeadSize
  }

  implicit object MinHeadSize extends Key[MinHeadSize]

  case class MinHeadCoverage(value: Double) extends Threshold {
    def companion: MinHeadCoverage.type = MinHeadCoverage
  }

  implicit object MinHeadCoverage extends Key[MinHeadCoverage]

  case class MaxRuleLength(value: Int) extends Threshold {
    def companion: MaxRuleLength.type = MaxRuleLength
  }

  implicit object MaxRuleLength extends Key[MaxRuleLength]

  case class MinConfidence(value: Double) extends Threshold {
    def companion: MinConfidence.type = MinConfidence
  }

  implicit object MinConfidence extends Key[MinConfidence]

  case class MinPcaConfidence(value: Double) extends Threshold {
    def companion: MinPcaConfidence.type = MinPcaConfidence
  }

  implicit object MinPcaConfidence extends Key[MinPcaConfidence]

  case class TopK(value: Int) extends Threshold {
    def companion: TopK.type = TopK
  }

  implicit object TopK extends Key[TopK]

  implicit def thresholdToKeyValue(threshold: Threshold): (Key[Threshold], Threshold) = threshold.companion -> threshold

}
