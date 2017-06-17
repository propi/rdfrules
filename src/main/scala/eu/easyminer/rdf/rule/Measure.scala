package eu.easyminer.rdf.rule

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
sealed trait Measure

object Measure {

  type Measures = collection.Map[Any, Measure]

  case class Support(value: Int) extends Measure

  case class HeadCoverage(value: Double) extends Measure

  case class HeadSize(value: Int) extends Measure

  case class BodySize(value: Int) extends Measure

  case class Confidence(value: Double) extends Measure

  case class PcaBodySize(value: Int) extends Measure

  case class PcaConfidence(value: Double) extends Measure

}
