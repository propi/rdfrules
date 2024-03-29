package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Documentation.Context
import com.github.propi.rdfrules.gui.Property
import com.github.propi.rdfrules.gui.Property.SummaryTitle
import com.github.propi.rdfrules.gui.utils.ReactiveBinding
import com.thoughtworks.binding.Binding.{Constant, Var}
import com.thoughtworks.binding.Binding
import org.lrng.binding.html
import org.lrng.binding.html.NodeBinding
import org.scalajs.dom.Event
import org.scalajs.dom.html.{Div, Span}
import org.scalajs.dom.raw.HTMLInputElement
import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
class Checkbox(_name: String, _title: String, default: Boolean = false, onChecked: Boolean => Unit = _ => {}, val summaryTitle: SummaryTitle = SummaryTitle.Empty)(implicit context: Context) extends Property.FixedProps with Property.FixedHidden {

  private val _isChecked: Var[Boolean] = Var(default)

  val description: Var[String] = Var(context(_title).description)
  val title: Constant[String] = Constant(_title)
  val name: Constant[String] = Constant(_name)
  val isHidden: Constant[Boolean] = Constant(false)

  def setValue(data: Boolean): Unit = {
    _isChecked.value = data
    onChecked(data)
  }

  def setValue(data: js.Dynamic): Unit = setValue(data.asInstanceOf[Boolean])

  def isChecked: Boolean = _isChecked.value

  def validate(): Option[String] = None

  def toJson: js.Any = isChecked

  override def hasSummary: Binding[Boolean] = Constant(summaryTitle.isEmpty).ifM(Constant(false), _isChecked)

  @html
  override def summaryView: Binding[Span] = <span class="property-summary">
    <span class="ps-title novalue">
      {summaryTitle match {
      case x: SummaryTitle.Fixed => x.title
      case SummaryTitle.Variable(title) => title.bind
    }}
    </span>
  </span>

  final def summaryContentView: Binding[Span] = ReactiveBinding.emptySpan

  @html
  final def valueView: NodeBinding[Div] = {
    <div>
      <input type="checkbox" class="checkbox" checked={_isChecked.bind} onchange={e: Event =>
      _isChecked.value = e.target.asInstanceOf[HTMLInputElement].checked
      onChecked(_isChecked.value)}/>
    </div>
  }

}