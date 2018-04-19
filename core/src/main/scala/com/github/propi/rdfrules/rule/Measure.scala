package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.utils.TypedKeyMap.{Key, Value}

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
sealed trait Measure extends Value {
  def companion: Key[Measure]
}

object Measure {

  case class Support(value: Int) extends Measure {
    def companion: Support.type = Support
  }

  implicit object Support extends Key[Support]

  case class HeadCoverage(value: Double) extends Measure {
    def companion: HeadCoverage.type = HeadCoverage
  }

  implicit object HeadCoverage extends Key[HeadCoverage]

  case class HeadSize(value: Int) extends Measure {
    def companion: HeadSize.type = HeadSize
  }

  implicit object HeadSize extends Key[HeadSize]

  case class BodySize(value: Int) extends Measure {
    def companion: BodySize.type = BodySize
  }

  implicit object BodySize extends Key[BodySize]

  case class Confidence(value: Double) extends Measure {
    def companion: Confidence.type = Confidence
  }

  implicit object Confidence extends Key[Confidence]

  case class HeadConfidence(value: Double) extends Measure {
    def companion: HeadConfidence.type = HeadConfidence
  }

  implicit object HeadConfidence extends Key[HeadConfidence]

  case class Lift(value: Double) extends Measure {
    def companion: Lift.type = Lift
  }

  implicit object Lift extends Key[Lift]

  case class PcaBodySize(value: Int) extends Measure {
    def companion: PcaBodySize.type = PcaBodySize
  }

  implicit object PcaBodySize extends Key[PcaBodySize]

  case class PcaConfidence(value: Double) extends Measure {
    def companion: PcaConfidence.type = PcaConfidence
  }

  implicit object PcaConfidence extends Key[PcaConfidence]

  case class PcaLift(value: Double) extends Measure {
    def companion: PcaLift.type = PcaLift
  }

  implicit object PcaLift extends Key[PcaLift]

  case class Cluster(number: Int) extends Measure {
    def companion: Cluster.type = Cluster
  }

  implicit object Cluster extends Key[Cluster]

  implicit def mesureToKeyValue(measure: Measure): (Key[Measure], Measure) = measure.companion -> measure

  implicit val measureKeyOrdering: Ordering[Key[Measure]] = {
    val map = Iterator[Key[Measure]](Support, HeadCoverage, Confidence, Lift, PcaConfidence, PcaLift, HeadConfidence, HeadSize, BodySize, PcaBodySize, Cluster).zipWithIndex.map(x => x._1 -> (x._2 + 1)).toMap
    Ordering.by[Key[Measure], Int](map.getOrElse(_, 0))
  }

  implicit val measureOrdering: Ordering[Measure] = Ordering.by[Measure, Double] {
    case Measure.BodySize(x) => x
    case Measure.Confidence(x) => x
    case Measure.HeadConfidence(x) => x
    case Measure.HeadCoverage(x) => x
    case Measure.HeadSize(x) => x
    case Measure.Lift(x) => x
    case Measure.PcaBodySize(x) => x
    case Measure.PcaConfidence(x) => x
    case Measure.PcaLift(x) => x
    case Measure.Support(x) => x
    case Measure.Cluster(x) => x * -1
  }.reverse

}
