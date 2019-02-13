package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Property
import com.github.propi.rdfrules.gui.utils.Validate._
import com.thoughtworks.binding.Binding.Var
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.Event
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.HTMLInputElement

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
abstract class Text(val name: String, val title: String, default: String, description: String, validator: Validator[String]) extends Property {

  private var text: String = default

  val descriptionVar: Binding.Var[String] = Var(description)

  final def getText: String = text

  def setValue(data: js.Dynamic): Unit = {
    text = data.toString
  }

  def validate(): Option[String] = {
    val msg = validator.validate(text).errorMsg
    errorMsg.value = msg
    msg
  }

  @dom
  final def valueView: Binding[Div] = {
    <div>
      <input type="text" value={text} onkeyup={e: Event =>
      text = e.target.asInstanceOf[HTMLInputElement].value
      validate()}/>
    </div>
  }

}