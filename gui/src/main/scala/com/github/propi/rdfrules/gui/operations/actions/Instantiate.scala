package com.github.propi.rdfrules.gui.operations.actions

import com.github.propi.rdfrules.gui._
import com.github.propi.rdfrules.gui.properties.{Checkbox, Hidden, Rule}
import com.github.propi.rdfrules.gui.results.PredictedResult
import com.thoughtworks.binding.Binding.{Constants, Var}

import scala.concurrent.Future
import scala.scalajs.js.JSConverters.JSRichIterableOnce

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class Instantiate(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.Instantiate

  val properties: Constants[Property] = {
    /*val predictedResults = ArrayElement("predictedResults", "Instantiated head constraints") { implicit context =>
      new Select("value", "Constraint", Constants(
        PredictedResult.Positive.toString -> PredictedResult.Positive.label,
        PredictedResult.Negative.toString -> PredictedResult.Negative.label,
        PredictedResult.PcaPositive.toString -> PredictedResult.PcaPositive.label
      ))
    }
    predictedResults.setValue(js.Array(PredictedResult.Positive.toString).asInstanceOf[js.Dynamic])*/
    Constants(
      new Rule(),
      new Hidden[Seq[String]]("predictedResults", PredictedResult.Positive.toString)(List(_), _.toJSArray),
      new Checkbox("injectiveMapping", "Injective mapping", true) /*
    new Select("part", "Part", Constants(
      "Whole" -> "Whole rule (correct predictions)",
      "Head" -> "Head (head support, or head size - if two variables)",
      "HeadExisting" -> "Head + Existing (support)",
      "HeadMissing" -> "Head + Missing (without support)",
      "BodyAll" -> "Body + All (body size)",
      "BodyExisting" -> "Body + Existing (correct predictions)",
      "BodyMissing" -> "Body + Missing (incorrect predictions)",
      "BodyComplementary" -> "Body + Complementary (PCA incorrect predictions)"), Some("Whole"), description = "The part of the rule to be instantiated. If the body is instantiated, there are four options. Existing: all instantiated triples in the head are contained in the input KG. Missing: all instantiated triples in the head are not contained in the input KG. All: all instantiated triples in the head are Existing or Missing. Complementary: an instantiated triple in the head is Missing and the subject did not contain any information related with the predicted predicate (it is new valuable knowledge)."),
    new Checkbox("allowDuplicateAtoms", "Allow duplicate atoms", description = "Check this if you want to enable duplicate instantiated atoms (triples) during projections mapping.")
  */)
  }
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))

  override def buildActionProgress(id: Future[String]): Option[ActionProgress] = Some(new results.InstantiatedRules(info.title, id))
}