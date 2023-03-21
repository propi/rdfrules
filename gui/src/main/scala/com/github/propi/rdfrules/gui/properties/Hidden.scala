package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Property
import com.github.propi.rdfrules.gui.Property.SummaryTitle
import com.github.propi.rdfrules.gui.utils.ReactiveBinding
import com.thoughtworks.binding.Binding
import com.thoughtworks.binding.Binding.{Constant, Var}
import org.lrng.binding.html
import org.lrng.binding.html.NodeBinding
import org.scalajs.dom.html.{Div, Span}

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
class Hidden[T](_name: String, value: String)(implicit f: String => T, g: T => js.Any) extends Property.FixedProps with Property.FixedHidden {

  val summaryTitle: SummaryTitle = SummaryTitle.Empty

  def summaryContentView: Binding[Span] = ReactiveBinding.emptySpan

  private val _value: Var[String] = Var(value)

  val title: Constant[String] = Constant("")
  val name: Constant[String] = Constant(_name)
  val isHidden: Constant[Boolean] = Constant(true)
  val description: Var[String] = Var("")

  @html
  def valueView: NodeBinding[Div] = {
    <div>
      <input type="hidden" name={_name} value={_value.bind}/>
    </div>
  }

  def validate(): Option[String] = None

  def setValue(data: js.Dynamic): Unit = _value.value = data.toString

  def setValueString(value: String): Unit = _value.value = value

  def getValue: T = f(_value.value)

  def toJson: js.Any = f(_value.value)
}