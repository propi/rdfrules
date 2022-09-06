package com.github.propi.rdfrules.gui

import com.github.propi.rdfrules.gui.OperationInfo.IndexTransformation.LoadRulesetWithRules
import com.github.propi.rdfrules.gui.OperationInfo.{GetPrediction, Instantiate}
import com.github.propi.rdfrules.gui.OperationInfo.RulesetTransformation.Predict

import scala.scalajs.concurrent.JSExecutionContext.Implicits._
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
@JSExportTopLevel("RDFRules")
object Main {

  lazy val canvas = new Canvas

  @JSExport
  def loadTask(content: String): Unit = canvas.loadTask(content)

  def main(args: Array[String]): Unit = {
    Documentation.init()
    canvas.render()
    Option(Globals.getParameterByName(Canvas.newWindowTaskKey))
      .filter(_ == "1")
      .flatMap(_ => LocalStorage.get(Canvas.newWindowTaskKey))
      .foreach { x =>
        canvas.loadTask(x)
        LocalStorage.remove(Canvas.newWindowTaskKey)
      }
    Option(Globals.getParameterByName(Canvas.loadRulesKey))
      .filter(_ == "1")
      .flatMap(_ => LocalStorage.get(Canvas.loadRulesKey))
      .foreach { x =>
        canvas.addOperation(LoadRulesetWithRules, js.Dynamic.literal(rules = JSON.parse(x)))
        LocalStorage.remove(Canvas.loadRulesKey)
      }
    for (_ <- AutoCaching.loadCache()) {
      Option(Globals.getParameterByName(Canvas.instantiationKey))
        .filter(_ == "1")
        .flatMap(_ => LocalStorage.get(Canvas.instantiationKey))
        .foreach { x =>
          canvas.addOperation(Instantiate, js.Dynamic.literal(rules = js.Array(JSON.parse(x))))
          LocalStorage.remove(Canvas.instantiationKey)
          canvas.getOperations.last.launch()
        }
      Option(Globals.getParameterByName(Canvas.predictionKey))
        .filter(_ == "1")
        .flatMap(_ => LocalStorage.get(Canvas.predictionKey))
        .foreach { x =>
          canvas.addOperation(Predict, js.Dynamic.literal(rules = js.Array(JSON.parse(x))))
          canvas.addOperation(GetPrediction, js.Dynamic.literal(showRules = false))
          LocalStorage.remove(Canvas.predictionKey)
          canvas.getOperations.last.launch()
        }
    }
    /*for {
      taskId <- Option(Globals.getParameterByName("pickup")).map(_.trim).filter(_.nonEmpty)
      task <- LocalStorage.get[String](taskId)(Some(_))
      rule <- LocalStorage.get[String](taskId + "-rule")(Some(_)).map(JSON.parse(_)).map(_.asInstanceOf[Rules.Rule])
    } {
      LocalStorage.remove(taskId)
      LocalStorage.remove(taskId + "-rule")
      canvas.loadTask(task)
      canvas.getOperations.last.delete()
      val op0 = canvas.getOperations.last
      val op1 = op0.appendOperation(OperationInfo.RulesetTransformation.Instantiate)
      op1.asInstanceOf[Instantiate].setRule(rule)
      canvas.addOperation(op1)
      canvas.addOperation(op1.appendOperation(OperationInfo.GetRules))
      op1.openModal()
    }*/
  }

}
