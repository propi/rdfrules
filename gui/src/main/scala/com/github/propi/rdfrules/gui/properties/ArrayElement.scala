package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Documentation.Context
import com.github.propi.rdfrules.gui.Property
import com.thoughtworks.binding.Binding
import com.thoughtworks.binding.Binding.{Var, Vars}
import org.lrng.binding.html
import org.scalajs.dom.Event
import org.scalajs.dom.html.{Div, Span}

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
class ArrayElement private(val name: String, val title: String, property: Context => Property, val summaryTitle: String = "")(implicit context: Context) extends Property {

  private val groups: Vars[Property] = Vars.empty

  val descriptionVar: Binding.Var[String] = Var(context(title).description)

  override def hasSummary: Binding[Boolean] = Binding.BindingInstances.map(groups.nonEmpty)(_ && summaryTitle.nonEmpty)

  def validate(): Option[String] = {
    val msg = groups.value.iterator.map(_.validate()).find(_.nonEmpty).flatten.map(x => s"There is an error within '$title' properties: $x")
    errorMsg.value = msg
    msg
  }

  def setValue(data: js.Dynamic): Unit = {
    for (x <- data.asInstanceOf[js.Array[js.Dynamic]]) {
      val newProperty = property(context(title))
      newProperty.errorMsg.addListener((_: Option[String], _: Option[String]) => validate())
      newProperty.setValue(x)
      groups.value += newProperty
    }
  }

  def toJson: js.Any = js.Array(groups.value.flatMap { property =>
    val x = property.toJson
    if (js.isUndefined(x)) None else Some(x)
  }.toList: _*)

  @html
  def valueView: Binding[Div] = {
    <div>
      {for (group <- groups) yield
      <div>
        {group.valueView.bind}<a class="del" onclick={_: Event =>
        groups.value -= group
        validate()}>
        <i class="material-icons">remove_circle_outline</i>
      </a>
      </div>}<a class="add" onclick={_: Event =>
      val newProperty = property(context(title))
      groups.value += newProperty
      newProperty.errorMsg.addListener((_: Option[String], _: Option[String]) => validate())}>
      <i class="material-icons">add_circle_outline</i>
    </a>
    </div>
  }

  @html
  final def summaryContentView: Binding[Span] = <span>
    {for (group <- groups if group.hasSummary.bind) yield group.summaryView.bind}
  </span>

}

object ArrayElement {

  def apply(name: String, title: String)(property: Context => Property)(implicit context: Context) = new ArrayElement(name, title, property)

}