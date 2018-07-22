package com.github.propi.rdfrules.gui

import com.github.propi.rdfrules.gui.operations.Root
import com.thoughtworks.binding.Binding.{Var, Vars}
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.{Event, document}
import org.scalajs.dom.html.Div

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class Canvas {

  private val modal: Var[Option[Binding[Div]]] = Var(None)
  private val operations: Vars[Operation] = Vars(new Root)

  @dom
  private def view: Binding[Div] = {
    <div class="canvas">
      {val x = modal.bind
    if (x.isEmpty) {
      <div class="modal closed"></div>
    } else {
      <div class="modal open">
        <a class="close" onclick={_: Event => closeModal()}>
          <i class="material-icons">close</i>
        </a>{x.get.bind}
      </div>
    }}<div class="content">
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

  def render(): Unit = dom.render(document.getElementById("rdfrules"), view)

}
