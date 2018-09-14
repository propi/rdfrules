package com.github.propi.rdfrules.gui.results

import com.github.propi.rdfrules.gui.ActionProgress
import com.github.propi.rdfrules.gui.results.Histogram.HistogramItem
import com.thoughtworks.binding.Binding.Constants
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.html.Div

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 14. 9. 2018.
  */
class Histogram(val name: String, val id: Future[String]) extends ActionProgress {

  @dom
  def viewResult(result: Constants[js.Dynamic]): Binding[Div] = <div class="histogram">
    {for (item <- result.map(_.asInstanceOf[HistogramItem])) yield
      <div class="item">
        <div class="subject">
          {item.subject}
        </div>
        <div class="predicate">
          {item.predicate}
        </div>
        <div class="object">
          {if (item.`object` == null) "null" else item.`object`.toString}
        </div>
        <div class="amount">
          {item.amount.toString}
        </div>
      </div>}
  </div>

}

object Histogram {

  trait HistogramItem extends js.Object {
    val subject: String
    val predicate: String
    val `object`: js.Dynamic
    val amount: Int
  }

}