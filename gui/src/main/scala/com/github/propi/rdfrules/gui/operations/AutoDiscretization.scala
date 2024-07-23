package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators._
import com.github.propi.rdfrules.gui.utils.StringConverters._
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class AutoDiscretization(fromOperation: Operation, val info: OperationInfo) extends Operation {

  val properties: Constants[Property] = {
    Constants(
      new Checkbox("minSupportLowerBoundOn", "Min support lower bound", true),
      new Checkbox("minSupportUpperBoundOn", "Min support upper bound", true),
      new FixedText[Int]("minHeadSize", "Min head size", "100", GreaterThanOrEqualsTo[Int](1)),
      new FixedText[Double]("minHeadCoverage", "Min head coverage", "0.01", GreaterThanOrEqualsTo[Double](0.0)),
      new FixedText[Int]("maxRuleLength", "Max rule length", "3", GreaterThanOrEqualsTo[Int](1)),
      ArrayElement("predicates", "Predicates") { implicit context =>
        new FixedText[String]("predicate", "Predicate", validator = RegExp("<.*>|\\w+:.*"))
      }
    )
  }

  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}