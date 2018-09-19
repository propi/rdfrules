package com.github.propi.rdfrules.gui.properties

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
class OptionalText[T](name: String, title: String, default: String = "")(implicit f: String => T, g: T => js.Any) extends Text(name, title, default) {
  def toJson: js.Any = if (getText.isEmpty) UndefOr.undefOr2jsAny(js.undefined)(x => x) else f(getText)
}