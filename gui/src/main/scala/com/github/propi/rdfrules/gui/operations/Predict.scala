package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui._
import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.results.PredictedResult
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class Predict(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = {
    val predictedResults = ArrayElement("predictedResults", "Predicted triple constraints", Property.SummaryTitle.NoTitle) { implicit context =>
      new Select("value", "Constraint", Constants(
        PredictedResult.Positive.toString -> PredictedResult.Positive.label,
        PredictedResult.Negative.toString -> PredictedResult.Negative.label,
        PredictedResult.PcaPositive.toString -> PredictedResult.PcaPositive.label
      ), summaryTitle = Property.SummaryTitle.NoTitle)
    }
    Constants(
      new Rule(),
      predictedResults,
      new Checkbox("injectiveMapping", "Injective mapping", true)
    )
  }
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}