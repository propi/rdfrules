package com.github.propi.rdfrules.data.formats

import java.io.{BufferedInputStream, BufferedOutputStream}

import com.github.propi.rdfrules.data.Quad.QuadTraversableView
import com.github.propi.rdfrules.data.RdfSource.CompressedRdfSource
import com.github.propi.rdfrules.data.{Compression, RdfReader, RdfSource, RdfWriter}
import com.github.propi.rdfrules.utils.{InputStreamBuilder, OutputStreamBuilder}
import org.apache.commons.compress.compressors.bzip2.{BZip2CompressorInputStream, BZip2CompressorOutputStream}
import org.apache.commons.compress.compressors.gzip.{GzipCompressorInputStream, GzipCompressorOutputStream}
import com.github.propi.rdfrules.data.jenaFormatToRdfWriter
import RdfSource.PimpedRdfFormat

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 22. 5. 2019.
  */
trait Compressed {

  implicit def compressedToRdfReader(compressedRdfSource: CompressedRdfSource): RdfReader = (inputStreamBuilder: InputStreamBuilder) => compressedRdfSource match {
    case CompressedRdfSource.RdfFormat(format, compression) => compressedToRdfReader(CompressedRdfSource.Basic(RdfSource.JenaLang(format.getLang), compression)).fromInputStream(inputStreamBuilder)
    case CompressedRdfSource.Basic(rdfSource, Compression.BZ2) => rdfSource.fromInputStream(new BZip2CompressorInputStream(new BufferedInputStream(inputStreamBuilder.build)))
    case CompressedRdfSource.Basic(rdfSource, Compression.GZ) => rdfSource.fromInputStream(new GzipCompressorInputStream(new BufferedInputStream(inputStreamBuilder.build)))
  }

  private def compressedOutputStreamBuilder(outputStreamBuilder: OutputStreamBuilder, compression: Compression): OutputStreamBuilder = compression match {
    case Compression.BZ2 => new BZip2CompressorOutputStream(new BufferedOutputStream(outputStreamBuilder.build))
    case Compression.GZ => new GzipCompressorOutputStream(new BufferedOutputStream(outputStreamBuilder.build))
  }

  implicit def compressedToRdfWriter(compressedRdfSource: CompressedRdfSource): RdfWriter = (quads: QuadTraversableView, outputStreamBuilder: OutputStreamBuilder) => compressedRdfSource match {
    case CompressedRdfSource.Basic(rdfSource, compression) => rdfSource match {
      case x: RdfSource.JenaLang => compressedToRdfWriter(x.toRDFFormat.compressedBy(compression)).writeToOutputStream(quads, outputStreamBuilder)
      case RdfSource.Tsv => RdfSource.Tsv.writeToOutputStream(quads, compressedOutputStreamBuilder(outputStreamBuilder, compression))
    }
    case CompressedRdfSource.RdfFormat(format, compression) => format.writeToOutputStream(quads, compressedOutputStreamBuilder(outputStreamBuilder, compression))
  }

}