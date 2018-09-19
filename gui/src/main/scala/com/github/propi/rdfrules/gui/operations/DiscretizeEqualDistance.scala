package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.StringConverters._
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

import scala.scalajs.js
import scala.scalajs.js.Dictionary

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class DiscretizeEqualDistance(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.DiscretizeEqualDistance
  val properties: Constants[Property] = Constants(
    new OptionalText[String]("subject", "Subject"),
    new OptionalText[String]("predicate", "Predicate"),
    new OptionalText[String]("object", "Object"),
    new OptionalText[String]("graph", "Graph"),
    new Checkbox("inverse", "Inverse"),
    new FixedText[Int]("bins", "Number of bins")
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))

  override protected def propertiesToJson: Dictionary[js.Any] = {
    val x = super.propertiesToJson
    val bins = x.remove("bins").get
    x += ("task" -> js.Dynamic.literal(name = "EquidistanceDiscretizationTask", bins = bins))
    x
  }
}