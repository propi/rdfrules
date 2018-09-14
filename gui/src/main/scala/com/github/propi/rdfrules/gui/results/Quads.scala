package com.github.propi.rdfrules.gui.results

import com.github.propi.rdfrules.gui.ActionProgress
import com.github.propi.rdfrules.gui.results.Quads.Quad
import com.thoughtworks.binding.Binding.Constants
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.html.Div

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 14. 9. 2018.
  */
class Quads(val name: String, val id: Future[String]) extends ActionProgress {

  @dom
  def viewResult(result: Constants[js.Dynamic]): Binding[Div] = <div class="quads">
    {for (quad <- result.map(_.asInstanceOf[Quad])) yield
      <div class="quad">
        <div class="subject">
          {quad.subject}
        </div>
        <div class="predicate">
          {quad.predicate}
        </div>
        <div class="object">
          {quad.`object`.toString}
        </div>
        <div class="graph">
          {quad.graph}
        </div>
      </div>}
  </div>

}

object Quads {

  trait Quad extends js.Object {
    val subject: String
    val predicate: String
    val `object`: js.Dynamic
    val graph: String
  }

}