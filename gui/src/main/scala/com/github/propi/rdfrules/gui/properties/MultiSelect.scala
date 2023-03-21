package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Documentation.Context
import com.github.propi.rdfrules.gui.Property
import com.github.propi.rdfrules.gui.Property.SummaryTitle
import com.github.propi.rdfrules.gui.components.Multiselect
import com.github.propi.rdfrules.gui.utils.ReactiveBinding.PimpedBindingSeq
import com.thoughtworks.binding.Binding
import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._
import com.thoughtworks.binding.Binding.{Constant, Constants, Var}
import org.lrng.binding.html
import org.lrng.binding.html.NodeBinding
import org.scalajs.dom.html.{Div, Span}

import scala.scalajs.js
import scala.scalajs.js.JSConverters.JSRichIterableOnce

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
class MultiSelect(_name: String, _title: String, items: Constants[(String, String)], default: Seq[String] = Nil, placeholder: String = "", onchange: Seq[(String, String)] => Unit = _ => (), val summaryTitle: SummaryTitle = SummaryTitle.Empty)(implicit context: Context) extends Property.FixedProps with Property.FixedHidden {

  val title: Constant[String] = Constant(_title)
  val name: Constant[String] = Constant(_name)
  val isHidden: Constant[Boolean] = Constant(false)
  val description: Var[String] = Var(context(_title).description)

  private val multiselect = new Multiselect(items, default.toSet, placeholder, onchange)
  private val selectedItemsLabel = items.withFilterBinding(x => multiselect.selectedValuesBinding.find(_ == x._1).map(_.nonEmpty)).map(_._2).all.map(_.mkString(", "))

  override def hasSummary: Binding[Boolean] = Constant(summaryTitle.isEmpty).ifM(Constant(false), multiselect.selectedValuesBinding.nonEmpty)

  def validate(): Option[String] = None

  def setValue(data: js.Dynamic): Unit = {
    if (js.Array.isArray(data)) {
      multiselect.replace(data.asInstanceOf[js.Array[String]].iterator)
    }
  }

  def toJson: js.Any = multiselect.selectedValues.toJSArray

  @html
  final def summaryContentView: Binding[Span] = <span class="ps-text">
    {selectedItemsLabel.bind}
  </span>

  @html
  final def valueView: NodeBinding[Div] = {
    <div>
      {multiselect.view.bind}
    </div>
  }

}