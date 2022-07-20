package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Documentation.Context
import com.github.propi.rdfrules.gui.Property
import com.thoughtworks.binding.Binding
import com.thoughtworks.binding.Binding.{Constants, Var}
import org.lrng.binding.html
import org.lrng.binding.html.NodeBinding
import org.scalajs.dom.html.Div

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
class FixedArray private(val name: String, val title: String, propertiesBuilder: Context => Constants[Property])(implicit context: Context) extends Property {

  private val properties: Constants[Property] = propertiesBuilder(context(title))

  val descriptionVar: Binding.Var[String] = Var(context(title).description)

  def validate(): Option[String] = {
    val msg = properties.value.iterator.map(_.validate()).find(_.nonEmpty).flatten.map(x => s"There is an error within '$title' properties: $x")
    errorMsg.value = msg
    msg
  }

  def setValue(data: js.Dynamic): Unit = {
    for ((data, property) <- data.asInstanceOf[js.Array[js.Dynamic]].iterator.zip(properties.value)) {
      property.errorMsg.addListener((_: Option[String], _: Option[String]) => validate())
      property.setValue(data)
    }
  }

  def toJson: js.Any = js.Array(properties.value.flatMap { property =>
    val x = property.toJson
    if (js.isUndefined(x)) None else Some(x)
  }.toList: _*)

  @html
  def valueView: NodeBinding[Div] = {
    <div>
      {for (group <- properties) yield
      <div>
        {group.valueView.bind}
      </div>}
    </div>
  }

}

object FixedArray {

  def apply(name: String, title: String)(properties: Context => Constants[Property])(implicit context: Context) = new FixedArray(name, title, properties)

}
