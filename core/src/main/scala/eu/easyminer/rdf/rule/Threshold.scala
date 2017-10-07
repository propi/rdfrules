package eu.easyminer.rdf.rule

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
sealed trait Threshold {
  def companion: Threshold.Key[Threshold]
}

object Threshold {

  case class Thresholds(m: collection.mutable.Map[Key[Threshold], Threshold]) {
    def apply[A <: Threshold](implicit key: Key[A]): A = m(key).asInstanceOf[A]

    def get[A <: Threshold](implicit key: Key[A]): Option[A] = m.get(key).map(_.asInstanceOf[A])

    def +=(thresholds: (Key[Threshold], Threshold)*): Thresholds = {
      m ++= thresholds
      this
    }
  }

  object Thresholds {
    def apply(thresholds: (Key[Threshold], Threshold)*): Thresholds = Thresholds(collection.mutable.Map(thresholds: _*))
  }

  sealed trait Key[+T <: Threshold]

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

  case class MinConfidence(value: Double) extends Threshold {
    def companion: MinConfidence.type = MinConfidence
  }

  implicit object MinConfidence extends Key[MinConfidence]

  case class MinPcaConfidence(value: Double) extends Threshold {
    def companion: MinPcaConfidence.type = MinPcaConfidence
  }

  implicit object MinPcaConfidence extends Key[MinPcaConfidence]

  implicit def thresholdToKeyValue(threshold: Threshold): (Key[Threshold], Threshold) = threshold.companion -> threshold

}
