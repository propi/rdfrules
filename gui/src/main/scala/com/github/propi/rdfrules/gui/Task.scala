package com.github.propi.rdfrules.gui

import com.thoughtworks.binding.Binding.Constants

import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.JSON

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
object Task {

  implicit class ResultOps(result: Result) {
    def getLogs: Constants[Log] = Constants(result.logs: _*)

    def getResult: Option[Constants[js.Dynamic]] = result.result.toOption.map(x => Constants(x: _*))
  }

  trait Result extends js.Object {
    val id: String
    val started: String
    val finished: js.UndefOr[String]
    val logs: js.Array[Log]
    val result: js.UndefOr[js.Array[js.Dynamic]]
  }

  trait Log extends js.Object {
    val time: String
    val message: String
  }

  trait TaskError extends js.Object {
    val code: String
    val message: String
  }

  case class TaskException(msg: String) extends Exception(msg) {
    lazy val taskError: TaskError = JSON.parse(msg).asInstanceOf[TaskError]
  }

  private val notFound = TaskException("""{"code": "NotFound", "message": "Task not found"}""")
  private val unspecifiedTaskError = TaskException("""{"code": "UnspecifiedTaskError", "message": "Unspecified task error"}""")

  def sendTask(data: js.Any): Future[String] = {
    val result = Promise[String]()
    Endpoint.post[String]("/task", data) { response =>
      response.headers.get("location").map(_.replaceFirst(".*/", "")) match {
        case Some(id) if id.nonEmpty && response.status == 202 => result.success(id)
        case None => response.status match {
          case 400 | 500 => result.failure(TaskException(response.data))
          case _ => result.failure(unspecifiedTaskError)
        }
      }
    }
    result.future
  }

  def getStatus(id: String): Future[Result] = {
    val result = Promise[Result]()
    Endpoint.get[String]("/task/" + id) { response =>
      response.status match {
        case 202 | 200 => result.success(JSON.parse(response.data).asInstanceOf[Result])
        case 400 | 500 => result.failure(TaskException(response.data))
        case 404 => result.failure(notFound)
        case _ => result.failure(unspecifiedTaskError)
      }
    }
    result.future
  }

}
