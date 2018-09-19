package com.github.propi.rdfrules.gui.operations.actions

import com.github.propi.rdfrules.gui.results.Quads
import com.github.propi.rdfrules.gui.{ActionProgress, Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

import scala.concurrent.Future

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class GetQuads(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.GetQuads
  val properties: Constants[Property] = Constants()
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))

  override def buildActionProgress(id: Future[String]): Option[ActionProgress] = Some(new Quads(info.title, id))
}