package com.github.propi.rdfrules.data

import com.github.propi.rdfrules.data.Quad.QuadTraversableView
import com.github.propi.rdfrules.utils.InputStreamBuilder

import java.io.{File, FileInputStream}
import scala.language.implicitConversions
import scala.util.Try

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

  def apply(file: File)(implicit sourceSettings: RdfSource.Settings): RdfReader = apply(file.getName)

  def apply(path: String)(implicit sourceSettings: RdfSource.Settings): RdfReader = {
    val Ext2 = ".+[.](.+)[.](.+)".r
    val Ext1 = ".+[.](.+)".r
    path.toLowerCase match {
      case Ext2(ext1, ext2) => Try(Compression(ext2)).toOption match {
        case Some(compression) => RdfSource(ext1).compressedBy(compression)
        case None => RdfSource(ext2)
      }
      case Ext1(ext1) => RdfSource(ext1)
      case _ => throw new IllegalArgumentException(s"No file extension to detect an RDF format.")
    }
  }

}