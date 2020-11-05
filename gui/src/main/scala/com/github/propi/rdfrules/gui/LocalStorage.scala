package com.github.propi.rdfrules.gui

import org.scalajs.dom._

object LocalStorage {

  private val storage = window.localStorage

  def put[T](key: String, value: T)(implicit f: T => String): Unit = storage.setItem(key, f(value))

  def get[T](key: String)(implicit f: String => Option[T]): Option[T] = Option(storage.getItem(key)).flatMap(f)

  def remove(key: String): Unit = storage.removeItem(key)

  def clear(): Unit = storage.clear()

}