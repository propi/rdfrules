package com.github.propi.rdfrules.gui.results

sealed trait PredictedResult {
  def symbol: String

  def label: String

  val id: String
}

object PredictedResult {

  case object Positive extends PredictedResult {
    val id: String = "Positive"

    def symbol: String = "+"

    def label: String = id

    override def toString: String = id
  }

  case object Negative extends PredictedResult {
    val id: String = "Negative"

    def symbol: String = "-"

    def label: String = id

    override def toString: String = id
  }

  case object PcaPositive extends PredictedResult {
    val id: String = "PcaPositive"

    def symbol: String = "?"

    def label: String = "PCA positive"

    override def toString: String = id
  }

  def apply(x: String): Option[PredictedResult] = x match {
    case Positive.id => Some(Positive)
    case Negative.id => Some(Negative)
    case PcaPositive.id => Some(PcaPositive)
    case _ => None
  }

}