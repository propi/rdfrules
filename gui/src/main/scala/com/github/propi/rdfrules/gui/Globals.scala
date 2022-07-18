package com.github.propi.rdfrules.gui

import org.scalajs.dom.Blob

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobalScope

/**
  * Created by Vaclav Zeman on 12. 9. 2018.
  */
@js.native
@JSGlobalScope
object Globals extends js.Object {
  var endpoint: String = js.native

  def stripText(x: String): String = js.native

  def getParameterByName(name: String, url: String = js.native): String = js.native

  def saveAs(blob: Blob, filename: String): Unit = js.native

  def escapeHTML(text: String): String = js.native
}