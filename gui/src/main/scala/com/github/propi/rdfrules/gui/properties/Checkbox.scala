package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Property
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.Event
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.HTMLInputElement

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
class Checkbox(val name: String, val title: String, default: Boolean = false, val description: String = "") extends Property {

  private var isChecked: Boolean = default

  def validate(): Option[String] = None

  def toJson: js.Any = isChecked

  @dom
  final def valueView: Binding[Div] = {
    <div>
      <input type="checkbox" class="checkbox" checked={isChecked} onchange={e: Event => isChecked = e.srcElement.asInstanceOf[HTMLInputElement].checked}/>
    </div>
  }

}