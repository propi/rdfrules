package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class Prune(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = {
    /*val (cba1, cba2) = context.use("Data coverage pruning") { implicit context =>
      new DynamicElement(Constants(new Checkbox("onlyFunctionalProperties", "Only functional properties", true)), true) ->
        new DynamicElement(Constants(new Checkbox("onlyExistingTriples", "Only existing triples", true)), true)
    }*/
    val measure = context.use("Closed or SkylinePruning")(implicit context => new DynamicElement(Constants(new Select("measure", "Measure", Constants(
      "HeadSize" -> "Head size",
      "HeadSupport" -> "Head support",
      "Support" -> "Support",
      "HeadCoverage" -> "Head coverage",
      "BodySize" -> "Body size",
      "Confidence" -> "CWA confidence",
      "PcaConfidence" -> "PCA confidence",
      "PcaBodySize" -> "PCA body size",
      "QpcaConfidence" -> "QPCA confidence",
      "QpcaBodySize" -> "QPCA body size",
      "Lift" -> "Lift"
    ))), true))

    //    def activeStrategy(cba: Boolean, hasMeasure: Boolean): Unit = {
    //      if (cba) {
    //        cba1.setElement(0)
    //        cba2.setElement(0)
    //      } else {
    //        cba1.setElement(-1)
    //        cba2.setElement(-1)
    //      }
    //      if (hasMeasure) measure.setElement(0) else measure.setElement(-1)
    //    }

    //activeStrategy(true, false)

    Constants(
      new Select("strategy", "Strategy",
        Constants("DataCoveragePruning" -> "Data coverage pruning", "Maximal" -> "Maximal", "Closed" -> "Close", "SkylinePruning" -> "Skyline pruning", "WithoutQuasiBinding" -> "Without quasi-binding"),
        Some("DataCoveragePruning"),
        {
          case ("Closed", _) | ("SkylinePruning", _) => measure.setElement(0)
          case _ => measure.setElement(-1)
        },
        Property.SummaryTitle.NoTitle
      ),
      measure,
      new Hidden[Boolean]("onlyFunctionalProperties", "false")(_.toBoolean, x => x),
      new Hidden[Boolean]("onlyExistingTriples", "true")(_.toBoolean, x => x),
      new Hidden[Boolean]("injectiveMapping", "true")(_.toBoolean, x => x)
    )
  }
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}