package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}
import com.github.propi.rdfrules.gui.utils.StringConverters._

import scala.scalajs.js
import scala.scalajs.js.Dictionary

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class DiscretizeEqualSize(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.DiscretizeEqualSize
  val properties: Constants[Property] = Constants(
    new OptionalText[String]("subject", "Subject"),
    new OptionalText[String]("predicate", "Predicate"),
    new OptionalText[String]("object", "Object"),
    new OptionalText[String]("graph", "Graph"),
    new Checkbox("inverse", "Inverse"),
    new FixedText[Double]("support", "Min support")
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))

  override protected def propertiesToJson: Dictionary[js.Any] = {
    val x = super.propertiesToJson
    val support = x.remove("support").get
    x += ("task" -> js.Dynamic.literal(name = "EquisizeDiscretizationTask", support = support))
    x
  }
}