package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Documentation.Context
import com.github.propi.rdfrules.gui.Property
import com.github.propi.rdfrules.gui.Property.SummaryTitle
import com.github.propi.rdfrules.gui.utils.ReactiveBinding.PimpedBindingSeq
import com.thoughtworks.binding.Binding.{Constant, Constants, Var}
import com.thoughtworks.binding.Binding
import org.lrng.binding.html
import org.lrng.binding.html.NodeBinding
import org.scalajs.dom.Event
import org.scalajs.dom.html.{Div, Span}
import org.scalajs.dom.raw.HTMLSelectElement
import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
class Select(_name: String, _title: String, items: Constants[(String, String)], default: Option[String] = None, onSelect: (String, String) => Unit = (_, _) => {}, val summaryTitle: SummaryTitle = SummaryTitle.Empty, nonEmpty: Boolean = true)(implicit context: Context) extends Property.FixedProps {

  val title: Constant[String] = Constant(_title)
  val name: Constant[String] = Constant(_name)
  val description: Var[String] = Var(context(_title).description)
  val isHidden: Binding[Boolean] = Constant(false)

  private val selectedItem: Var[Option[String]] = Var(default)
  private val preparedItems: Constants[(String, String)] = if (default.isEmpty) {
    Constants((("" -> "") +: items.value).toList: _*)
  } else {
    items
  }
  private val selectedItemLabel = preparedItems.findBinding(x => selectedItem.map(_.contains(x._1))).map(_.map(_._2).getOrElse(""))

  for {
    default <- default
    (key, value) <- items.value.find(_._1 == default)
  } {
    onSelect(key, value)
  }

  override def hasSummary: Binding[Boolean] = Constant(summaryTitle.isEmpty).ifM(Constant(false), selectedItem.map(_.nonEmpty))

  def validate(): Option[String] = {
    val msg = if (nonEmpty && selectedItem.value.isEmpty) {
      Some(s"Select box '${title.value}' can not be empty.")
    } else {
      None
    }
    errorMsg.value = msg
    msg
  }

  def setValue(data: js.Dynamic): Unit = {
    val x = data.toString
    for ((key, value) <- items.value.find(_._1 == x)) {
      onSelect(key, value)
      selectedItem.value = Some(x)
    }
  }

  def toJson: js.Any = selectedItem.value match {
    case Some(x) => x
    case None => js.undefined
  }

  @html
  final def summaryContentView: Binding[Span] = <span class="ps-text">
    {selectedItemLabel.bind}
  </span>

  @html
  final def valueView: NodeBinding[Div] = {
    <div>
      <select onchange={e: Event =>
        val el = e.target.asInstanceOf[HTMLSelectElement]
        val key = el.value
        if (key.isEmpty) {
          onSelect(key, "")
          selectedItem.value = None
        } else {
          for ((_, value) <- items.value.find(_._1 == key)) {
            onSelect(key, value)
            selectedItem.value = Some(key)
          }
        }}>
        {for (item <- preparedItems) yield
        <option value={item._1} selected={selectedItem.bind.contains(item._1)}>
          {item._2}
        </option>}
      </select>
    </div>
  }

}