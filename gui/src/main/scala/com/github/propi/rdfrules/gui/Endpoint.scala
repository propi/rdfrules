package com.github.propi.rdfrules.gui

import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.ProgressEvent

import scala.scalajs.js
import scala.scalajs.js.JSON

/**
  * Created by Vaclav Zeman on 12. 9. 2018.
  */
object Endpoint {

  case class Response[T](status: Int, headers: Map[String, String], data: T) {
    def to[A](f: T => A): Response[A] = Response(status, headers, f(data))
  }

  def request(url: String, method: String, data: Option[js.Any], contentType: Option[String] = Some("application/json"))(callback: Response[String] => Unit): Unit = {
    var last_index = 0
    val buffer = new StringBuffer()
    val xhr = new dom.XMLHttpRequest()
    xhr.open(method, Globals.endpoint + url)
    xhr.setRequestHeader("Access-Control-Expose-Headers", "Location")
    xhr.onprogress = { _: ProgressEvent =>
      val curr_index = xhr.responseText.length
      if (last_index != curr_index) {
        buffer.append(xhr.responseText.substring(last_index, curr_index))
      }
      last_index = curr_index
    }
    xhr.onload = { _: Event =>
      val Header = "(.+?)\\s*:\\s*(.+)".r
      val headers = xhr.getAllResponseHeaders().trim.split("[\\r\\n]+").collect {
        case Header(name, value) => name.toLowerCase -> value.toLowerCase
      }.toMap
      if (buffer.length() == 0) {
        buffer.append(xhr.responseText)
      }
      callback(Response(xhr.status, headers, buffer.toString))
    }
    data match {
      case Some(data) =>
        contentType match {
          case Some("application/json") =>
            xhr.setRequestHeader("Content-Type", "application/json")
            xhr.send(JSON.stringify(data))
          case Some(contentType) =>
            xhr.setRequestHeader("Content-Type", contentType)
            xhr.send(data)
          case None =>
            xhr.send(data)
        }
      case None => xhr.send()
    }
  }

  def delete[T](url: String)(callback: Response[T] => Unit)(implicit f: String => T): Unit = request(url, "DELETE", None)(data => callback(data.to(f)))

  def get[T](url: String)(callback: Response[T] => Unit)(implicit f: String => T): Unit = request(url, "GET", None)(data => callback(data.to(f)))

  def post[T](url: String, data: js.Any)(callback: Response[T] => Unit = (_: Response[T]) => {})(implicit f: String => T): Unit = request(url, "POST", Some(data))(data => callback(data.to(f)))

  def postWithContentType[T](url: String, data: js.Any, contentType: String)(callback: Response[T] => Unit = (_: Response[T]) => {})(implicit f: String => T): Unit = request(url, "POST", Some(data), Some(contentType))(data => callback(data.to(f)))

  def postWithAutoContentType[T](url: String, data: js.Any)(callback: Response[T] => Unit = (_: Response[T]) => {})(implicit f: String => T): Unit = request(url, "POST", Some(data), None)(data => callback(data.to(f)))

}