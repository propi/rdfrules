package com.github.propi.rdfrules.gui.results

import com.github.propi.rdfrules.gui.ActionProgress
import com.github.propi.rdfrules.gui.results.PropertiesCardinalities.PropertyCardinalities
import com.github.propi.rdfrules.gui.results.Types.Predicate
import com.thoughtworks.binding.Binding
import com.thoughtworks.binding.Binding.Constants
import org.lrng.binding.html
import org.scalajs.dom.html.Div

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 14. 9. 2018.
  */
class PropertiesCardinalities(val title: String, val id: Future[String]) extends ActionProgress with Pagination[PropertyCardinalities] {

  @html
  def viewRecord(record: PropertyCardinalities): Binding[Div] = <div class="type">
    <div class="predicate">
      {Rules.viewAtomItem(record.property)}
    </div>
    <div class="predicate-types">
      size:
      {record.size.toString}
      , domain:
      {record.domain.toString}
      , range:
      {record.range.toString}
    </div>
  </div>

  @html
  def viewResult(result: Constants[js.Dynamic]): Binding[Div] = <div class="types">
    <div class="types-amount">
      <span class="text">Number of predicates:</span>
      <span class="number">
        {result.value.size.toString}
      </span>
    </div>
    <div class="types-body">
      {viewRecords(result.value.view.map(_.asInstanceOf[PropertyCardinalities])).bind}
    </div>
    <div class="types-pages">
      {viewPages(result.value.size).bind}
    </div>
  </div>
}

object PropertiesCardinalities {

  trait PropertyCardinalities extends js.Object {
    val property: js.Dynamic
    val size: Int
    val domain: Int
    val range: Int
  }

}