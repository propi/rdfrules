package com.github.propi.rdfrules.gui.results

import com.github.propi.rdfrules.gui.ActionProgress
import com.github.propi.rdfrules.gui.results.Rules.{Atom, Rule}
import com.thoughtworks.binding.Binding.Constants
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.html.{Div, Span}
import org.scalajs.dom.raw.HTMLSpanElement
import org.scalajs.{dom => dom2}

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 14. 9. 2018.
  */
class Rules(val title: String, val id: Future[String]) extends ActionProgress with Pagination[(Rule, Int)] {

  /*@dom
  private def viewAtom(atom: Atom): Binding[Div] = <div class="atom">
    <span class="symbol">(</span>
    <span class={"subject " + atom.subject.`type`}>
      {atom.subject.value.toString}
    </span>
    <span class="predicate">
      {atom.predicate}
    </span>
    <span class={"object " + atom.`object`.`type`}>
      {atom.`object`.value.toString}
    </span>{atom.graphs.toOption match {
      case Some(graphs) if graphs.length > 0 =>
        if (graphs.length == 1) {
          <span class="graph">
            {graphs.head}
          </span>
        } else {
          <span class="graph">
            [
            {graphs.mkString(", ")}
            ]
          </span>
        }
      case None => <span class="graph"></span>
    }}<span class="symbol">)</span>
  </div>*/

  /*
        {val span = <span></span>.asInstanceOf[Span]
    span.innerHTML = record.body.map(viewAtom).mkString(" ^ ") + " &rArr; " + viewAtom(record.head)
    span}
   */

  private def viewAtom(atom: Atom): String = s"( ${atom.subject.value} ${atom.predicate} ${atom.`object`.value}${atom.graphs.toOption.map(x => if (x.length == 1) x.head else x.mkString(", ")).map(" " + _).getOrElse("")} )"

  @dom
  def viewRecord(record: (Rule, Int)): Binding[Div] = <div class="rule">
    <div class="text">
      <span>
        {(record._2 + 1).toString + ":"}
      </span>
      <span>
        {record._1.body.map(viewAtom).mkString(" ^ ")}
      </span>
      <span>
        &rArr;
      </span>
      <span>
        {viewAtom(record._1.head)}
      </span>
    </div>
    <div class="measures">
      {record._1.measures.map(x => s"${x.name}: ${x.value}").mkString(", ")}
    </div>
  </div>

  @dom
  def viewResult(result: Constants[js.Dynamic]): Binding[Div] = <div class="rules">
    <div class="rules-amount">
      <span class="text">Number of rules:</span>
      <span class="number">
        {result.value.size.toString}
      </span>
    </div>
    <div class="rules-body">
      {viewRecords(result.value.view.map(_.asInstanceOf[Rule]).zipWithIndex).bind}
    </div>
    <div class="rules-pages">
      {viewPages(result.value.size).bind}
    </div>
  </div>

}

object Rules {

  trait AtomItem extends js.Object {
    val `type`: String
    val value: js.Dynamic
  }

  trait Atom extends js.Object {
    val subject: AtomItem
    val predicate: String
    val `object`: AtomItem
    val graphs: js.UndefOr[js.Array[String]]
  }

  trait Measure extends js.Object {
    val name: String
    val value: Double
  }

  trait Rule extends js.Object {
    val head: Atom
    val body: js.Array[Atom]
    val measures: js.Array[Measure]
  }

}