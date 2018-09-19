package com.github.propi.rdfrules.gui

import com.github.propi.rdfrules.gui.Task.{Result, TaskException}
import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.binding.Binding.{Constants, Var}
import org.scalajs.dom.html.Div

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.timers._
import scala.util.{Failure, Success, Try}

/**
  * Created by Vaclav Zeman on 14. 9. 2018.
  */
trait ActionProgress {
  val title: String
  val id: Future[String]

  private implicit val ec: ExecutionContext = ExecutionContext.global
  private val progress: Var[Option[Try[Result]]] = Var(None)

  private def getStatus(id: String): Unit = Task.getStatus(id).onComplete {
    case Success(result) =>
      progress.value = Some(Success(result))
      if (result.finished.isEmpty) {
        setTimeout(3000)(getStatus(id))
      }
    case Failure(th) => progress.value = Some(Failure(th))
  }

  id.onComplete {
    case Success(id) => getStatus(id)
    case Failure(th) => progress.value = Some(Failure(th))
  }

  protected def viewResult(result: Constants[js.Dynamic]): Binding[Div]

  @dom
  def view: Binding[Div] = progress.bind match {
    case Some(Success(result)) =>
      <div class="action-progress">
        <h2>
          {title}
        </h2>
        <ul class="meta">
          <li>Started:
            {result.started}
          </li>
          <li>Finished:
            {result.finished.getOrElse("")}
          </li>
        </ul>
        <h3>Result</h3>
        <div class="result">
          {result.getResult match {
          case Some(x) => viewResult(x).bind
          case None => <div>This task is still in progress. Wait a moment!</div>
        }}
        </div>
        <h3>Logs</h3>
        <ul class="logs">
          {for (log <- result.getLogs) yield
          <li>
            <span class="time">
              {log.time}
              :</span> <span class="message">
            {log.message}
          </span>
          </li>}
        </ul>
      </div>
    case Some(Failure(te: TaskException)) =>
      <div class="action-progress">
        <h2>
          {title}
        </h2>
        <ul class="error">
          <li>Name:
            {te.taskError.code}
          </li>
          <li>Message:
            {te.taskError.message}
          </li>
        </ul>
      </div>
    case Some(Failure(th)) =>
      <div class="action-progress">
        <h2>
          {title}
        </h2>
        <ul class="error">
          <li>Message:
            {th.getMessage}
          </li>
        </ul>
      </div>
    case None =>
      <div class="action-progress waiting">Waiting for a result...</div>
  }

}