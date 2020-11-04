package com.github.propi.rdfrules.gui

import com.github.propi.rdfrules.gui.utils.ReactiveBinding
import com.thoughtworks.binding.Binding.Var
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.{Event, MouseEvent}
import org.scalajs.dom.html.{Div, TableRow}

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
trait Property {
  val name: String
  val title: String
  val descriptionVar: Var[String]

  val errorMsg: ReactiveBinding.Var[Option[String]] = ReactiveBinding.Var(None)
  val isHidden: Var[Boolean] = Var(false)

  def valueView: Binding[Div]

  def isValid: Boolean = errorMsg.value.isEmpty

  def validate(): Option[String]

  def setValue(data: js.Dynamic): Unit

  @dom
  def view: Binding[TableRow] = {
    <tr class={if (isHidden.bind) "hidden" else ""}>
      <th>
        <div class="title">
          <div class="hints">
            <div class={"error" + (if (errorMsg.binding.bind.isEmpty) " hidden" else "")} onmousemove={e: MouseEvent => Main.canvas.openHint(errorMsg.value.getOrElse(""), e)} onmouseout={_: MouseEvent => Main.canvas.closeHint()} onclick={_: Event => Main.canvas.fixHint()}>
              <i class="material-icons">error</i>
            </div>
            <div class={"description" + (if (descriptionVar.bind.isEmpty) " hidden" else "")} onmousemove={e: MouseEvent => Main.canvas.openHint(descriptionVar.value, e)} onmouseout={_: MouseEvent => Main.canvas.closeHint()} onclick={_: Event => Main.canvas.fixHint()}>
              <i class="material-icons">help</i>
            </div>
          </div>
          <div class="text">
            {title}
          </div>
        </div>
      </th>
      <td>
        {valueView.bind}
      </td>
    </tr>
  }

  def toJson: js.Any
}