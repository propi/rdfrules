package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Property
import com.thoughtworks.binding.Binding.{Constants, Var}
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.html.Div

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 17. 9. 2018.
  */
class DynamicElement(properties: Constants[Property], val description: String = "") extends Property {
  val name: String = properties.value.head.name
  val title: String = properties.value.head.title

  private val active: Var[Int] = Var(-1)

  for (i <- properties.value.indices) {
   properties.value(i).errorMsg.addListener((_: Option[String], newValue: Option[String]) => if (active.value == i) errorMsg.value = newValue)
  }

  def validate(): Option[String] = if (active.value >= 0) {
    properties.value(active.value).validate()
  } else {
    None
  }

  @dom
  def valueView: Binding[Div] = {
    val activeIndex = active.bind
    if (activeIndex >= 0) {
      <div>{properties.value(activeIndex).valueView.bind}</div>
    } else {
      <div></div>
    }
  }

  def setElement(x: Int): Unit = active.value = x

  def toJson: js.Any = if (active.value >= 0) properties.value(active.value).toJson else js.undefined
}
