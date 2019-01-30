package com.github.propi.rdfrules.gui

import com.github.propi.rdfrules.gui.utils.ReactiveBinding
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.html.{Div, TableRow}

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
trait Property {
  val name: String
  val title: String
  val description: String

  val errorMsg: ReactiveBinding.Var[Option[String]] = ReactiveBinding.Var(None)

  def valueView: Binding[Div]

  def isValid: Boolean = errorMsg.value.isEmpty

  def validate(): Option[String]

  @dom
  def view: Binding[TableRow] = {
    <tr>
      <th>
        <div class="title">
          <div class="hints">
            <div class={"error" + (if (errorMsg.binding.bind.isEmpty) " hidden" else "")} onmousemove={e: MouseEvent => Main.canvas.openHint(errorMsg.value.getOrElse(""), e)} onmouseout={_: MouseEvent => Main.canvas.closeHint()}>
              <i class="material-icons">error</i>
            </div>
            <div class={"description" + (if (description.isEmpty) " hidden" else "")} onmousemove={e: MouseEvent => Main.canvas.openHint(description, e)} onmouseout={_: MouseEvent => Main.canvas.closeHint()}>
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