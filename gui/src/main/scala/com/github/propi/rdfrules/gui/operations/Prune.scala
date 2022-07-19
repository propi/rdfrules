package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class Prune(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = {
    val (cba1, cba2) = context.use("Data coverage pruning") { implicit context =>
      new DynamicElement(Constants(new Checkbox("onlyFunctionalProperties", "Only functional properties", true))) ->
        new DynamicElement(Constants(new Checkbox("onlyExistingTriples", "Only existing triples", true)))
    }
    val measure = context.use("Closed or OnlyBetterDescendant")(implicit context => new DynamicElement(Constants(new Select("measure", "Measure", Constants(
      "RuleLength" -> "Rule length",
      "HeadSize" -> "Head size",
      "Support" -> "Support",
      "HeadCoverage" -> "Head coverage",
      "BodySize" -> "Body size",
      "Confidence" -> "Confidence",
      "PcaConfidence" -> "PCA confidence",
      "PcaBodySize" -> "PCA body size",
      "HeadConfidence" -> "Head confidence",
      "Lift" -> "Lift"
    ), Some("HeadCoverage")))))

    def activeStrategy(cba: Boolean, hasMeasure: Boolean): Unit = {
      if (cba) {
        cba1.setElement(0)
        cba2.setElement(0)
      } else {
        cba1.setElement(-1)
        cba2.setElement(-1)
      }
      if (hasMeasure) measure.setElement(0) else measure.setElement(-1)
    }

    activeStrategy(true, false)

    Constants(
      new Select("name", "Strategy",
        Constants("DataCoveragePruning" -> "Data coverage pruning", "Maximal" -> "Maximal", "Closed" -> "Close", "OnlyBetterDescendant" -> "Only better descendant"),
        Some("DataCoveragePruning"),
        {
          case "DataCoveragePruning" => activeStrategy(true, false)
          case "Closed" | "OnlyBetterDescendant" => activeStrategy(false, true)
          case _ => activeStrategy(false, false)
        }
      ),
      cba1,
      cba2,
      measure
    )
  }
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}