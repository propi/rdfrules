package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Documentation.Context
import com.github.propi.rdfrules.gui.Property.SummaryTitle
import com.github.propi.rdfrules.gui.utils.Validate._

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
class FixedText[T](name: String, title: String, default: String = "", validator: Validator[String] = NoValidator[String](), summaryTitle: SummaryTitle = SummaryTitle.Empty)(implicit f: String => T, g: T => js.Any, context: Context) extends Text(name, title, default, validator, summaryTitle) {
  def toJson: js.Any = f(getText)
}