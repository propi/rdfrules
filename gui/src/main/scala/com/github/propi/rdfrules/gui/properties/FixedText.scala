package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.utils.Validate._

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
class FixedText[T](name: String, title: String, default: String = "", description: String = "", validator: Validator[String] = NoValidator[String]())(implicit f: String => T, g: T => js.Any) extends Text(name, title, default, description, validator) {
  def toJson: js.Any = f(getText)
}