package com.github.propi.rdfrules.gui

import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.binding.Binding.{Constants, Var}
import org.scalajs.dom.Event
import org.scalajs.dom.html.Div

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
trait Operation {
  val info: OperationInfo
  val `type`: Var[Operation.Type]
  val properties: Constants[Property]
  val previousOperation: Var[Option[Operation]]
  val description: Var[String] = Var("")

  private val nextOperation: Var[Option[Operation]] = Var(None)

  protected def followingOperations: Constants[OperationInfo]

  protected def buildFollowingOperation(operationInfo: OperationInfo): Operation

  private def delete(): Unit = {
    nextOperation.value.foreach(_.delete())
    nextOperation.value = None
    previousOperation.value.foreach(_.nextOperation.value = None)
    Main.canvas.deleteLastOperation()
  }

  @dom
  def view: Binding[Div] = {
    val _type = `type`.bind
    val _nextOperation = nextOperation.bind
    val _previousOperation = previousOperation.bind
    val classHidden = if (_nextOperation.nonEmpty && _previousOperation.isEmpty) " hidden" else ""
    val classHasNext = if (_nextOperation.nonEmpty) " has-next" else ""
    <div class={_type.toString + " operation " + info.name + classHidden + classHasNext} ondblclick={_: Event => if (properties.value.nonEmpty) Main.canvas.openModal(viewProperties)}>
      <a class={"add" + (if (_type == Operation.Type.Action || _nextOperation.nonEmpty) " hidden" else "")} onclick={_: Event => Main.canvas.openModal(viewFollowingOperations)}>
        <i class="material-icons">add_circle_outline</i>
      </a>
      <a class="del" onclick={_: Event => delete()}>
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
      {for (operationInfo <- followingOperations) yield {
      <div class={operationInfo.types.mkString(" ") + " operation-info"} onclick={_: Event =>
        val newOperation = buildFollowingOperation(operationInfo)
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