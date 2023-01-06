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
class FilterPrediction(fromOperation: Operation, val info: OperationInfo) extends Operation {
  //val info: OperationInfo = OperationInfo.FilterRules
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
    new Select("completionStrategy", "Completion strategy", Constants(
      "distinctPredictions" -> "Distinct predictions",
      "functionalPredictions" -> "Only functional predictions",
      "pcaPredictions" -> "Only PCA predictions",
      "qpcaPredictions" -> "Only QPCA predictions"
    ), summaryTitle = "completion"),
    DynamicGroup("tripleMatchers", "Triple filter", SummaryTitle.NoTitle) { implicit context =>
      Constants(
        new OptionalText[String]("subject", "Subject", validator = RegExp("<.*>|.*:.*", true), summaryTitle = "subject"),
        new OptionalText[String]("predicate", "Predicate", validator = RegExp("<.*>|.*:.*", true), summaryTitle = "predicate"),
        new OptionalText[String]("object", "Object", summaryTitle = "object"),
        new Checkbox("inverse", "Negation", summaryTitle = "negated")
      )
    },
    Pattern("patterns", "Patterns", true),
    DynamicGroup("measures", "Measures", SummaryTitle.NoTitle) { implicit context =>
      val summaryTitle = Var("")
      Constants(
        new Select("name", "Name", Constants(
          "RuleLength" -> "Rule length",
          "HeadSize" -> "Head size",
          "Support" -> "Support",
          "HeadCoverage" -> "Head coverage",
          "BodySize" -> "Body size",
          "Confidence" -> "Confidence",
          "PcaConfidence" -> "PCA confidence",
          "PcaBodySize" -> "PCA body size",
          "QpcaConfidence" -> "QPCA confidence",
          "QpcaBodySize" -> "QPCA body size",
          "Lift" -> "Lift"
        ), onSelect = (_, value) => summaryTitle.value = value),
        new FixedText[String]("value", "Value", validator = RegExp("\\d+(\\.\\d+)?|[><]=? \\d+(\\.\\d+)?|[\\[\\(]\\d+(\\.\\d+)?;\\d+(\\.\\d+)?[\\]\\)]"), summaryTitle = SummaryTitle.Variable(summaryTitle))
      )
    }
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}