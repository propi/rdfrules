package com.github.propi.rdfrules.gui

import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.html.{Div, TableRow}

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
trait Property {
  val name: String
  val title: String

  protected def valueView: Binding[Div]

  @dom
  def view: Binding[TableRow] = {
    <tr class={name}>
      <th>
        {title}
      </th>
      <td>
        {valueView.bind}
      </td>
    </tr>
  }

  def toJson: js.Any

}
