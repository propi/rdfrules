package com.github.propi.rdfrules.gui.operations.actions

import com.github.propi.rdfrules.gui.properties.FixedText
import com.github.propi.rdfrules.gui.results.PredictionTasks
import com.github.propi.rdfrules.gui.utils.CommonValidators.GreaterThanOrEqualsTo
import com.github.propi.rdfrules.gui.{ActionProgress, Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}
import com.github.propi.rdfrules.gui.utils.StringConverters._

import scala.concurrent.Future

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class GetPredictionTasks(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.GetPredictionTasks
  val properties: Constants[Property] = Constants(
    new FixedText[Int]("maxCandidates", "Max candidates", "10", GreaterThanOrEqualsTo[Int](0), "max candidates")
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))

  override def buildActionProgress(id: Future[String]): Option[ActionProgress] = Some(new PredictionTasks(info.title, id))
}