package com.github.propi.rdfrules.gui.results

import com.github.propi.rdfrules.gui.ActionProgress
import com.thoughtworks.binding.Binding.Constants
import com.thoughtworks.binding.Binding
import org.lrng.binding.html
import org.scalajs.dom.html.Div

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 14. 9. 2018.
  */
class EvaluationResult(val title: String, val id: Future[String]) extends ActionProgress {

  private val rules = new Rules(title, Future.failed(new NoSuchElementException))

  @html
  def viewResult(result: Constants[js.Dynamic]): Binding[Div] = {
    val data = result.value.head.asInstanceOf[EvaluationResult.Data]
    <div>
      <div>Precision: {(data.precision * 100).toString}%</div>
      <div>Recall: {(data.recall * 100).toString}%</div>
      <div>F-Measure: {(data.fscore * 100).toString}%</div>
      <div>Accuracy: {(data.accuracy * 100).toString}%</div>
      <table class="confusion-matrix">
        <tr>
          <td></td>
          <th>KG: Included</th>
          <th>KG: Missing</th>
        </tr>
        <tr>
          <th>Predicted: Included</th>
          <td class="true">{data.tp.toString}</td>
          <td class="false">{data.fp.toString}</td>
        </tr>
        <tr>
          <th>Predicted: Missing</th>
          <td class="false">{data.fn.toString}</td>
          <td>&nbsp;</td>
        </tr>
      </table>
    </div>
  }

}

object EvaluationResult {

  trait Data extends js.Object {
    val tp: Int
    val fp: Int
    val fn: Int
    val precision: Double
    val recall: Double
    val fscore: Double
    val accuracy: Double
  }

}