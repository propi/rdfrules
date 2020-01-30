package com.github.propi.rdfrules.gui

import com.thoughtworks.binding
import com.thoughtworks.binding.Binding
import com.thoughtworks.binding.Binding.Var
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.ProgressEvent
import com.github.propi.rdfrules.gui.utils.StringConverters._

import scala.scalajs.js
import scala.scalajs.js.JSON

/**
  * Created by Vaclav Zeman on 12. 9. 2018.
  */
object Endpoint {

  private val memoryInfo: Var[Option[MemoryInfo]] = Var(None)

  case class MemoryInfo(total: Long, free: Long, itemsInCache: Int) {
    def used: Long = total - free
  }

  case class UploadProgress(loaded: Double, total: Option[Double]) {
    def loadedInPercent: Option[Double] = total.collect {
      case total if total > 0 => (loaded / total) * 100
    }
  }

  case class Response[T](status: Int, headers: Map[String, String], data: T) {
    def to[A](f: T => A): Response[A] = Response(status, headers, f(data))
  }

  @binding.dom
  def memoryCacheInfoView: Binding[Div] = {
    <div>
      {memoryInfo.bind match {
      case Some(memoryInfo) => <div class="list">
        <div>
          <span>Used memory:</span>
          <span class="used">
            {Workspace.humanReadableByteSize(memoryInfo.used)}
          </span>
          <span>/</span>
          <span class="total">
            {Workspace.humanReadableByteSize(memoryInfo.total)}
          </span>
        </div>
        <div>
          <span>Items in the cache:</span>
          <span class="items">
            {memoryInfo.itemsInCache.toString}
          </span>
          <span class="clear" onclick={_: Event => get[String]("/cache/clear")(_ => Unit)}>(clear)</span>
        </div>
      </div>
      case None => <div></div>
    }}
    </div>
  }

  def request(url: String, method: String, data: Option[js.Any], contentType: Option[String] = Some("application/json"), uploadProgress: UploadProgress => Unit = _ => Unit)(callback: Response[String] => Unit): Unit = {
    var last_index = 0
    val buffer = new StringBuffer()
    val xhr = new dom.XMLHttpRequest()
    xhr.open(method, Globals.endpoint + url)
    xhr.setRequestHeader("Access-Control-Expose-Headers", "Location, MemoryCache-Free, MemoryCache-Items, MemoryCache-Total")
    if (method == "POST" && xhr.upload != null) {
      xhr.upload.onprogress = { e: ProgressEvent =>
        uploadProgress(UploadProgress(e.loaded, if (e.lengthComputable) Some(e.total) else None))
      }
    }
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
      for {
        total <- headers.get("memorycache-total")
        free <- headers.get("memorycache-free")
        items <- headers.get("memorycache-items")
        totalLong <- stringToTryLong(total).toOption
        freeLong <- stringToTryLong(free).toOption
        itemsInt <- stringToTryInt(items).toOption
      } {
        memoryInfo.value = Some(MemoryInfo(totalLong, freeLong, itemsInt))
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

  def post[T](url: String, data: js.Any, uploadProgress: UploadProgress => Unit = _ => Unit)(callback: Response[T] => Unit = (_: Response[T]) => {})(implicit f: String => T): Unit = request(url, "POST", Some(data), uploadProgress = uploadProgress)(data => callback(data.to(f)))

  def postWithContentType[T](url: String, data: js.Any, contentType: String, uploadProgress: UploadProgress => Unit = _ => Unit)(callback: Response[T] => Unit = (_: Response[T]) => {})(implicit f: String => T): Unit = request(url, "POST", Some(data), Some(contentType), uploadProgress)(data => callback(data.to(f)))

  def postWithAutoContentType[T](url: String, data: js.Any, uploadProgress: UploadProgress => Unit = _ => Unit)(callback: Response[T] => Unit = (_: Response[T]) => {})(implicit f: String => T): Unit = request(url, "POST", Some(data), None, uploadProgress)(data => callback(data.to(f)))

}