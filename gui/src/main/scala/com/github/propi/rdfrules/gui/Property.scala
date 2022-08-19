package com.github.propi.rdfrules.gui

import com.github.propi.rdfrules.gui.Property.SummaryTitle
import com.github.propi.rdfrules.gui.utils.ReactiveBinding
import com.thoughtworks.binding.Binding
import com.thoughtworks.binding.Binding.{Constant, Var}
import org.lrng.binding.html
import org.scalajs.dom.html.{Div, Span, TableRow}
import org.scalajs.dom.{Event, MouseEvent}

import scala.language.implicitConversions
import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
trait Property {
  val name: String
  val title: String
  val descriptionVar: Var[String]
  val summaryTitle: SummaryTitle

  val errorMsg: ReactiveBinding.Var[Option[String]] = ReactiveBinding.Var(None)
  val isHidden: Var[Boolean] = Var(false)

  def hasSummary: Binding[Boolean] = Constant(summaryTitle.nonEmpty)

  def summaryContentView: Binding[Span]

  def valueView: Binding[Div]

  def isValid: Boolean = errorMsg.value.isEmpty

  def validate(): Option[String]

  def setValue(data: js.Dynamic): Unit

  @html
  def summaryView: Binding[Span] = <span class="property-summary">
    {if (summaryTitle == SummaryTitle.NoTitle) {
      <!-- empty content -->
    } else {
      <span class="ps-title">
        {summaryTitle match {
        case x: SummaryTitle.Fixed => s"${x.title}:"
        case SummaryTitle.Variable(title) => s"${title.bind}:"
      }}
      </span>
    }}<span class="ps-content">
      {summaryContentView.bind}
    </span>
  </span>

  @html
  def view: Binding[TableRow] = {
    <tr class={if (isHidden.bind) "hidden" else ""}>
      <th>
        <div class="title">
          <div class="hints">
            <div class={"error" + (if (errorMsg.binding.bind.isEmpty) " hidden" else "")} onmousemove={e: MouseEvent => Main.canvas.openHint(errorMsg.value.getOrElse(""), e)} onmouseout={_: MouseEvent => Main.canvas.closeHint()} onclick={_: Event => Main.canvas.fixHint()}>
              <i class="material-icons">error</i>
            </div>
            <div class={"description" + (if (descriptionVar.bind.isEmpty) " hidden" else "")} onmousemove={e: MouseEvent => Main.canvas.openHint(descriptionVar.value, e)} onmouseout={_: MouseEvent => Main.canvas.closeHint()} onclick={_: Event => Main.canvas.fixHint()}>
              <i class="material-icons">help</i>
            </div>
          </div>
          <div class="text">
            {title}
          </div>
        </div>
      </th>
      <td>
        {valueView.bind}
      </td>
    </tr>
  }

  def toJson: js.Any
}

object Property {

  sealed trait SummaryTitle {
    def isEmpty: Boolean = false

    final def nonEmpty: Boolean = !isEmpty
  }

  object SummaryTitle {
    implicit def apply(x: String): SummaryTitle = Value(x)

    sealed trait Fixed extends SummaryTitle {
      def title: String
    }

    case object Empty extends SummaryTitle with Fixed {
      override def isEmpty: Boolean = true

      def title: String = ""
    }

    case object NoTitle extends SummaryTitle with Fixed {
      def title: String = ""
    }

    case class Value(title: String) extends SummaryTitle with Fixed

    case class Variable(title: Var[String]) extends SummaryTitle
  }

}