package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.Documentation.Context
import com.github.propi.rdfrules.gui.Property.SummaryTitle
import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators.{GreaterThanOrEqualsTo, RegExp}
import com.github.propi.rdfrules.gui.utils.StringConverters._
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class PredictionToPredictionTasks(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = {
    Constants(
      new Select("confidence", "Confidence", Constants(
        "default" -> "Default",
        "cwa" -> "CWA",
        "pca" -> "PCA",
        "qpca" -> "QPCA"
      ), default = Some("default")),
      Group("generator", "Prediction tasks generator", summaryTitle = "generator") { implicit context =>
        def targetVariableSelect(implicit context: Context) = new Select("targetVariable", "Target variable position", Constants(
          "subject" -> "Subject",
          "object" -> "Object"
        ), nonEmpty = false)

        val patterns = new DynamicElement(Constants(
          DynamicGroup("patterns", "Patterns") { implicit context =>
            Constants(
              new OptionalText[String]("p", "Predicate", validator = RegExp("<.*>|.*:.*")),
              targetVariableSelect
            )
          }
        ), true)

        def activateCustomFields(_patterns: Boolean): Unit = {
          patterns.setElement(if (_patterns) 0 else -1)
        }

        Constants(
          new Select("type", "Type", Constants(
            "testAll" -> "All test prediction tasks",
            "testCardinalities" -> "Test prediction tasks with target variable at the lower cardinality side",
            "testPatterns" -> "Test prediction tasks by patterns",
            "predictionCardinalities" -> "Prediction tasks from predicted triples with target variable at the lower cardinality side",
            "predictionPatterns" -> "Prediction tasks from predicted triples by patterns",
          ), default = Some("testCardinalities"), (key, _) => key match {
            case "testPatterns" | "predictionPatterns" => activateCustomFields(true)
            case _ => activateCustomFields(false)
          }, SummaryTitle.NoTitle),
          patterns
        )
      },
      new OptionalText[Int]("limit", "Limit", validator = GreaterThanOrEqualsTo[Int](1), summaryTitle = "limit"),
      new OptionalText[Int]("topK", "Top-k candidates", validator = GreaterThanOrEqualsTo[Int](1), summaryTitle = "top-k")
    )
  }

  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}