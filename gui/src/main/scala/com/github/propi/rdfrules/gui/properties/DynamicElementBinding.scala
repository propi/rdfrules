package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Property
import com.github.propi.rdfrules.gui.Property.SummaryTitle
import com.github.propi.rdfrules.gui.utils.ReactiveBinding
import com.thoughtworks.binding.Binding
import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._
import com.thoughtworks.binding.Binding.{Constant, Constants}
import org.lrng.binding.html
import org.scalajs.dom.html.{Div, Span, TableRow}

import scala.language.reflectiveCalls
import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 17. 9. 2018.
  */
class DynamicElementBinding[T](properties: Constants[Property], protected val activeBindings: T, hidden: Boolean = false)(fv: T => Int, fb: T => Binding[Int]) extends Property {
  val active: Binding[Int] = fb(activeBindings)

  private val selectedElement = active.map(i => if (i < 0) None else Some(properties.value(i)))

  val name: Binding[String] = selectedElement.flatMap {
    case Some(el) => el.name
    case None => Constant("")
  }
  val title: Binding[String] = selectedElement.flatMap {
    case Some(el) => el.title
    case None => Constant("")
  }
  val summaryTitle: SummaryTitle = SummaryTitle.Empty
  val description: Binding[String] = selectedElement.flatMap {
    case Some(el) => el.description
    case None => Constant("")
  }
  val errorMsg: Binding[Option[String]] = selectedElement.flatMap {
    case Some(el) => el.errorMsg
    case None => Constant(None)
  }
  val isHidden: Binding[Boolean] = selectedElement.map {
    case None => if (hidden) true else false
    case _ => false
  }

  def getActive: Int = fv(activeBindings)

  def getName: String = {
    val _active = getActive
    if (_active < 0) "" else properties.value(_active).getName
  }

  def getTitle: String = {
    val _active = getActive
    if (_active < 0) "" else properties.value(_active).getTitle
  }

  def getErrorMsg: Option[String] = {
    val _active = getActive
    if (_active < 0) None else properties.value(_active).getErrorMsg
  }

  def getDescription: String = {
    val _active = getActive
    if (_active < 0) "" else properties.value(_active).getDescription
  }

  def getIsHidden: Boolean = {
    val _active = getActive
    if (_active < 0) true else properties.value(_active).getIsHidden
  }

  def validate(): Option[String] = {
    val _active = getActive
    if (_active >= 0) {
      properties.value(_active).validate()
    } else {
      None
    }
  }

  def setValue(data: js.Dynamic): Unit = {
    val _active = getActive
    if (_active >= 0) {
      properties.value(_active).setValue(data)
    }
  }

  override def hasSummary: Binding[Boolean] = active.flatMap(x => if (x >= 0) properties.value(x).hasSummary else Constant(false))

  def summaryContentView: Binding[Span] = active.flatMap(x => if (x >= 0) properties.value(x).summaryView else ReactiveBinding.emptySpan)

  override def summaryView: Binding[Span] = summaryContentView

  @html
  def valueView: Binding[Div] = <div>
    {val activeIndex = active.bind
    if (activeIndex >= 0) {
      properties.value(activeIndex).valueView.bind
    } else {
      ReactiveBinding.empty.bind
    }}
  </div>

  override def view: Binding[TableRow] = super.view

  def toJson: js.Any = {
    val _active = getActive
    if (_active >= 0) properties.value(_active).toJson else js.undefined
  }
}