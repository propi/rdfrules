package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.utils.{Stringifier, TypedKeyMap}
import com.github.propi.rdfrules.utils.TypedKeyMap.{Key, Value}
import spray.json.DefaultJsonProtocol._
import spray.json._

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

  case class HeadSupport(value: Int) extends Measure {
    def companion: HeadSupport.type = HeadSupport
  }

  implicit object HeadSupport extends Key[HeadSupport]

  case class HeadCoverage(value: Double) extends Measure {
    def companion: HeadCoverage.type = HeadCoverage
  }

  implicit object HeadCoverage extends Key[HeadCoverage]

  case class SupportIncreaseRatio(value: Float) extends Measure {
    def companion: SupportIncreaseRatio.type = SupportIncreaseRatio
  }

  implicit object SupportIncreaseRatio extends Key[SupportIncreaseRatio]

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

  case class Cluster(number: Int) extends Measure {
    def companion: Cluster.type = Cluster
  }

  implicit object Cluster extends Key[Cluster]

  def unapply(arg: Measure): Option[Double] = arg match {
    case Measure.BodySize(x) => Some(x)
    case Measure.Confidence(x) => Some(x)
    case Measure.HeadConfidence(x) => Some(x)
    case Measure.HeadCoverage(x) => Some(x)
    case Measure.HeadSize(x) => Some(x)
    case Measure.SupportIncreaseRatio(x) => Some(x)
    case Measure.Lift(x) => Some(x)
    case Measure.PcaBodySize(x) => Some(x)
    case Measure.PcaConfidence(x) => Some(x)
    case Measure.Support(x) => Some(x)
    case Measure.HeadSupport(x) => Some(x)
    case Measure.Cluster(x) => Some(x)
  }

  implicit def mesureToKeyValue(measure: Measure): (Key[Measure], Measure) = measure.companion -> measure

  implicit val measureKeyOrdering: Ordering[Key[Measure]] = {
    val map = Iterator[Key[Measure]](Support, HeadCoverage, Confidence, PcaConfidence, Lift, HeadConfidence, HeadSize, SupportIncreaseRatio, BodySize, PcaBodySize, HeadSupport, Cluster).zipWithIndex.map(x => x._1 -> (x._2 + 1)).toMap
    Ordering.by[Key[Measure], Int](map.getOrElse(_, 0))
  }

  implicit val measureOrdering: Ordering[Measure] = Ordering.by[Measure, Double] {
    case Measure.BodySize(x) => x
    case Measure.Confidence(x) => x
    case Measure.HeadConfidence(x) => x
    case Measure.HeadCoverage(x) => x
    case Measure.HeadSize(x) => x
    case Measure.SupportIncreaseRatio(x) => x
    case Measure.Lift(x) => x
    case Measure.PcaBodySize(x) => x
    case Measure.PcaConfidence(x) => x
    case Measure.Support(x) => x
    case Measure.HeadSupport(x) => x
    case Measure.Cluster(x) => x * -1
  }.reverse

  implicit val measuresOrdering: Ordering[TypedKeyMap.Immutable[Measure]] = Ordering.by[TypedKeyMap.Immutable[Measure], (Measure, Measure, Measure, Measure, Measure)] { measures =>
    (
      measures.get(Measure.Cluster).getOrElse(Measure.Cluster(0)),
      measures.get(Measure.PcaConfidence).getOrElse(Measure.PcaConfidence(0)),
      measures.get(Measure.Lift).getOrElse(Measure.Lift(0)),
      measures.get(Measure.Confidence).getOrElse(Measure.Confidence(0)),
      measures.get(Measure.HeadCoverage).getOrElse(Measure.HeadCoverage(0))
    )
  }

  implicit val measureStringifier: Stringifier[Measure] = {
    case Measure.Support(v) => s"support: $v"
    case Measure.HeadCoverage(v) => s"headCoverage: $v"
    case Measure.Confidence(v) => s"confidence: $v"
    case Measure.Lift(v) => s"lift: $v"
    case Measure.PcaConfidence(v) => s"pcaConfidence: $v"
    case Measure.HeadConfidence(v) => s"headConfidence: $v"
    case Measure.HeadSize(v) => s"headSize: $v"
    case Measure.SupportIncreaseRatio(v) => s"supportIncreaseRatio: $v"
    case Measure.BodySize(v) => s"bodySize: $v"
    case Measure.PcaBodySize(v) => s"pcaBodySize: $v"
    case Measure.HeadSupport(v) => s"headSupport: $v"
    case Measure.Cluster(v) => s"cluster: $v"
  }

  implicit val measureJsonFormat: RootJsonFormat[Measure] = new RootJsonFormat[Measure] {
    def write(obj: Measure): JsValue = obj match {
      case Measure.BodySize(x) => JsObject("name" -> JsString("BodySize"), "value" -> JsNumber(x))
      case Measure.Confidence(x) => JsObject("name" -> JsString("Confidence"), "value" -> JsNumber(x))
      case Measure.HeadConfidence(x) => JsObject("name" -> JsString("HeadConfidence"), "value" -> JsNumber(x))
      case Measure.HeadCoverage(x) => JsObject("name" -> JsString("HeadCoverage"), "value" -> JsNumber(x))
      case Measure.HeadSize(x) => JsObject("name" -> JsString("HeadSize"), "value" -> JsNumber(x))
      case Measure.SupportIncreaseRatio(x) => JsObject("name" -> JsString("SupportIncreaseRatio"), "value" -> JsNumber(x))
      case Measure.Lift(x) => JsObject("name" -> JsString("Lift"), "value" -> JsNumber(x))
      case Measure.PcaBodySize(x) => JsObject("name" -> JsString("PcaBodySize"), "value" -> JsNumber(x))
      case Measure.PcaConfidence(x) => JsObject("name" -> JsString("PcaConfidence"), "value" -> JsNumber(x))
      case Measure.Support(x) => JsObject("name" -> JsString("Support"), "value" -> JsNumber(x))
      case Measure.HeadSupport(x) => JsObject("name" -> JsString("HeadSupport"), "value" -> JsNumber(x))
      case Measure.Cluster(x) => JsObject("name" -> JsString("Cluster"), "value" -> JsNumber(x))
    }

    def read(json: JsValue): Measure = {
      val fields = json.asJsObject.fields
      val value = fields("value")
      fields("name").convertTo[String] match {
        case "BodySize" => Measure.BodySize(value.convertTo[Int])
        case "Confidence" => Measure.Confidence(value.convertTo[Double])
        case "HeadConfidence" => Measure.HeadConfidence(value.convertTo[Double])
        case "HeadCoverage" => Measure.HeadCoverage(value.convertTo[Double])
        case "HeadSize" => Measure.HeadSize(value.convertTo[Int])
        case "SupportIncreaseRatio" => Measure.SupportIncreaseRatio(value.convertTo[Float])
        case "Lift" => Measure.Lift(value.convertTo[Double])
        case "PcaBodySize" => Measure.PcaBodySize(value.convertTo[Int])
        case "PcaConfidence" => Measure.PcaConfidence(value.convertTo[Double])
        case "Support" => Measure.Support(value.convertTo[Int])
        case "HeadSupport" => Measure.HeadSupport(value.convertTo[Int])
        case "Cluster" => Measure.Cluster(value.convertTo[Int])
        case x => deserializationError(s"Invalid measure of significance: $x")
      }
    }
  }

}