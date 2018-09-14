package com.github.propi.rdfrules.gui.results

import com.github.propi.rdfrules.gui.ActionProgress
import com.github.propi.rdfrules.gui.results.Types.Predicate
import com.thoughtworks.binding.Binding.Constants
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.html.Div

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 14. 9. 2018.
  */
class Types(val name: String, val id: Future[String]) extends ActionProgress {

  @dom
  def viewResult(result: Constants[js.Dynamic]): Binding[Div] = <div class="types">
    {for (item <- result.map(_.asInstanceOf[Predicate])) yield
      <div class="type">
        <div class="predicate">
          {item.predicate}
        </div>
        <div class="predicate-types">
          {for (typeItem <- item.getTypes) yield
          <div class="type-item">
            <div class="name">
              {typeItem.name}
            </div>
            <div class="amount">
              {typeItem.amount.toString}
            </div>
          </div>}
        </div>
      </div>}
  </div>

}

object Types {

  implicit class PredicateOps(predicate: Predicate) {
    def getTypes: Constants[Type] = Constants(predicate.types: _*)
  }

  trait Predicate extends js.Object {
    val predicate: String
    val types: js.Array[Type]
  }

  trait Type extends js.Object {
    val name: String
    val amount: Int
  }

}