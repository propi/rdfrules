package eu.easyminer.rdf.rule

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
sealed trait Threshold {
  def companion: Threshold.Key
}

object Threshold {

  type Thresholds = collection.mutable.Map[Key, Threshold]

  sealed trait Key

  case class MinSupport(value: Int) extends Threshold {
    def companion: Key = MinSupport
  }

  object MinSupport extends Key

  case class MinHeadCoverage(value: Double) extends Threshold {
    def companion: Key = MinHeadCoverage
  }

  object MinHeadCoverage extends Key

  case class MaxRuleLength(value: Int) extends Threshold {
    def companion: Key = MaxRuleLength
  }

  object MaxRuleLength extends Key

  case class MinConfidence(value: Double) extends Threshold {
    def companion: Key = MinConfidence
  }

  object MinConfidence extends Key

  implicit def thresholdToKeyValue(threshold: Threshold): (Key, Threshold) = threshold.companion -> threshold

}
