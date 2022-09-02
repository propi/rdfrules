package com.github.propi.rdfrules.gui

import org.scalajs.dom._

object LocalStorage {

  private val storage = window.localStorage

  def put(key: String, value: String): Unit = storage.setItem(key, value)

  def get(key: String): Option[String] = Option(storage.getItem(key))

  def remove(key: String): Unit = storage.removeItem(key)

  def clear(): Unit = storage.clear()

}