package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Documentation.Context
import com.github.propi.rdfrules.gui.Property
import com.github.propi.rdfrules.gui.Property.SummaryTitle
import com.github.propi.rdfrules.gui.utils.ReactiveBinding.PimpedBindingSeq
import com.thoughtworks.binding.Binding
import com.thoughtworks.binding.Binding.{Constant, Constants, Var}
import org.lrng.binding.html
import org.lrng.binding.html.NodeBinding
import org.scalajs.dom.html.{Div, Span}
import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
class Group private(_name: String, _title: String, val summaryTitle: SummaryTitle, propertiesBuilder: Context => Constants[Property])(implicit context: Context) extends Property.FixedProps with Property.FixedHidden {

  private val properties = propertiesBuilder(context(_title))

  val title: Constant[String] = Constant(_title)
  val name: Constant[String] = Constant(_name)
  val isHidden: Constant[Boolean] = Constant(false)
  val description: Var[String] = Var(context(_title).description)

  override def hasSummary: Binding[Boolean] = Constant(summaryTitle.isEmpty).ifM(Constant(false), properties.existsBinding(_.hasSummary))

  def validate(): Option[String] = properties.value.iterator.map(_.validate()).find(_.nonEmpty).flatten.map(x => s"There is an error within '${title.value}' properties: $x")

  def setValue(data: js.Dynamic): Unit = {
    for (prop <- properties.value) {
      val propData = data.selectDynamic(prop.getName)
      if (!js.isUndefined(propData)) prop.setValue(propData)
    }
  }

  def toJson: js.Any = js.Dictionary(properties.value.map(x => x.getName -> x.toJson).filter(x => !js.isUndefined(x._2)).toList: _*)

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

  @html
  def summaryContentView: Binding[Span] = <span class="group">
    {for (property <- properties if property.hasSummary.bind) yield property.summaryView.bind}
  </span>
}

object Group {

  def apply(name: String, title: String, summaryTitle: SummaryTitle = SummaryTitle.Empty)(propertiesBuilder: Context => Constants[Property])(implicit context: Context): Group = new Group(name, title, summaryTitle, propertiesBuilder)

}