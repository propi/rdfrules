package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Property
import com.thoughtworks.binding.Binding.{Constants, Var}
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.html.{Div, TableRow}

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 17. 9. 2018.
  */
class DynamicElement(properties: Constants[Property], description: String = "", hidden: Boolean = false) extends Property {
  val name: String = properties.value.head.name
  val title: String = properties.value.head.title

  val descriptionVar: Var[String] = Var(description)

  private val active: Var[Int] = Var(-1)

  if (hidden) isHidden.value = true

  for (i <- properties.value.indices) {
    properties.value(i).errorMsg.addListener((_: Option[String], newValue: Option[String]) => if (active.value == i) errorMsg.value = newValue)
  }

  def validate(): Option[String] = if (active.value >= 0) {
    properties.value(active.value).validate()
  } else {
    None
  }

  def setValue(data: js.Dynamic): Unit = {
    if (active.value >= 0) {
      properties.value(active.value).setValue(data)
    }
  }

  @dom
  def valueView: Binding[Div] = {
    val activeIndex = active.bind
    if (activeIndex >= 0) {
      <div>
        {properties.value(activeIndex).valueView.bind}
      </div>
    } else {
      <div></div>
    }
  }


  override def view: Binding[TableRow] = super.view

  def setElement(x: Int): Unit = {
    active.value = x
    if (x < 0) {
      if (hidden) {
        isHidden.value = true
      }
      descriptionVar.value = description
      errorMsg.value = None
    } else {
      val el = properties.value(x)
      descriptionVar.value = el.descriptionVar.value
      errorMsg.value = el.errorMsg.value
      if (hidden) {
        isHidden.value = false
      }
    }
  }

  def toJson: js.Any = if (active.value >= 0) properties.value(active.value).toJson else js.undefined
}
