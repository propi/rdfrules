package com.github.propi.rdfrules.gui.results

import com.github.propi.rdfrules.gui.ActionProgress
import com.thoughtworks.binding.Binding
import com.thoughtworks.binding.Binding.Constants
import org.lrng.binding.html
import org.scalajs.dom.html.Div

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 14. 9. 2018.
  */
class EvaluationResult(val title: String, val id: Future[String]) extends ActionProgress {

  @html
  def viewResult(result: Constants[js.Dynamic]): Binding[Div] = {
    val ranking = result.value.head.asInstanceOf[EvaluationResult.Ranking]
    val completeness = result.value(1).asInstanceOf[EvaluationResult.Completeness]
    val stats = result.value(2).asInstanceOf[EvaluationResult.Stats]
    <div class="evaluation">
      <div class="stats">
        {s"Number of evaluated prediction tasks: ${stats.n}"}
      </div>
      <div class="ranking">
        <div>
          {s"Total entities (E): ${ranking.total}"}
        </div>
        <div>
          {s"Total correctly predicted entities (TP): ${ranking.totalCorrect}"}
        </div>
        <div>
          {s"Covered entities by correct prediction (TP / E): ${(ranking.totalCorrect.toDouble / ranking.total) * 100}%"}
        </div>
        <div>
          {s"Mean rank: ${ranking.mr}"}
        </div>
        <div>
          {s"Mean reciprocal rank: ${ranking.mrr * 100}%"}
        </div>{for (hit <- ranking.hits) yield <div>
        {s"Hits@${hit.k}: ${hit.v * 100}%"}
      </div>}
      </div>
      <div class="completeness">
        <div>
          {s"Precision: ${completeness.precision * 100}%"}
        </div>
        <div>
          {s"Recall: ${completeness.recall * 100}%"}
        </div>
        <div>
          {s"F-Measure: ${completeness.fscore * 100}%"}
        </div>
        <table class="confusion-matrix">
          <tr>
            <td></td>
            <th>In KG</th>
            <th>Not in KG</th>
          </tr>
          <tr>
            <th>Predicted</th>
            <td class="true">
              {completeness.tp.toString}
            </td>
            <td class="false">
              {completeness.fp.toString}
            </td>
          </tr>
          <tr>
            <th>Not predicted</th>
            <td class="false">
              {completeness.fn.toString}
            </td>
            <td>
              &nbsp;
            </td>
          </tr>
        </table>
      </div>
    </div>
  }

}

object EvaluationResult {

  trait Hit extends js.Object {
    val k: Int
    val v: Double
  }

  trait Ranking extends js.Object {
    val hits: js.Array[Hit]
    val mr: Double
    val mrr: Double
    val total: Int
    val totalCorrect: Int
  }

  trait Completeness extends js.Object {
    val tp: Int
    val fp: Int
    val fn: Int
    val precision: Double
    val recall: Double
    val fscore: Double
    val accuracy: Double
  }

  trait Stats extends js.Object {
    val n: Int
  }

}