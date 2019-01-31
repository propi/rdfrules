package com.github.propi.rdfrules.gui

import com.github.propi.rdfrules.gui.operations.Root
import com.thoughtworks.binding.Binding.{Var, Vars}
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.HTMLElement
import org.scalajs.dom.{Event, MouseEvent, document}

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class Canvas {

  private val modal: Var[Option[Binding[Div]]] = Var(None)
  private val fixedHint: Var[Option[(Double, Double)]] = Var(None)
  private val hint: Var[Option[(String, Double, Double)]] = Var(None)
  private val operations: Vars[Operation] = Vars(new Root)

  @dom
  private def view: Binding[Div] = {
    <div class="canvas">
      <div class={"whint" + (if (hint.bind.isEmpty) " hidden" else "")} style={val _hint = hint.bind
      val _fixedHint = fixedHint.bind
      (_hint, _fixedHint) match {
        case (Some((_, x, y)), None) => s"left: ${x}px; top: ${y}px;"
        case (_, Some((x, y))) => s"left: ${x}px; top: ${y}px; position: absolute;"
        case _ => ""
      }}>
        <i class={"material-icons close" + (if (fixedHint.bind.isEmpty) " hidden" else "")} onclick={_: Event => unfixHint()}>close</i>
        {hint.bind.map(_._1).getOrElse("")}
      </div>
      <div class={"modal" + (if (modal.bind.isEmpty) " closed" else " open")}>
        <a class="close" onclick={_: Event => closeModal()}>
          <i class="material-icons">close</i>
        </a>{modal.bind match {
        case Some(x) => x.bind
        case None => <div></div>
      }}
      </div>
      <div class="content">
        <div class="operations">
          {for (operation <- operations) yield {
          operation.view.bind
        }}
        </div>
      </div>
    </div>
  }

  def addOperation(operation: Operation): Unit = operations.value += operation

  def deleteLastOperation(): Unit = operations.value.remove(operations.value.size - 1)

  def openModal(content: Binding[Div]): Unit = modal.value = Some(content)

  def closeModal(): Unit = modal.value = None

  def openHint(x: String, event: MouseEvent): Unit = hint.value = Some(x, event.pageX + 10, event.pageY + 10)

  def fixHint(time: Int = 300): Unit = {
    for ((x, y) <- hint.value.map(x => x._2 -> (x._3 - document.getElementById("rdfrules").asInstanceOf[HTMLElement].offsetTop))) {
      val steps = math.ceil(math.max(25, time) / 25.0)
      val xDelta = x / steps
      val yDelta = y / steps
      def moveHint(): Unit = {
        for ((x, y) <- fixedHint.value if x > 0 || y > 0) {
          fixedHint.value = Some(math.max(0, x - xDelta), math.max(0, y - yDelta))
          js.timers.setTimeout(25)(moveHint())
        }
      }
      fixedHint.value = Some(x, y)
      moveHint()
    }
  }

  def unfixHint(): Unit = {
    fixedHint.value = None
    closeHint()
  }

  def closeHint(): Unit = {
    if (fixedHint.value.isEmpty) hint.value = None
  }

  def render(): Unit = dom.render(document.getElementById("rdfrules"), view)

}
