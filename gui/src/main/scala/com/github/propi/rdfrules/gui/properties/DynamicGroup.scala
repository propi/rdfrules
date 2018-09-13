package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Property
import com.thoughtworks.binding.Binding.{Constants, Vars}
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.Event
import org.scalajs.dom.html.Div

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
case class DynamicGroup(name: String, title: String, properties: () => Constants[Property]) extends Property {

  private val groups: Vars[Constants[Property]] = Vars.empty

  @dom
  protected def valueView: Binding[Div] = {
    <div>
      {for (group <- groups) yield
      <table>
        <tr>
          <th colSpan={2}>
            <a class="del" onclick={_: Event => groups.value -= group}>
              <i class="material-icons">remove_circle_outline</i>
            </a>
          </th>
        </tr>{for (property <- group) yield {
        property.view.bind
      }}
      </table>}<a class="add" onclick={_: Event => groups.value += properties()}>
      <i class="material-icons">add_circle_outline</i>
    </a>
    </div>
  }

}