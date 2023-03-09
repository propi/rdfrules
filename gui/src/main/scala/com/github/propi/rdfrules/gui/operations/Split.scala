package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators.RegExp
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}
import com.github.propi.rdfrules.gui.utils.StringConverters._

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class Split(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = {
    var trainSettings = Option.empty[DynamicElement]
    var testSettings = Option.empty[DynamicElement]

    def activeValueSplitting(x: Int): Unit = {
      trainSettings.foreach(_.setElement(x))
      testSettings.foreach(_.setElement(x))
    }

    Constants(
      new Select("splittingValue", "Value for splitting",
        Constants("relative" -> "Relative", "absolute" -> "Absolute"),
        Some("relative"),
        {
          case ("absolute", _) => activeValueSplitting(1)
          case ("relative", _) => activeValueSplitting(0)
          case _ => activeValueSplitting(0)
        }
      ),
      Group("train", "Training set", Property.SummaryTitle.NoTitle) { implicit context =>
        trainSettings = Some(new DynamicElement(Constants(
          new FixedText[Double]("ratio", "Ratio", "0.8", RegExp("0\\.\\d+"), "training set"),
          new FixedText[Int]("max", "Number of triples", validator = RegExp("\\d+"), summaryTitle = "training set")
        )))
        trainSettings.get.setElement(0)
        Constants(
          new FixedText[String]("uri", "URI", "<trainingSet>", RegExp("<.*>|.*:.*")),
          Group("part", "Part size", Property.SummaryTitle.NoTitle)(_ => Constants(trainSettings.get))
        )
      },
      Group("test", "Test set", Property.SummaryTitle.NoTitle) { implicit context =>
        testSettings = Some(new DynamicElement(Constants(
          new FixedText[Double]("ratio", "Ratio", "0.2", RegExp("0\\.\\d+"), "test set"),
          new FixedText[Int]("max", "Number of triples", validator = RegExp("\\d+"), summaryTitle = "test set")
        )))
        testSettings.get.setElement(0)
        Constants(
          new FixedText[String]("uri", "URI", "<testSet>", RegExp("<.*>|.*:.*")),
          Group("part", "Part size", Property.SummaryTitle.NoTitle)(_ => Constants(testSettings.get))
        )
      }
    )
  }
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}