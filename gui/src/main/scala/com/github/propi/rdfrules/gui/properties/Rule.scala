package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Property
import com.github.propi.rdfrules.gui.results.Rules
import com.thoughtworks.binding.Binding.Var
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.html.Div

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
class Rule extends Property {

  private val rule: Var[Option[Rules.Rule]] = Var(None)

  val name: String = "rule"
  val title: String = "Rule"
  val descriptionVar: Var[String] = Var("")

  def setRule(rule: Rules.Rule): Unit = this.rule.value = Some(rule)

  @dom
  def valueView: Binding[Div] =
    <div class="rule">
      {rule.bind match {
      case Some(rule) =>
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
      case None => <div class="text"></div>
    }}
    </div>

  def validate(): Option[String] = {
    if (rule.value.isEmpty) {
      Some("No rule selected.")
    } else {
      None
    }
  }

  def setValue(data: js.Dynamic): Unit = {
    rule.value = Some(data.asInstanceOf[Rules.Rule])
  }

  def toJson: js.Any = rule.value.orNull
}