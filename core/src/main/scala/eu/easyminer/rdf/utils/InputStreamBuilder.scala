package eu.easyminer.rdf.utils

import java.io.InputStream

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 15. 1. 2018.
  */
trait InputStreamBuilder {
  def build: InputStream
}

object InputStreamBuilder {

  implicit def apply(f: => InputStream): InputStreamBuilder = new InputStreamBuilder {
    def build: InputStream = f
  }

}
