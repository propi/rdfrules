package eu.easyminer.rdf.rule

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
sealed trait Measure {
  def companion: Measure.Key[Measure]
}

object Measure {

  case class Measures(m: collection.mutable.Map[Key[Measure], Measure]) {
    def apply[A <: Measure](implicit key: Key[A]): A = m(key).asInstanceOf[A]

    def get[A <: Measure](implicit key: Key[A]): Option[A] = m.get(key).map(_.asInstanceOf[A])

    def +=(measures: (Key[Measure], Measure)*): Measures = {
      m ++= measures
      this
    }
  }

  object Measures {
    def apply(measures: (Key[Measure], Measure)*): Measures = Measures(collection.mutable.Map(measures: _*))
  }

  sealed trait Key[+T <: Measure]

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

  object HeadConfidence extends Key[HeadConfidence]

  case class Lift(value: Double) extends Measure {
    def companion: Lift.type = Lift
  }

  object Lift extends Key[Lift]

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

  object PcaLift extends Key[PcaLift]

  implicit def mesureToKeyValue(measure: Measure): (Key[Measure], Measure) = measure.companion -> measure

}
