package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.Property.SummaryTitle
import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators.GreaterThanOrEqualsTo
import com.github.propi.rdfrules.gui.utils.StringConverters._
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class GroupPredictions(fromOperation: Operation, val info: OperationInfo) extends Operation {

  val properties: Constants[Property] = {
    val confidence1 = new Hidden[String]("confidence", "default")
    val confidence2 = new Hidden[String]("confidence", "default")
    val confidence = new DynamicElement(Constants(
      new Select("confidence", "Confidence", Constants(
        "default" -> "Default",
        "cwa" -> "CWA",
        "pca" -> "PCA",
        "qpca" -> "QPCA"
      ), default = Some("default"), (key, _) => {
        confidence1.setValueString(key)
        confidence2.setValueString(key)
      })
    ), true)
    val scorer = new DynamicElement(Constants(Group("scorer", "Scorer", summaryTitle = SummaryTitle.NoTitle) { implicit context =>
      Constants(
        new Select("type", "Strategy", Constants(
          "maximum" -> "Maximum",
          "noisyOr" -> "Noisy-or"
        ), Some("maximum"), summaryTitle = "scorer"),
        confidence1
      )
    }), true)
    val consumer = new DynamicElement(Constants(Group("consumer", "Rules consumer", summaryTitle = SummaryTitle.NoTitle) { implicit context =>
      Constants(
        new Select("type", "Strategy", Constants(
          "topK" -> "Top-k"
        ), Some("topK"), summaryTitle = "consumer"),
        new FixedText[Int]("topK", "k-value", validator = GreaterThanOrEqualsTo[Int](1)),
        confidence2
      )
    }), true)

    def activateStrategy(_scorer: Option[Boolean], _consumer: Option[Boolean]): Unit = {
      val (main, second) = if (_scorer.isDefined) scorer -> consumer else consumer -> scorer
      _scorer.orElse(_consumer).foreach { x =>
        main.setElement(if (x) 0 else -1)
        confidence.setElement(if (!x && second.getActive < 0) -1 else 0)
      }
    }

    Constants(
      new Checkbox("useScorer", "Use scorer", onChecked = isChecked => activateStrategy(Some(isChecked), None)),
      scorer,
      new Checkbox("useConsumer", "Use rules consumer", onChecked = isChecked => activateStrategy(None, Some(isChecked))),
      consumer,
      confidence,
      new OptionalText[Int]("limit", "Limit", validator = GreaterThanOrEqualsTo[Int](1))
    )
  }

  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}

