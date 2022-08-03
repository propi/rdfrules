package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Documentation.Context
import com.github.propi.rdfrules.gui.Property
import com.github.propi.rdfrules.gui.utils.Validate._
import com.thoughtworks.binding.Binding
import com.thoughtworks.binding.Binding.{Constant, Var}
import org.lrng.binding.html
import org.scalajs.dom.Event
import org.scalajs.dom.html.{Div, Span}
import org.scalajs.dom.raw.HTMLInputElement
import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
abstract class Text(val name: String, val title: String, default: String, validator: Validator[String], val summaryTitle: String)(implicit context: Context) extends Property {

  private val text: Var[String] = Var(default)

  val descriptionVar: Binding.Var[String] = Var(context(title).description)

  final def getText: String = text.value

  def setValue(data: js.Dynamic): Unit = {
    text.value = data.toString
  }

  def validate(): Option[String] = {
    val msg = validator.validate(text.value).errorMsg
    errorMsg.value = msg
    msg
  }

  @html
  final def valueView: Binding[Div] = {
    <div>
      <input type="text" value={text.bind} onkeyup={e: Event =>
      text.value = e.target.asInstanceOf[HTMLInputElement].value
      validate()}/>
    </div>
  }

  @html
  final def summaryContentView: Binding[Span] = <span>
    {text.bind}
  </span>

  override def hasSummary: Binding[Boolean] = Constant(summaryTitle.isEmpty).ifM(Constant(false), text.map(_.trim.nonEmpty))
}