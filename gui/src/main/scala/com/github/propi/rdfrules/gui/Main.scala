package com.github.propi.rdfrules.gui

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
@JSExportTopLevel("RDFRules")
object Main {

  lazy val canvas = new Canvas

  @JSExport
  def loadTask(content: String): Unit = canvas.loadTask(content)

  def main(args: Array[String]): Unit = {
    canvas.render()
  }

}
