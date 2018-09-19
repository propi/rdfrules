package com.github.propi.rdfrules.gui.operations.actions

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.results.NoResult
import com.github.propi.rdfrules.gui.{ActionProgress, Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

import scala.concurrent.Future

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class CacheDataset(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.CacheDatasetAction
  val properties: Constants[Property] = Constants(
    new FixedText[String]("path", "Path")
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))

  override def buildActionProgress(id: Future[String]): Option[ActionProgress] = Some(new NoResult(info.title, id))
}