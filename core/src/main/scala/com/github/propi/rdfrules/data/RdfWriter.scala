package com.github.propi.rdfrules.data

import java.io.File

import com.github.propi.rdfrules.data.ops.PrefixesOps
import com.github.propi.rdfrules.utils.OutputStreamBuilder

import scala.util.Try

/**
  * Created by Vaclav Zeman on 4. 10. 2017.
  */
trait RdfWriter {
  def writeToOutputStream(col: PrefixesOps[_], outputStreamBuilder: OutputStreamBuilder): Unit
}

object RdfWriter {

  implicit object NoWriter extends RdfWriter {
    def writeToOutputStream(col: PrefixesOps[_], outputStreamBuilder: OutputStreamBuilder): Unit = throw new IllegalStateException("No specified RdfWriter.")
  }

  //  private  /*extension match {
  //    case "nt" | "nq" | "ttl" | "trig" | "trix" | "tsv" => RdfSource(extension)
  //    case x => throw new IllegalArgumentException(s"Unsupported RDF format for streaming writing: $x")
  //  }*/

  def apply(file: File)(implicit sourceSettings: RdfSource.Settings): RdfWriter = {
    def restrictedRdfSource(extension: String): RdfSource = RdfSource(extension)

    val Ext2 = ".+[.](.+)[.](.+)".r
    val Ext1 = ".+[.](.+)".r
    file.getName.toLowerCase match {
      case Ext2(ext1, ext2) => Try(Compression(ext2)).toOption match {
        case Some(compression) => restrictedRdfSource(ext1).compressedBy(compression)
        case None => restrictedRdfSource(ext2)
      }
      case Ext1(ext1) => restrictedRdfSource(ext1)
      case _ => throw new IllegalArgumentException(s"No file extension to detect an RDF format.")
    }
  }

}