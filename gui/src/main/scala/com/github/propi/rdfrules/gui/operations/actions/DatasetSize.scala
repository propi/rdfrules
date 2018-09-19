package com.github.propi.rdfrules.gui.operations.actions

import com.github.propi.rdfrules.gui.results.Size
import com.github.propi.rdfrules.gui.{ActionProgress, Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

import scala.concurrent.Future

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class DatasetSize(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.DatasetSize
  val properties: Constants[Property] = Constants()
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))

  override def buildActionProgress(id: Future[String]): Option[ActionProgress] = Some(new Size(info.title, id))
}