package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Documentation.Context
import com.github.propi.rdfrules.gui.Property
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
class Select(val name: String, val title: String, items: Constants[(String, String)], default: Option[String] = None, onSelect: String => Unit = _ => {}, val summaryTitle: String = "")(implicit context: Context) extends Property {

  val descriptionVar: Binding.Var[String] = Var(context(title).description)

  private val selectedItem: Var[Option[String]] = Var(default)
  private val preparedItems: Constants[(String, String)] = if (default.isEmpty) {
    Constants((("" -> "") +: items.value).toList: _*)
  } else {
    items
  }
  private val selectedItemLabel = preparedItems.findBinding(x => selectedItem.map(_.contains(x._1))).map(_.map(_._2).getOrElse(""))

  default.foreach(onSelect)

  override def hasSummary: Binding[Boolean] = Constant(summaryTitle.isEmpty).ifM(Constant(false), selectedItem.map(_.nonEmpty))

  def validate(): Option[String] = None

  def setValue(data: js.Dynamic): Unit = {
    val x = data.toString
    onSelect(x)
    selectedItem.value = Some(x)
  }

  def toJson: js.Any = selectedItem.value match {
    case Some(x) => x
    case None => js.undefined
  }

  @html
  final def summaryContentView: Binding[Span] = <span>
    {selectedItemLabel.bind}
  </span>

  @html
  final def valueView: NodeBinding[Div] = {
    <div>
      <select onchange={e: Event =>
        val x = e.target.asInstanceOf[HTMLSelectElement].value
        onSelect(x)
        selectedItem.value = if (x.isEmpty) None else Some(x)}>
        {for (item <- preparedItems) yield
        <option value={item._1} selected={selectedItem.bind.contains(item._1)}>
          {item._2}
        </option>}
      </select>
    </div>
  }

}