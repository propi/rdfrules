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
      new FixedText[Double]("min", "Min confidence", "0.5", "A minimal confidence threshold. This operation counts the standard confidence for all rules and filter them by this minimal threshold. The value range is between 0.001 and 1 included. Default value is set to 0.5.", RegExp("1(\\.0+)?|0\\.00[1-9]\\d*|0\\.0?[1-9]\\d*")),
      new FixedText[Double]("min", "Min PCA confidence", "0.5", "A minimal PCA (Partial Completeness Assumption) confidence threshold. This operation counts the PCA confidence for all rules and filter them by this minimal threshold. The value range is between 0.001 and 1 included. Default value is set to 0.5.", RegExp("1(\\.0+)?|0\\.00[1-9]\\d*|0\\.0?[1-9]\\d*")),
      new FixedText[Double]("min", "Min confidence", "0.5", "A minimal confidence threshold. This operation first counts the standard confidence for all rules and filter them by this minimal threshold, then it counts the lift measure by the computed cofindence. The value range is between 0.001 and 1 included. Default value is set to 0.5.", RegExp("1(\\.0+)?|0\\.00[1-9]\\d*|0\\.0?[1-9]\\d*"))
    ))
    val topK = new DynamicElement(Constants(
      new OptionalText[Int]("topk", "Top-k", description = "Get top-k rules with highest confidence. This is the optional parameter.", validator = GreaterThanOrEqualsTo[Int](1))
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
          case "Lift" => activeStrategy(2, false)
          case "PcaConfidence" => activeStrategy(1, true)
          case _ => activeStrategy(0, true)
        }
      ),
      min,
      topK
    )
  }
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}