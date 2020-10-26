package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.utils.TypedKeyMap.{Key, Value}

import scala.concurrent.duration._
import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
sealed trait Threshold extends Value {
  def companion: Key[Threshold]
}

object Threshold {

  case class MinAtomSize(value: Int) extends Threshold {
    def companion: MinAtomSize.type = MinAtomSize
  }

  implicit object MinAtomSize extends Key[MinAtomSize]

  case class MinHeadSize(value: Int) extends Threshold {
    def companion: MinHeadSize.type = MinHeadSize
  }

  implicit object MinHeadSize extends Key[MinHeadSize]

  case class MinSupport(value: Int) extends Threshold {
    def companion: MinSupport.type = MinSupport
  }

  implicit object MinSupport extends Key[MinSupport]

  case class MinHeadCoverage(value: Double) extends Threshold {
    def companion: MinHeadCoverage.type = MinHeadCoverage
  }

  implicit object MinHeadCoverage extends Key[MinHeadCoverage]

  case class MaxRuleLength(value: Int) extends Threshold {
    def companion: MaxRuleLength.type = MaxRuleLength
  }

  implicit object MaxRuleLength extends Key[MaxRuleLength]

  /*case class TopK(value: Int) extends Threshold {
    def companion: TopK.type = TopK
  }

  implicit object TopK extends Key[TopK]*/

  case class Timeout(value: Int) extends Threshold {
    lazy val duration: FiniteDuration = Duration(value, MINUTES)

    def companion: Timeout.type = Timeout
  }

  implicit object Timeout extends Key[Timeout]

  implicit def thresholdToKeyValue(threshold: Threshold): (Key[Threshold], Threshold) = threshold.companion -> threshold

  def validate(threshold: Threshold): Threshold = threshold match {
    case MinHeadSize(x) if x < 1 => MinHeadSize(1)
    case MaxRuleLength(x) if x < 2 => MaxRuleLength(2)
    case MinSupport(x) if x < 1 => MinSupport(1)
    case MinHeadCoverage(x) if x < 0.001 => MinHeadCoverage(0.001)
    case MinHeadCoverage(x) if x > 1 => MinHeadCoverage(1)
    //case TopK(x) if x < 1 => TopK(1)
    case Timeout(x) if x < 1 => Timeout(1)
    case x => x
  }

}
