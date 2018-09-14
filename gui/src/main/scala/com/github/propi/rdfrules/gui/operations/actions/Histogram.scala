package com.github.propi.rdfrules.gui.operations.actions

import com.github.propi.rdfrules.gui.properties.Checkbox
import com.github.propi.rdfrules.gui._
import com.thoughtworks.binding.Binding.{Constants, Var}

import scala.concurrent.Future

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class Histogram(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.Histogram
  val properties: Constants[Property] = Constants(
    Checkbox("subject", "Subject"),
    Checkbox("predicate", "Predicate"),
    Checkbox("object", "Object")
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))

  override def buildActionProgress(id: Future[String]): Option[ActionProgress] = Some(new results.Histogram(info.name, id))
}