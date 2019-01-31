package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators.{GreaterThan, GreaterThanOrEqualsTo, LowerThanOrEqualsTo}
import com.github.propi.rdfrules.gui.utils.StringConverters._
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class MakeClusters(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.MakeClusters
  val properties: Constants[Property] = Constants(
    new OptionalText[Int]("minNeighbours", "Min neighbours", default = "5", description = "Min number of neighbours to form a cluster.", validator = GreaterThanOrEqualsTo[Int](2)),
    new OptionalText[Double]("minSimilarity", "Min similarity", default = "0.9", description = "Min similarity between two rules to form a cluster.", validator = GreaterThanOrEqualsTo(0.0).map[String] & LowerThanOrEqualsTo(1.0).map[String]),
    new DynamicGroup("features", "Features", () => Constants(
      new Select("name", "Name", Constants(
        "Atoms" -> "Atoms",
        "Support" -> "Support",
        "Confidence" -> "Confidence",
        "PcaConfidence" -> "PCA confidence",
        "Lift" -> "Lift",
        "Length" -> "Rule length"
      ), Some("Atoms"), description = "Choose feature which will be participated during similarities counting."),
      new FixedText[Double]("weight", "Weight", description = "Weight of the chosen feature. The sum of all weight must be one.", validator = GreaterThan(0.0).map[String] & LowerThanOrEqualsTo(1.0).map[String])
    ), description = "Here, you can specify features which are participating to count similarities between two rules. Default is Atoms:0.5, Length:0.1, Support:0.15, PcaConfidence:0.15, Confidence:0.05, Lift:0.05.")
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))

  override def validate(): Boolean = {
    if (super.validate()) {
      val weightsList = properties.value.lift(2).iterator.collect {
        case x: DynamicGroup => x.getGroups
      }.flatten.flatMap(_.value.lift(1)).collect {
        case x: Text => stringToTryDouble(x.getText).toOption
      }.flatten.toList
      val weightsSum = weightsList.foldLeft(0.0)(_ + _)
      val isValid = weightsList.isEmpty || weightsSum == 1
      if (!isValid) errorMsg.value = Some("The sum of all weight must be one.")
      isValid
    } else {
      false
    }
  }
}