package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators.{GreaterThanOrEqualsTo, RegExp}
import com.github.propi.rdfrules.gui.utils.StringConverters._
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class ComputeConfidence(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = {
    val min = new DynamicElement(Constants(
      context.use("CWA confidence")(implicit context => new FixedText[Double]("min", "Min confidence", "0.5", RegExp("1(\\.0+)?|0\\.00[1-9]\\d*|0\\.0?[1-9]\\d*"), "min")),
      context.use("PCA confidence")(implicit context => new FixedText[Double]("min", "Min PCA confidence", "0.5", RegExp("1(\\.0+)?|0\\.00[1-9]\\d*|0\\.0?[1-9]\\d*"), "min")),
      context.use("Lift")(implicit context => new FixedText[Double]("min", "Min confidence", "0.5", RegExp("1(\\.0+)?|0\\.00[1-9]\\d*|0\\.0?[1-9]\\d*"), "min"))
    ))
    val topK = new DynamicElement(Constants(
      new OptionalText[Int]("topk", "Top-k", validator = GreaterThanOrEqualsTo[Int](1), summaryTitle = "top")
    ))

    def activeStrategy(minIndex: Int, hasTopK: Boolean): Unit = {
      min.setElement(minIndex)
      if (hasTopK) topK.setElement(0) else topK.setElement(-1)
    }

    Constants(
      new Select("name", "Name",
        Constants("StandardConfidence" -> "CWA confidence", "PcaConfidence" -> "PCA confidence", "Lift" -> "Lift"),
        Some("StandardConfidence"),
        {
          case ("Lift", _) => activeStrategy(2, false)
          case ("PcaConfidence", _) => activeStrategy(1, true)
          case _ => activeStrategy(0, true)
        },
        Property.SummaryTitle.NoTitle
      ),
      min,
      topK
    )
  }
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}