package com.github.propi.rdfrules.gui.results

import com.github.propi.rdfrules.gui.ActionProgress
import com.github.propi.rdfrules.gui.results.Histogram.HistogramItem
import com.thoughtworks.binding.Binding.Constants
import com.thoughtworks.binding.Binding
import org.lrng.binding.html
import org.scalajs.dom.html.Div

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSNumberOps._

/**
  * Created by Vaclav Zeman on 14. 9. 2018.
  */
class Histogram(val title: String, val id: Future[String]) extends ActionProgress with Pagination[HistogramItem] {

  private var max: Double = 1

  @html
  def viewRecord(record: HistogramItem): Binding[Div] = <div class="item">
    <div class="info">
      <div class={"subject" + (if (record.subject == null) " empty" else "")}>
        {if (record.subject == null) "*" else Rules.viewAtomItem(record.subject)}
      </div>
      <div class={"predicate" + (if (record.predicate == null) " empty" else "")}>
        {if (record.predicate == null) "*" else Rules.viewAtomItem(record.predicate)}
      </div>
      <div class={"object" + (if (record.`object` == null) " empty" else "")}>
        {if (record.`object` == null) "*" else Rules.viewAtomItem(record.`object`)}
      </div>
      <div class="amount">
        {record.amount.toString}
      </div>
    </div>
    <div class="status">
      <div class="bar" style={"width: " + math.round((record.amount / max) * 100).toString + "%"}>
        {s"${((record.amount / max) * 100).toFixed(2).toDouble} %"}
      </div>
    </div>
  </div>

  @html
  def viewResult(result: Constants[js.Dynamic]): Binding[Div] = {
    max = result.value.iterator.map(_.asInstanceOf[HistogramItem]).map(_.amount).max
    <div class="histogram">
      <div class="histogram-amount">
        <span class="text">Number of values:</span>
        <span class="number">
          {result.value.size.toString}
        </span>
      </div>
      <div class="histogram-body">
        {viewRecords(result.value.view.map(_.asInstanceOf[HistogramItem])).bind}
      </div>
      <div class="histogram-pages">
        {viewPages(result.value.size).bind}
      </div>
    </div>
  }

}

object Histogram {

  trait HistogramItem extends js.Object {
    val subject: js.Dynamic
    val predicate: js.Dynamic
    val `object`: js.Dynamic
    val amount: Int
  }

}