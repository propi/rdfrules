package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.utils.Validate.{NoValidator, Validator}

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.util.Success

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
class OptionalText[T](name: String,
                      title: String,
                      default: String = "",
                      description: String = "",
                      validator: Validator[String] = NoValidator[String]())
                     (implicit f: String => T, g: T => js.Any) extends Text(name, title, default, description, (x: String) => if (x.isEmpty) Success(x) else validator.validate(x)) {
  def toJson: js.Any = if (getText.isEmpty) js.undefined.asInstanceOf[UndefOr[T]] else f(getText)
}