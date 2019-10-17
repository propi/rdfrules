package com.github.propi.rdfrules.model

/**
  * Created by Vaclav Zeman on 15. 10. 2019.
  */
case class EvaluationResult(tp: Int, fp: Int, fn: Int) {

  def accuracy: Double = tp.toDouble / (tp + fp + fn)

  def precision: Double = tp.toDouble / (tp + fp)

  def recall: Double = tp.toDouble / (tp + fn)

  def fscore: Double = {
    val p = precision
    val r = recall
    if (p + r > 0) (2 * p * r) / (p + r) else 0
  }

  override def toString: String = s"accuracy: $accuracy, precision: $precision, recall: $recall, f-measure: $fscore (TP: $tp, FP: $fp, FN: $fn)"

}