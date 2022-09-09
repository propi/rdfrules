package com.github.propi.rdfrules.gui.operations.actions

import com.github.propi.rdfrules.gui._
import com.github.propi.rdfrules.gui.properties.{Checkbox, Hidden}
import com.github.propi.rdfrules.gui.results.EvaluationResult
import com.thoughtworks.binding.Binding.{Constants, Var}

import scala.concurrent.Future

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class Evaluate(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.Evaluate

  val properties: Constants[Property] = {
    Constants(
      new Checkbox("pca", "Partial Completeness Assumption (PCA)", true),
      new Hidden[Boolean]("injectiveMapping", "true")(_.toBoolean, x => x)
    )
  }
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))

  override def buildActionProgress(id: Future[String]): Option[ActionProgress] = Some(new EvaluationResult(info.title, id))
}