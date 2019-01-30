package com.github.propi.rdfrules.gui

import com.thoughtworks.binding.Binding.{Constants, Var}
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.{Event, MouseEvent}
import org.scalajs.dom.html.Div
import org.scalajs.dom.html.Anchor

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
  val errorMsg: Var[Option[String]] = Var(None)

  def buildActionProgress(id: Future[String]): Option[ActionProgress] = None

  def validate(): Boolean = {
    val msg = properties.value.iterator.map(_.validate()).find(_.nonEmpty).flatten
    errorMsg.value = msg
    msg.isEmpty
  }

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

  private def launchAction(): Boolean = {
    def validateOperation(operation: Operation): Boolean = {
      val prevValid = operation.previousOperation.value.forall(validateOperation)
      val curValid = operation.validate()
      prevValid && curValid
    }

    val isValid = validateOperation(this)
    if (isValid) {
      buildActionProgress(Task.sendTask(js.Array(toJson(Nil): _*))).foreach(x => actionProgress.value = Some(x))
    }
    isValid
  }

  private class LaunchButton {
    private val color: Var[(Int, Int, Int)] = Var((235, 253, 229))

    private def closeToColor(r: Int, g: Int, b: Int, delta: Int, waitingTime: Int): Unit = {
      def closeTo(s: Int, t: Int): Int = if (math.abs(s - t) <= delta) {
        t
      } else if (t > s) {
        s + delta
      } else {
        s - delta
      }

      val (cr, cg, cb) = color.value
      if (r != cr || g != cg || b != cb) {
        color.value = (closeTo(cr, r), closeTo(cg, g), closeTo(cb, b))
        js.timers.setTimeout(waitingTime)(closeToColor(r, g, b, delta, waitingTime))
      }
    }

    @dom
    def view: Binding[Anchor] = {
      <a class="launch" onclick={e: Event =>
        if (launchAction()) {
          Main.canvas.openModal(viewActionProgress)
        } else {
          color.value = (250, 146, 146)
          js.timers.setTimeout(50)(closeToColor(235, 253, 229, 2, 50))
        }
        e.stopPropagation()} style={val (r, g, b) = color.bind
      s"background-color: rgb($r, $g, $b);"}>Launch Pipeline</a>
    }
  }

  @dom
  def view: Binding[Div] = {
    <div class={val _nextOperation = nextOperation.bind
    val _previousOperation = previousOperation.bind
    val classHidden = if (_nextOperation.nonEmpty && _previousOperation.isEmpty) " hidden" else ""
    val classHasNext = if (_nextOperation.nonEmpty) " has-next" else ""
    info.`type`.toString + " operation " + info.name + classHidden + classHasNext} onclick={_: Event =>
      if (properties.value.nonEmpty) {
        errorMsg.value = None
        Main.canvas.openModal(viewProperties)
      }}>
      {info.`type` match {
      case Operation.Type.Transformation =>
        <a class={"add" + (if (nextOperation.bind.nonEmpty) " hidden" else "")} onclick={e: Event => Main.canvas.openModal(viewFollowingOperations); e.stopPropagation();}>
          <i class="material-icons">add_circle_outline</i>
        </a>
      case Operation.Type.Action =>
        <div class="action-buttons">
          {(new LaunchButton).view.bind}<a class={val _actionProgress = actionProgress.bind
        val actionClassHidden = if (_actionProgress.isEmpty) " hidden" else ""
        "result" + actionClassHidden} onclick={e: Event =>
          Main.canvas.openModal(viewActionProgress)
          e.stopPropagation()}>Show results</a>
        </div>
    }}<a class="del" onclick={e: Event => delete(); e.stopPropagation();}>
      <i class="material-icons">delete</i>
    </a>
      <a class={"error" + (if (errorMsg.bind.isEmpty) " hidden" else "")} onmousemove={e: MouseEvent => Main.canvas.openHint(errorMsg.value.getOrElse(""), e)} onmouseout={_: MouseEvent => Main.canvas.closeHint()}>
        <i class="material-icons">error</i>
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
  private def viewOperationInfo(operationInfo: OperationInfo): Binding[Div] = {
    <div class={operationInfo.`type` + " operation-info"} onclick={_: Event =>
      val newOperation = operationInfo.buildOperation(self)
      nextOperation.value = Some(newOperation)
      Main.canvas.addOperation(newOperation)
      Main.canvas.closeModal()}>
      {operationInfo.title}
    </div>
  }

  @dom
  private def viewFollowingOperations: Binding[Div] = {
    <div class="following-operations">
      <h3>Transformations</h3>
      <div class="transformations">
        {for (operationInfo <- Constants(info.followingOperations.value.filter(_.`type` == Operation.Type.Transformation): _*)) yield viewOperationInfo(operationInfo).bind}
      </div>
      <h3 class={if (info.followingOperations.value.exists(_.`type` == Operation.Type.Action)) "" else "hidden"}>Actions</h3>
      <div class="actions">
        {for (operationInfo <- Constants(info.followingOperations.value.filter(_.`type` == Operation.Type.Action): _*)) yield viewOperationInfo(operationInfo).bind}
      </div>
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