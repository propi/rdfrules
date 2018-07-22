package com.github.propi.rdfrules.gui

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
object Main {

  lazy val canvas = new Canvas

  def main(args: Array[String]): Unit = {
    canvas.render()
  }

}
