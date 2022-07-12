package com.github.propi.rdfrules.gui.results

import com.github.propi.rdfrules.gui.ActionProgress
import com.github.propi.rdfrules.gui.results.Prefixes.Prefix
import com.thoughtworks.binding.Binding.Constants
import com.thoughtworks.binding.Binding
import org.lrng.binding.html
import org.scalajs.dom.html.Div

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 14. 9. 2018.
  */
class Prefixes(val title: String, val id: Future[String]) extends ActionProgress with Pagination[Prefix] {

  @html
  def viewRecord(record: Prefix): Binding[Div] = <div class="prefix">
    <span class="prefix-name">
      {record.prefix}
    </span>
    <span class="sep">:</span>
    <span class="name-space">
      {record.nameSpace}
    </span>
  </div>

  @html
  def viewResult(result: Constants[js.Dynamic]): Binding[Div] = <div class="prefixes">
    <div class="prefixes-amount">
      <span class="text">Number of prefixes:</span>
      <span class="number">
        {result.value.size.toString}
      </span>
    </div>
    <div class="prefixes-body">
      {viewRecords(result.value.view.map(_.asInstanceOf[Prefix])).bind}
    </div>
    <div class="prefixes-pages">
      {viewPages(result.value.size).bind}
    </div>
  </div>

}

object Prefixes {

  trait Prefix extends js.Object {
    val prefix: String
    val nameSpace: String
  }

}