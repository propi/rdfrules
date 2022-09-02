package com.github.propi.rdfrules.gui.operations.actions

import com.github.propi.rdfrules.gui._
import com.github.propi.rdfrules.gui.properties.{ArrayElement, FixedText}
import com.github.propi.rdfrules.gui.utils.CommonValidators.RegExp
import com.thoughtworks.binding.Binding.{Constants, Var}

import scala.concurrent.Future

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class PropertiesCardinalities(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.PropertiesCardinalities
  val properties: Constants[Property] = Constants(
    ArrayElement("filter", "Filter") { implicit context =>
      new FixedText[String]("property", "Property", validator = RegExp("<.*>|\\w+:.*"))
    }
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))

  override def buildActionProgress(id: Future[String]): Option[ActionProgress] = Some(new results.PropertiesCardinalities(info.title, id))
}