package com.github.propi.rdfrules.gui

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
