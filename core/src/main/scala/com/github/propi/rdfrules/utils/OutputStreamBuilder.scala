package com.github.propi.rdfrules.utils

import java.io.OutputStream

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 15. 1. 2018.
  */
trait OutputStreamBuilder {
  def build: OutputStream
}

object OutputStreamBuilder {

  implicit def apply(f: => OutputStream): OutputStreamBuilder = new OutputStreamBuilder {
    def build: OutputStream = f
  }

}