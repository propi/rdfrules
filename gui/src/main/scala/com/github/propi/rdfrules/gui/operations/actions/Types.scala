package com.github.propi.rdfrules.gui.operations.actions

import com.github.propi.rdfrules.gui._
import com.thoughtworks.binding.Binding.{Constants, Var}

import scala.concurrent.Future

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class Types(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.Types
  val properties: Constants[Property] = Constants()
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))

  override def buildActionProgress(id: Future[String]): Option[ActionProgress] = Some(new results.Types(info.name, id))
}