package com.github.propi.rdfrules.gui.operations.actions

import com.github.propi.rdfrules.gui.properties.Checkbox
import com.github.propi.rdfrules.gui.results.Rules
import com.github.propi.rdfrules.gui.{ActionProgress, Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

import scala.concurrent.Future

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class GetPrediction(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.GetPrediction
  val properties: Constants[Property] = Constants(
    new Checkbox("group", "Group by triples", true)
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))

  override def buildActionProgress(id: Future[String]): Option[ActionProgress] = Some(new Rules(info.title, id))
}