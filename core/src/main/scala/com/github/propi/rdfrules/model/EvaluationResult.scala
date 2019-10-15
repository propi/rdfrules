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
    (2 * p * r) / (p + r)
  }

}