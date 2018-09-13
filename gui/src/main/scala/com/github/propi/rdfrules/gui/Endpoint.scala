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

  def request(url: String, method: String, data: Option[js.Any])(callback: String => Unit): Unit = {
    var last_index = 0
    val buffer = new StringBuffer()
    val xhr = new dom.XMLHttpRequest()
    xhr.open(method, Globals.endpoint + url)
    xhr.onprogress = { _: ProgressEvent =>
      val curr_index = xhr.responseText.length
      if (last_index != curr_index) {
        buffer.append(xhr.responseText.substring(last_index, curr_index))
      }
      last_index = curr_index
    }
    xhr.onload = { _: Event =>
      callback(buffer.toString)
    }
    data match {
      case Some(data) =>
        xhr.setRequestHeader("Content-Type", "application/json")
        xhr.send(JSON.stringify(data))
      case None => xhr.send()
    }
  }

  def get[T](url: String)(callback: T => Unit)(implicit f: String => T): Unit = request(url, "GET", None)(data => callback(f(data)))

  def post[T](url: String, data: js.Any)(callback: T => Unit = (_: T) => {})(implicit f: String => T): Unit = request(url, "POST", Some(data))(data => callback(f(data)))

}