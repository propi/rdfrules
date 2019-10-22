package com.github.propi.rdfrules.gui.operations.actions

import com.github.propi.rdfrules.gui._
import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.results.EvaluationResult
import com.github.propi.rdfrules.gui.utils.CommonValidators.NonEmpty
import com.thoughtworks.binding.Binding.{Constants, Var}

import scala.concurrent.Future

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class Evaluate(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.Evaluate
  val properties: Constants[Property] = Constants(
    new ChooseFileFromWorkspace(Workspace.loadFiles, "path", description = "You can load a serialized model file from the workspace on the server side (just click onto a file name).", validator = NonEmpty),
    new Select("format", "Rules format", Constants("json" -> "JSON", "cache" -> "Model cache"), description = "Rules format."),
    new Checkbox("onlyFunctionalProperties", "Only functional properties", true, "Generate only functional properties. That means only one object can be predicted for pair (subject, predicate).")
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))

  override def buildActionProgress(id: Future[String]): Option[ActionProgress] = Some(new EvaluationResult(info.title, id))
}