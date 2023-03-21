package com.github.propi.rdfrules.gui.operations.actions

import com.github.propi.rdfrules.gui.properties.Hidden
import com.github.propi.rdfrules.gui.results.PredictedTriples
import com.github.propi.rdfrules.gui.{ActionProgress, Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

import scala.concurrent.Future

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class GetPrediction(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.GetPrediction

  private val showRules = new Hidden[Boolean]("showRules", "true")(_.toBoolean, x => x)

  val properties: Constants[Property] = Constants(showRules)
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))

  override def buildActionProgress(id: Future[String]): Option[ActionProgress] = Some(new PredictedTriples(info.title, id, showRules.getValue))
}