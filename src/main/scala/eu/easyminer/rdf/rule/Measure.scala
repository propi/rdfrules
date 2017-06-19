package eu.easyminer.rdf.rule

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
sealed trait Measure {
  def companion: Measure.Key
}

object Measure {

  type Measures = collection.mutable.Map[Key, Measure]

  sealed trait Key

  case class Support(value: Int) extends Measure {
    def companion: Key = Support
  }

  object Support extends Key

  case class HeadCoverage(value: Double) extends Measure {
    def companion: Key = HeadCoverage
  }

  object HeadCoverage extends Key

  case class HeadSize(value: Int) extends Measure {
    def companion: Key = HeadSize
  }

  object HeadSize extends Key

  case class BodySize(value: Int) extends Measure {
    def companion: Key = BodySize
  }

  object BodySize extends Key

  case class Confidence(value: Double) extends Measure {
    def companion: Key = Confidence
  }

  object Confidence extends Key

  case class PcaBodySize(value: Int) extends Measure {
    def companion: Key = PcaBodySize
  }

  object PcaBodySize extends Key

  case class PcaConfidence(value: Double) extends Measure {
    def companion: Key = PcaConfidence
  }

  object PcaConfidence extends Key

  implicit def mesureToKeyValue(measure: Measure): (Key, Measure) = measure.companion -> measure

}
