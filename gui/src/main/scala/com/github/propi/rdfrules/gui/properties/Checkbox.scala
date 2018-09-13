package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Property
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.Event
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.HTMLInputElement

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
case class Checkbox(name: String, title: String, default: Boolean = false) extends Property {

  private var isChecked: Boolean = default

  @dom
  final protected def valueView: Binding[Div] = {
    <div>
      <input type="checkbox" checked={isChecked} onchange={e: Event => isChecked = e.srcElement.asInstanceOf[HTMLInputElement].checked}/>
    </div>
  }

}