package com.github.propi.rdfrules.gui.results

import com.github.propi.rdfrules.gui.ActionProgress
import com.github.propi.rdfrules.gui.results.Types.Predicate
import com.thoughtworks.binding.Binding.Constants
import com.thoughtworks.binding.Binding
import org.lrng.binding.html
import org.scalajs.dom.html.Div

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 14. 9. 2018.
  */
class Types(val title: String, val id: Future[String]) extends ActionProgress with Pagination[Predicate] {

  @html
  def viewRecord(record: Predicate): Binding[Div] = <div class="type">
    <div class="predicate">
      {Rules.viewAtomItem(record.predicate)}
    </div>
    <div class="predicate-types">
      {record.types.map(x => x.name + ": " + x.amount).mkString(", ")}
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
      {viewRecords(result.value.view.map(_.asInstanceOf[Predicate])).bind}
    </div>
    <div class="types-pages">
      {viewPages(result.value.size).bind}
    </div>
  </div>

}

object Types {

  implicit class PredicateOps(predicate: Predicate) {
    def getTypes: Constants[Type] = Constants(predicate.types.toList: _*)
  }

  trait Predicate extends js.Object {
    val predicate: js.Dynamic
    val types: js.Array[Type]
  }

  trait Type extends js.Object {
    val name: String
    val amount: Int
  }

}