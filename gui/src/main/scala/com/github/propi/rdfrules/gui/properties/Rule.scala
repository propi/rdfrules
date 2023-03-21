package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Property
import com.github.propi.rdfrules.gui.Property.SummaryTitle
import com.github.propi.rdfrules.gui.results.Rules
import com.github.propi.rdfrules.gui.utils.ReactiveBinding
import com.thoughtworks.binding.Binding
import com.thoughtworks.binding.Binding.{Constant, Var, Vars}
import org.lrng.binding.html
import org.lrng.binding.html.NodeBinding
import org.scalajs.dom.html.{Div, Span}

import scala.scalajs.js
import scala.scalajs.js.JSConverters.JSRichIterableOnce

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
class Rule extends Property.FixedProps {

  private val rules: Vars[Rules.Rule] = Vars.empty

  val title: Constant[String] = Constant("Rules")
  val name: Constant[String] = Constant("rules")
  val isHidden: Var[Boolean] = Var(true)
  val description: Var[String] = Var("")
  val summaryTitle: SummaryTitle = SummaryTitle.Empty

  def summaryContentView: Binding[Span] = ReactiveBinding.emptySpan

  @html
  def valueView: NodeBinding[Div] = <div class="rules-property">
    {for (rule <- rules) yield
      <div class="rule">
        <div class="text">
          <span>
            {rule.body.map(Rules.viewAtom).mkString(" ^ ")}
          </span>
          <span>
            &rArr;
          </span>
          <span>
            {Rules.viewAtom(rule.head)}
          </span>
        </div>
      </div>}
  </div>


  def validate(): Option[String] = None

  def setValue(data: js.Dynamic): Unit = {
    rules.value.clear()
    rules.value.addAll(data.asInstanceOf[js.Array[Rules.Rule]])
    if (rules.value.nonEmpty) {
      isHidden.value = false
    }
  }

  def getIsHidden: Boolean = isHidden.value

  def toJson: js.Any = rules.value.toJSArray
}