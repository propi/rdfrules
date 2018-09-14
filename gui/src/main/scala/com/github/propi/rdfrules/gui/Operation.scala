package com.github.propi.rdfrules.gui

import com.thoughtworks.binding.Binding.{Constants, Var}
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.Event
import org.scalajs.dom.html.Div

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
trait Operation {
  self =>

  val info: OperationInfo
  val properties: Constants[Property]
  val previousOperation: Var[Option[Operation]]
  val description: Var[String] = Var("")

  def buildActionProgress(id: Future[String]): Option[ActionProgress] = None

  private val nextOperation: Var[Option[Operation]] = Var(None)
  private val actionProgress: Var[Option[ActionProgress]] = Var(None)

  private def delete(): Unit = {
    nextOperation.value.foreach(_.delete())
    nextOperation.value = None
    previousOperation.value.foreach(_.nextOperation.value = None)
    Main.canvas.deleteLastOperation()
  }

  protected def propertiesToJson: js.Dictionary[js.Any] = {
    js.Dictionary(properties.value.map(x => x.name -> x.toJson).filter(x => !js.isUndefined(x._2)): _*)
  }

  private def toJson(list: List[js.Any]): List[js.Any] = {
    val json = js.Dynamic.literal(name = info.name, parameters = propertiesToJson)
    previousOperation.value match {
      case Some(op) => op.toJson(json :: list)
      case None => list
    }
  }

  private def launchAction(): Unit = {
    previousOperation.value
    buildActionProgress(Task.sendTask(js.Array(toJson(Nil): _*))).foreach(x => actionProgress.value = Some(x))
  }

  @dom
  def view: Binding[Div] = {
    val _nextOperation = nextOperation.bind
    val _previousOperation = previousOperation.bind
    val classHidden = if (_nextOperation.nonEmpty && _previousOperation.isEmpty) " hidden" else ""
    val classHasNext = if (_nextOperation.nonEmpty) " has-next" else ""
    <div class={info.`type`.toString + " operation " + info.name + classHidden + classHasNext} ondblclick={_: Event => if (properties.value.nonEmpty) Main.canvas.openModal(viewProperties)}>
      {info.`type` match {
      case Operation.Type.Transformation =>
        <a class={"add" + (if (_nextOperation.nonEmpty) " hidden" else "")} onclick={_: Event => Main.canvas.openModal(viewFollowingOperations)}>
          <i class="material-icons">add_circle_outline</i>
        </a>
      case Operation.Type.Action =>
        <a class="launch" onclick={_: Event =>
          launchAction()
          Main.canvas.openModal(viewActionProgress)}>Launch Pipeline</a>
    }}<a class="del" onclick={_: Event => delete()}>
      <i class="material-icons">delete</i>
    </a>
      <strong class="title">
        {info.title}
      </strong>
      <span class="description">
        {description.bind}
      </span>
    </div>
  }

  @dom
  private def viewActionProgress: Binding[Div] = {
    actionProgress.bind match {
      case Some(ap) =>
        <div>
          {ap.view.bind}
        </div>
      case None => <div class="action-progress">No action!</div>
    }
  }

  @dom
  private def viewProperties: Binding[Div] = {
    <div class="properties">
      <table>
        {for (property <- properties) yield {
        property.view.bind
      }}
      </table>
    </div>
  }

  @dom
  private def viewFollowingOperations: Binding[Div] = {
    <div class="following-operations">
      {for (operationInfo <- info.followingOperations) yield {
      <div class={operationInfo.`type` + " operation-info"} onclick={_: Event =>
        val newOperation = operationInfo.buildOperation(self)
        nextOperation.value = Some(newOperation)
        Main.canvas.addOperation(newOperation)
        Main.canvas.closeModal()}>
        {operationInfo.title}
      </div>
    }}
    </div>
  }

}

object Operation {

  sealed trait Type

  object Type {

    object Transformation extends Type {
      override def toString: String = "transformation"
    }

    object Action extends Type {
      override def toString: String = "action"
    }

  }

}