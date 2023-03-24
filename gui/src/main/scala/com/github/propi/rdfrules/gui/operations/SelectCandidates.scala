package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.Property.SummaryTitle
import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.results.PredictedResult
import com.github.propi.rdfrules.gui.utils.CommonValidators.GreaterThanOrEqualsTo
import com.github.propi.rdfrules.gui.utils.StringConverters._
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class SelectCandidates(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = {
    val strategy = new DynamicElement(Constants(
      Group("strategy", "Strategy", "strategy") { implicit context =>
        val k = new DynamicElement(Constants(new FixedText[Int]("k", "k-value", validator = GreaterThanOrEqualsTo[Int](1))), true)
        Constants(
          new Select("type", "Type", Constants(
            "pca" -> "PCA",
            "qpca" -> "QPCA",
            "topK" -> "Top-k"
          ), onSelect = (key, _) => if (key == "topK") k.setElement(0) else k.setElement(-1), summaryTitle = SummaryTitle.NoTitle),
          k
        )
      }
    ), true)
    Constants(
      new MultiSelect(
        "predictedResults",
        "Predicted triple constraints",
        Constants(
          PredictedResult.Positive.toString -> PredictedResult.Positive.label,
          PredictedResult.Negative.toString -> PredictedResult.Negative.label,
          PredictedResult.PcaPositive.toString -> PredictedResult.PcaPositive.label
        ),
        summaryTitle = Property.SummaryTitle.NoTitle),
      new OptionalText[Double]("minScore", "Min score", validator = GreaterThanOrEqualsTo[Double](0.0), summaryTitle = "min score"),
      new Checkbox("useStrategy", "Use a selection strategy", onChecked = isChecked => if (isChecked) strategy.setElement(0) else strategy.setElement(-1)),
      strategy
    )
  }
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}