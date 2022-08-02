package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Documentation.Context
import com.github.propi.rdfrules.gui.utils.Validate.{NoValidator, Validator}
import com.thoughtworks.binding.Binding
import com.thoughtworks.binding.Binding.Constant

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.util.Success

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
class OptionalText[T](name: String,
                      title: String,
                      default: String = "",
                      validator: Validator[String] = NoValidator[String](),
                      summaryTitle: String = "")
                     (implicit f: String => T, g: T => js.Any, context: Context) extends Text(name, title, default, (x: String) => if (x.isEmpty) Success(x) else validator.validate(x), summaryTitle) {
  def toJson: js.Any = if (getText.isEmpty) js.undefined.asInstanceOf[UndefOr[T]] else f(getText)

  override def hasSummary: Binding[Boolean] = Binding.BindingInstances.ifM(Constant(summaryTitle.isEmpty), Constant(false), Binding.BindingInstances.map(text)(_.nonEmpty))
}