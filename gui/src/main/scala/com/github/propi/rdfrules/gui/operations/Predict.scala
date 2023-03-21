package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui._
import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.results.PredictedResult
import com.github.propi.rdfrules.gui.utils.CommonValidators.RegExp
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class Predict(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = {
    val validator = RegExp(".+[.](ttl|nt|nq|json|jsonld|xml|rdf|owl|trig|trix|tsv|sql|cache)([.](gz|bz2))?$")
    val fileChooser = new DynamicElement(Constants(new ChooseFileFromWorkspace(Workspace.loadFiles, false, "testPath", validator = validator, summaryTitle = "test set")), true)
    Constants(
      new Rule(),
      new Checkbox("chooseTestSet", "Choose test set", onChecked = isChecked => fileChooser.setElement(if (isChecked) 0 else -1)),
      fileChooser,
      new MultiSelect(
        "predictedResults",
        "Predicted triple constraints",
        Constants(
          PredictedResult.Positive.toString -> PredictedResult.Positive.label,
          PredictedResult.Negative.toString -> PredictedResult.Negative.label,
          PredictedResult.PcaPositive.toString -> PredictedResult.PcaPositive.label
        ),
        summaryTitle = Property.SummaryTitle.NoTitle),
      new Checkbox("injectiveMapping", "Injective mapping", true),
      new Hidden[Boolean]("mergeTestAndTrainForPrediction", "true")(_.toBoolean, x => x),
      new Hidden[Boolean]("onlyTestCoveredPredictions", "false")(_.toBoolean, x => x),
    )
  }
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}