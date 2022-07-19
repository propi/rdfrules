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
class Group private(val name: String, val title: String, propertiesBuilder: Context => Constants[Property])(implicit context: Context) extends Property {

  private val properties = propertiesBuilder(context(title))

  val descriptionVar: Binding.Var[String] = Var(context(title).description)

  def validate(): Option[String] = properties.value.iterator.map(_.validate()).find(_.nonEmpty).flatten.map(x => s"There is an error within '$title' properties: $x")

  def setValue(data: js.Dynamic): Unit = {
    for (prop <- properties.value) {
      val propData = data.selectDynamic(prop.name)
      if (!js.isUndefined(propData)) prop.setValue(propData)
    }
  }

  def toJson: js.Any = js.Dictionary(properties.value.map(x => x.name -> x.toJson).filter(x => !js.isUndefined(x._2)).toList: _*)

  @html
  def valueView: NodeBinding[Div] = {
    <div class="properties sub">
      <table>
        {for (property <- properties) yield {
        property.view.bind
      }}
      </table>
    </div>
  }

}

object Group {

  def apply(name: String, title: String)(propertiesBuilder: Context => Constants[Property])(implicit context: Context): Group = new Group(name, title, propertiesBuilder)

}