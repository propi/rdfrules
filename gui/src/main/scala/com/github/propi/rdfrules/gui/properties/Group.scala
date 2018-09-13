package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Property
import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.binding.Binding.Constants
import org.scalajs.dom.html.Div

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
case class Group(name: String, title: String, properties: Constants[Property]) extends Property {

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