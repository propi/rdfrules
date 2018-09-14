package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Property
import com.thoughtworks.binding.Binding.Constants
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.Event
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.HTMLSelectElement

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
case class Select(name: String, title: String, items: Constants[(String, String)]) extends Property {

  private var selectedItem: Option[String] = None

  def toJson: js.Any = selectedItem match {
    case Some(x) => x
    case None => js.undefined
  }

  @dom
  final protected def valueView: Binding[Div] = {
    <div>
      <select onchange={e: Event =>
        val x = e.srcElement.asInstanceOf[HTMLSelectElement].value
        selectedItem = if (x.isEmpty) None else Some(x)}>
        <option value=""></option>{for (item <- items) yield
        <option value={item._1} selected={selectedItem.contains(item._1)}>
          {item._2}
        </option>}
      </select>
    </div>
  }

}