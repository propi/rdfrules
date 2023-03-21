package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Documentation.Context
import com.github.propi.rdfrules.gui.Property
import com.github.propi.rdfrules.gui.Property.SummaryTitle
import com.github.propi.rdfrules.gui.utils.ReactiveBinding.PimpedBindingSeq
import com.thoughtworks.binding.Binding
import com.thoughtworks.binding.Binding.{Constant, Var, Vars}
import org.lrng.binding.html
import org.scalajs.dom.Event
import org.scalajs.dom.html.{Div, Span}
import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
class ArrayElement private(_name: String, _title: String, val summaryTitle: SummaryTitle, property: Context => Property)(implicit context: Context) extends Property.FixedHidden {

  private val groups: Vars[Property] = Vars.empty

  val description: Var[String] = Var(context(_title).description)
  val title: Constant[String] = Constant(_title)
  val name: Constant[String] = Constant(_name)
  val isHidden: Constant[Boolean] = Constant(false)
  val errorMsg: Binding[Option[String]] = groups.findBinding(_.errorMsg.map(_.nonEmpty)).flatMap {
    case Some(property) => property.errorMsg.tuple(property.title).map(x => x._1.map(createErrorMsg(x._2, _)))
    case None => Constant(None)
  }

  private def createErrorMsg(title: String, msg: String) = s"There is an error within '$title' properties: $msg"

  def getName: String = name.value

  def getTitle: String = title.value

  def getErrorMsg: Option[String] = groups.value.iterator.map(_.getErrorMsg).find(_.nonEmpty).flatten.map(x => createErrorMsg(title.value, x))

  def getDescription: String = description.value

  override def hasSummary: Binding[Boolean] = Constant(summaryTitle.isEmpty).ifM(Constant(false), groups.existsBinding(_.hasSummary))

  def validate(): Option[String] = groups.value.iterator.map(_.validate()).find(_.nonEmpty).flatten.map(x => createErrorMsg(title.value, x))

  def setValue(data: js.Dynamic): Unit = {
    for (x <- data.asInstanceOf[js.Array[js.Dynamic]]) {
      val newProperty = property(context(title.value))
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
      val newProperty = property(context(title.value))
      groups.value += newProperty}>
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

  def apply(name: String, title: String, summaryTitle: SummaryTitle = SummaryTitle.Empty)(property: Context => Property)(implicit context: Context) = new ArrayElement(name, title, summaryTitle, property)

}