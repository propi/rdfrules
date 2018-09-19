package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.StringConverters._
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class MakeClusters(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.MakeClusters
  val properties: Constants[Property] = Constants(
    new OptionalText[Int]("minNeighbours", "Min number of neighbours to form a cluster"),
    new OptionalText[Double]("minSimilarity", "Min similarity between two rules to form a cluster"),
    new DynamicGroup("features", "Features", () => Constants(
      new Select("name", "Name", Constants(
        "Atoms" -> "Atoms",
        "Support" -> "Support",
        "Confidence" -> "Confidence",
        "PcaConfidence" -> "PCA confidence",
        "Lift" -> "Lift",
        "Length" -> "Rule length"
      ), Some("Atoms")),
      new FixedText[Double]("weight", "Weight")
    ))
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}