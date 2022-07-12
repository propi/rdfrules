package com.github.propi.rdfrules.gui

import org.scalajs.dom.raw.{Blob, BlobPropertyBag}

import scala.scalajs.js
import scala.scalajs.js.JSON

/**
  * Created by Vaclav Zeman on 1. 2. 2019.
  */
object Downloader {

  def download(filename: String, data: String, mime: String): Unit = {
    val blob = new Blob(js.Array[js.Any](data), BlobPropertyBag(`type` = mime))
    Globals.saveAs(blob, filename)
    /*val el = document.createElement("a").asInstanceOf[Anchor]
    el.href = "data:" + mime + "," + URIUtils.encodeURIComponent(data)
    el.setAttribute("download", filename)
    el.style.display = "none"
    document.body.appendChild(el)
    el.click()
    document.body.removeChild(el)*/
  }

  def download(filename: String, data: js.Any): Unit = {
    download(filename, JSON.stringify(data, space = 2), "text/json;charset=utf-8")
  }

  def download(filename: String, data: String): Unit = {
    download(filename, data, "text/plain;charset=utf-8")
  }

  def download(filename: String, data: IterableOnce[String]): Unit = {
    download(filename, data.iterator.mkString("\n"), "text/plain;charset=utf-8")
  }

}