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
class Hidden[T](_name: String, value: String)(implicit f: String => T, g: T => js.Any) extends Property.FixedProps {

  val summaryTitle: SummaryTitle = SummaryTitle.Empty

  def summaryContentView: Binding[Span] = ReactiveBinding.emptySpan

  private var _value: String = value

  val title: Constant[String] = Constant("")
  val name: Constant[String] = Constant(_name)
  val isHidden: Binding[Boolean] = Constant(true)
  val description: Var[String] = Var("")

  @html
  def valueView: NodeBinding[Div] = {
    <div>
      <input type="hidden" name={_name} value={_value}/>
    </div>
  }

  def validate(): Option[String] = None

  def setValue(data: js.Dynamic): Unit = {
    _value = data.toString
  }

  def getValue: T = f(_value)

  def toJson: js.Any = f(_value)
}