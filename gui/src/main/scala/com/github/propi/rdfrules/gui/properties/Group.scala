package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Property
import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.binding.Binding.Constants
import org.scalajs.dom.html.Div

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
case class Group(name: String, title: String, properties: Constants[Property]) extends Property {

  def toJson: js.Any = js.Dictionary(properties.value.map(x => x.name -> x.toJson).filter(x => !js.isUndefined(x._2)): _*)

  @dom
  protected def valueView: Binding[Div] = {
    <div class="properties sub">
      <table>
        {for (property <- properties) yield {
        property.view.bind
      }}
      </table>
    </div>
  }

}