package com.github.propi.rdfrules.gui

import com.github.propi.rdfrules.gui.operations.Root
import com.thoughtworks.binding.Binding.{Var, Vars}
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.html.Div
import org.scalajs.dom.{Event, MouseEvent, document}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class Canvas {

  private val modal: Var[Option[Binding[Div]]] = Var(None)
  private val hint: Var[Option[(String, Double, Double)]] = Var(None)
  private val operations: Vars[Operation] = Vars(new Root)

  @dom
  private def view: Binding[Div] = {
    <div class="canvas">
      <div class={"whint" + (if (hint.bind.isEmpty) " hidden" else "")} style={hint.bind match {
        case Some((_, x, y)) => s"left: ${x}px; top: ${y}px;"
        case None => ""
      }}>
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

  def closeHint(): Unit = hint.value = None

  def render(): Unit = dom.render(document.getElementById("rdfrules"), view)

}
