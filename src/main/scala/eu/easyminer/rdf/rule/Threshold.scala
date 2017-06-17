package eu.easyminer.rdf.rule

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
sealed trait Threshold

object Threshold {

  type Thresholds = collection.Map[Any, Threshold]

  case class MinSupport(value: Int) extends Threshold

  case class MinHeadCoverage(value: Double) extends Threshold

  case class MaxRuleLength(value: Int) extends Threshold

}
