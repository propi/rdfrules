package com.github.propi.rdfrules.gui.properties

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
case class FixedText(name: String, title: String, default: String = "") extends Text(name, title, default)