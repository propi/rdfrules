package com.github.propi.rdfrules.gui

import com.github.propi.rdfrules.gui.operations.Root
import com.thoughtworks.binding.Binding.{Var, Vars}
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.{FileReader, HTMLElement, HTMLInputElement}
import org.scalajs.dom.{Event, MouseEvent, UIEvent, document}

import scala.scalajs.js
import scala.scalajs.js.JSON

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class Canvas {

  private val modal: Var[Option[Binding[Div]]] = Var(None)
  private val fixedHint: Var[Option[(Double, Double)]] = Var(None)
  private val hint: Var[Option[(String, Double, Double)]] = Var(None)
  private val operations: Vars[Operation] = Vars(new Root)

  @dom
  private def view: Binding[Div] = {
    <div class="canvas">
      <div class="memory-info">
        {Endpoint.memoryCacheInfoView.bind}
      </div>
      <div class={"whint" + (if (hint.bind.isEmpty) " hidden" else "")} style={val _hint = hint.bind
      val _fixedHint = fixedHint.bind
      (_hint, _fixedHint) match {
        case (Some((_, x, y)), None) => s"left: ${x}px; top: ${y}px;"
        case (_, Some((x, y))) => s"left: ${x}px; top: ${y}px; position: absolute;"
        case _ => ""
      }}>
        <i class={"material-icons close" + (if (fixedHint.bind.isEmpty) " hidden" else "")} onclick={_: Event => unfixHint()}>close</i>{hint.bind.map(_._1).getOrElse("")}
      </div>
      <div class={"modal" + (if (modal.bind.isEmpty) " closed" else " open")}>
        <a class="close" onclick={_: Event => closeModal()}>
          <i class="material-icons">close</i>
        </a>{modal.bind match {
        case Some(x) => x.bind
        case None => <div></div>
      }}<div class="ok"><span onclick={_: Event => closeModal()}>Ok</span></div>
      </div>
      <div class="content">
        <div class="tools">
          <input type="file" accept="application/json" id="importfile" onchange={e: Event =>
          val files = e.target.asInstanceOf[HTMLInputElement].files
          if (files.length > 0) {
            val reader = new FileReader
            reader.onload = (_: UIEvent) => {
              loadTask(reader.result.asInstanceOf[String])
            }
            reader.readAsText(files(0))
          }}/>
          <i class="material-icons" title="Save this pipeline to a local file." onclick={_: Event =>
            val op = operations.value.last
            if (op.validateAll()) {
              Downloader.download("task.json", js.Array(op.toJson(Nil): _*))
            }}>save</i>
          <i class="material-icons" title="Load a pipeline from a local file." onclick={_: Event =>
            val file = document.getElementById("importfile").asInstanceOf[HTMLInputElement]
            file.value = ""
            file.click()}>folder_open</i>
        </div>
        <div class="operations">
          {for (operation <- operations) yield {
          operation.view.bind
        }}
        </div>
      </div>
    </div>
  }

  def addOperation(operation: Operation): Unit = {
    if (operation.getNextOperation.isEmpty) {
      operations.value += operation
    } else {
      operations.value.clear()
      val firstOps = Stream.iterate(operation.previousOperation.value)(_.flatMap(_.previousOperation.value)).takeWhile(_.nonEmpty).flatten.last
      operations.value ++= Stream.iterate(Option(firstOps))(_.flatMap(_.getNextOperation)).takeWhile(_.nonEmpty).flatten
    }
  }

  def deleteOperation(op: Operation): Unit = operations.value -= op

  def deleteLastOperation(): Unit = operations.value.remove(operations.value.size - 1)

  def openModal(content: Binding[Div]): Unit = modal.value = Some(content)

  def closeModal(): Unit = modal.value = None

  def openHint(x: String, event: MouseEvent): Unit = hint.value = Some(x, event.pageX + 10, event.pageY + 10)

  def fixHint(time: Int = 300): Unit = {
    for ((x, y) <- hint.value.map(x => x._2 -> (x._3 - document.getElementById("rdfrules").asInstanceOf[HTMLElement].offsetTop))) {
      val steps = math.ceil(math.max(25, time) / 25.0)
      val xDelta = x / steps
      val yDelta = y / steps

      def moveHint(): Unit = {
        for ((x, y) <- fixedHint.value if x > 0 || y > 0) {
          fixedHint.value = Some(math.max(0, x - xDelta), math.max(0, y - yDelta))
          js.timers.setTimeout(25)(moveHint())
        }
      }

      fixedHint.value = Some(x, y)
      moveHint()
    }
  }

  def unfixHint(): Unit = {
    fixedHint.value = None
    closeHint()
  }

  def closeHint(): Unit = {
    if (fixedHint.value.isEmpty) hint.value = None
  }

  def loadTask(content: String): Unit = {
    operations.value.clear()
    val lastOps = JSON.parse(content)
      .asInstanceOf[js.Array[js.Dynamic]]
      .foldLeft[Operation](new Root) { (parent, data) =>
      OperationInfo(data, parent)
        .filter(parent.info.followingOperations.value.contains)
        .map { opsInfo =>
          val newOps = parent.appendOperation(opsInfo)
          newOps.setValue(data.parameters)
          newOps
        }.getOrElse(parent)
    }
    val newOperations = Iterator.iterate(Option(lastOps))(_.flatMap(_.previousOperation.value)).takeWhile(_.nonEmpty).flatten.foldLeft(List.empty[Operation])((a, b) => b :: a)
    operations.value ++= newOperations
  }

  def render(): Unit = dom.render(document.getElementById("rdfrules"), view)

}