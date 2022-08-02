package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Documentation.Context
import com.github.propi.rdfrules.gui.Property
import com.thoughtworks.binding.Binding.Var
import com.thoughtworks.binding.Binding
import org.lrng.binding.html
import org.lrng.binding.html.NodeBinding
import org.scalajs.dom.Event
import org.scalajs.dom.html.{Div, Span}
import org.scalajs.dom.raw.HTMLInputElement

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
class Checkbox(val name: String, val title: String, default: Boolean = false, onChecked: Boolean => Unit = _ => {}, val summaryTitle: String = "")(implicit context: Context) extends Property {

  private val _isChecked: Var[Boolean] = Var(default)

  val descriptionVar: Binding.Var[String] = Var(context(title).description)

  def setValue(data: Boolean): Unit = {
    _isChecked.value = data
    onChecked(data)
  }

  def setValue(data: js.Dynamic): Unit = setValue(data.asInstanceOf[Boolean])

  def isChecked: Boolean = _isChecked.value

  def validate(): Option[String] = None

  def toJson: js.Any = isChecked

  @html
  final def summaryContentView: Binding[Span] = <span>{Binding.BindingInstances.map(_isChecked)(x => if (x) "yes" else "no").bind}</span>

  @html
  final def valueView: NodeBinding[Div] = {
    <div>
      <input type="checkbox" class="checkbox" checked={_isChecked} onchange={e: Event =>
      _isChecked.value = e.target.asInstanceOf[HTMLInputElement].checked
      onChecked(_isChecked.value)}/>
    </div>
  }

}