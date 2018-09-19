package com.github.propi.rdfrules.gui.properties

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
class FixedText[T](name: String, title: String, default: String = "")(implicit f: String => T, g: T => js.Any) extends Text(name, title, default) {
  def toJson: js.Any = f(getText)
}