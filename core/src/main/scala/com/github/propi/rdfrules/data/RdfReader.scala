package com.github.propi.rdfrules.data

import java.io.{File, FileInputStream}

import com.github.propi.rdfrules.data.Quad.QuadTraversableView
import com.github.propi.rdfrules.utils.InputStreamBuilder

/**
  * Created by Vaclav Zeman on 27. 6. 2017.
  */
trait RdfReader {
  def fromInputStream(inputStreamBuilder: InputStreamBuilder): QuadTraversableView

  def fromFile(file: File): QuadTraversableView = fromInputStream(new FileInputStream(file))
}

object RdfReader {

  implicit object NoReader extends RdfReader {
    def fromInputStream(inputStreamBuilder: InputStreamBuilder): QuadTraversableView = throw new IllegalStateException("No specified RdfReader.")
  }

  def apply(file: File): RdfReader = RdfSource(file.getName.replaceAll(".*\\.", "")) match {
    case RdfSource.JenaLang(lang) => lang
    case x: RdfSource.Tsv.type => x
  }

}
