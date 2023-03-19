package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Property
import com.thoughtworks.binding.Binding.{Constants, Var}

import scala.language.reflectiveCalls

/**
  * Created by Vaclav Zeman on 17. 9. 2018.
  */
class DynamicElement(properties: Constants[Property], hidden: Boolean = false) extends DynamicElementBinding[Var[Int]](properties, Var(-1), hidden)(_.value, x => x) {
  def setElement(x: Int): Unit = activeBindings.value = x
}