package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Property
import com.thoughtworks.binding.Binding.{Constants, Var}
import com.thoughtworks.binding.Binding
import org.lrng.binding.html
import org.lrng.binding.html.NodeBinding
import org.scalajs.dom.Event
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.HTMLSelectElement

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
class Select(val name: String, val title: String, items: Constants[(String, String)], default: Option[String] = None, onSelect: String => Unit = _ => {}, description: String = "") extends Property {

  val descriptionVar: Binding.Var[String] = Var(description)

  private var selectedItem: Option[String] = default
  private val preparedItems: Constants[(String, String)] = if (default.isEmpty) {
    Constants((("" -> "") +: items.value).toList: _*)
  } else {
    items
  }

  def validate(): Option[String] = None

  def setValue(data: js.Dynamic): Unit = {
    val x = data.toString
    onSelect(x)
    selectedItem = Some(x)
  }

  def toJson: js.Any = selectedItem match {
    case Some(x) => x
    case None => js.undefined
  }

  @html
  final def valueView: NodeBinding[Div] = {
    <div>
      <select onchange={e: Event =>
        val x = e.target.asInstanceOf[HTMLSelectElement].value
        onSelect(x)
        selectedItem = if (x.isEmpty) None else Some(x)}>
        {for (item <- preparedItems) yield
        <option value={item._1} selected={selectedItem.contains(item._1)}>
          {item._2}
        </option>}
      </select>
    </div>
  }

}