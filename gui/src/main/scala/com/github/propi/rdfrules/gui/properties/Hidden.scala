package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Property
import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.binding.Binding.Var
import org.scalajs.dom.html.Div

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
class Hidden[T](val name: String, value: String)(implicit f: String => T, g: T => js.Any) extends Property {

  private var _value: String = value

  val title: String = ""
  val descriptionVar: Var[String] = Var("")

  isHidden.value = true

  @dom
  def valueView: Binding[Div] = {
    <div>
      <input type="hidden" name={name} value={_value}/>
    </div>
  }

  def validate(): Option[String] = None

  def setValue(data: js.Dynamic): Unit = {
    _value = data.toString
  }

  def toJson: js.Any = f(_value)
}