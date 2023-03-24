package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.Property.SummaryTitle
import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.results.PredictedResult
import com.github.propi.rdfrules.gui.utils.CommonValidators.RegExp
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class FilterPredictionTasks(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = Constants(
    new MultiSelect(
      "predictedResults",
      "Predicted triple constraints",
      Constants(
        PredictedResult.Positive.toString -> PredictedResult.Positive.label,
        PredictedResult.Negative.toString -> PredictedResult.Negative.label,
        PredictedResult.PcaPositive.toString -> PredictedResult.PcaPositive.label
      ),
      summaryTitle = Property.SummaryTitle.NoTitle),
    DynamicGroup("tripleMatchers", "Triple filter", SummaryTitle.NoTitle) { implicit context =>
      Constants(
        new OptionalText[String]("subject", "Subject", validator = RegExp("<.*>|.*:.*", true), summaryTitle = "subject"),
        new OptionalText[String]("predicate", "Predicate", validator = RegExp("<.*>|.*:.*", true), summaryTitle = "predicate"),
        new OptionalText[String]("object", "Object", summaryTitle = "object")
      )
    },
    new Checkbox("nonEmptyPredictions", "Non empty candidates", summaryTitle = "non empty")
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}