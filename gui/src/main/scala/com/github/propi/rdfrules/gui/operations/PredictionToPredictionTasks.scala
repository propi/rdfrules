package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.Documentation.Context
import com.github.propi.rdfrules.gui.properties.{DynamicElement, DynamicGroup, FixedText, Group, OptionalText, Select}
import com.github.propi.rdfrules.gui.utils.CommonValidators.{GreaterThanOrEqualsTo, RegExp}
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}
import com.github.propi.rdfrules.gui.utils.StringConverters._

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
      Group("generator", "Prediction tasks generator") { implicit context =>
        def targetVariableSelect(implicit context: Context) = new Select("targetVariable", "Target variable position", Constants(
          "subject" -> "Subject",
          "object" -> "Object"
        ), default = Some("object"))

        val patterns = new DynamicElement(Constants(
          DynamicGroup("patterns", "Patterns") { implicit context =>
            Constants(
              new FixedText[String]("p", "Predicate", validator = RegExp("<.*>|.*:.*")),
              targetVariableSelect
            )
          }
        ), true)
        val targetVariable = new DynamicElement(Constants(
          targetVariableSelect
        ), true)

        def activateCustomFields(_patterns: Boolean, _targetVariable: Boolean): Unit = {
          patterns.setElement(if (_patterns) 0 else -1)
          targetVariable.setElement(if (_targetVariable) 0 else -1)
        }

        Constants(
          new Select("type", "Type", Constants(
            "testAll" -> "All test prediction tasks",
            "testCardinalities" -> "Test prediction tasks with target variable at the lower cardinality side",
            "testPatterns" -> "Test prediction tasks by patterns",
            "predictionCardinalities" -> "Prediction tasks from predicted triples with target variable at the lower cardinality side",
            "predictionPatterns" -> "Prediction tasks from predicted triples by patterns",
            "predictionPosition" -> "Prediction tasks from predicted triples with a target variable position",
          ), default = Some("predictionCardinalities"), (key, _) => key match {
            case "testPatterns" | "predictionPatterns" => activateCustomFields(true, false)
            case "predictionPosition" => activateCustomFields(false, true)
            case _ => activateCustomFields(false, false)
          }),
          patterns,
          targetVariable
        )
      },
      new OptionalText[Int]("limit", "Limit", validator = GreaterThanOrEqualsTo[Int](1)),
      new OptionalText[Int]("topK", "Top-k", validator = GreaterThanOrEqualsTo[Int](1))
    )
  }

  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}