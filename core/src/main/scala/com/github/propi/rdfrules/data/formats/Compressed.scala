package com.github.propi.rdfrules.data.formats

import java.io.{BufferedInputStream, BufferedOutputStream}
import com.github.propi.rdfrules.data.RdfSource.{CompressedRdfSource, PimpedRdfFormat}
import com.github.propi.rdfrules.data.ops.PrefixesOps
import com.github.propi.rdfrules.data.{Compression, RdfReader, RdfSource, RdfWriter, jenaFormatToRdfWriter}
import com.github.propi.rdfrules.rule.ResolvedRule
import com.github.propi.rdfrules.ruleset.RulesetSource.CompressedRulesetSource
import com.github.propi.rdfrules.ruleset.{RulesetReader, RulesetWriter}
import com.github.propi.rdfrules.utils.{ForEach, InputStreamBuilder, OutputStreamBuilder}
import org.apache.commons.compress.compressors.bzip2.{BZip2CompressorInputStream, BZip2CompressorOutputStream}
import org.apache.commons.compress.compressors.gzip.{GzipCompressorInputStream, GzipCompressorOutputStream}

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 22. 5. 2019.
  */
trait Compressed {

  implicit def compressedToRulesetReader(compressedRulesetSource: CompressedRulesetSource): RulesetReader = (inputStreamBuilder: InputStreamBuilder) => compressedRulesetSource match {
    case CompressedRulesetSource(rulesetSource, Compression.BZ2) => rulesetSource.fromInputStream(new BZip2CompressorInputStream(new BufferedInputStream(inputStreamBuilder.build)))
    case CompressedRulesetSource(rulesetSource, Compression.GZ) => rulesetSource.fromInputStream(new GzipCompressorInputStream(new BufferedInputStream(inputStreamBuilder.build)))
  }

  implicit def compressedToRulesetWriter(compressedRulesetSource: CompressedRulesetSource): RulesetWriter = (rules: ForEach[ResolvedRule], outputStreamBuilder: OutputStreamBuilder) => {
    compressedRulesetSource.rulesetSource.writeToOutputStream(rules, compressedOutputStreamBuilder(outputStreamBuilder, compressedRulesetSource.compression))
  }

  implicit def compressedToRdfReader(compressedRdfSource: CompressedRdfSource): RdfReader = (inputStreamBuilder: InputStreamBuilder) => compressedRdfSource match {
    case CompressedRdfSource.RdfFormat(format, compression) => compressedToRdfReader(CompressedRdfSource.Basic(RdfSource.JenaLang(format.getLang), compression)).fromInputStream(inputStreamBuilder)
    case CompressedRdfSource.Basic(rdfSource, Compression.BZ2) => rdfSource.fromInputStream(new BZip2CompressorInputStream(new BufferedInputStream(inputStreamBuilder.build)))
    case CompressedRdfSource.Basic(rdfSource, Compression.GZ) => rdfSource.fromInputStream(new GzipCompressorInputStream(new BufferedInputStream(inputStreamBuilder.build)))
  }

  private def compressedOutputStreamBuilder(outputStreamBuilder: OutputStreamBuilder, compression: Compression): OutputStreamBuilder = compression match {
    case Compression.BZ2 => new BZip2CompressorOutputStream(new BufferedOutputStream(outputStreamBuilder.build))
    case Compression.GZ => new GzipCompressorOutputStream(new BufferedOutputStream(outputStreamBuilder.build))
  }

  implicit def compressedToRdfWriter(compressedRdfSource: CompressedRdfSource): RdfWriter = (col: PrefixesOps[_], outputStreamBuilder: OutputStreamBuilder) => compressedRdfSource match {
    case CompressedRdfSource.Basic(rdfSource, compression) => rdfSource match {
      case x: RdfSource.JenaLang => compressedToRdfWriter(x.toRDFFormat.compressedBy(compression)).writeToOutputStream(col, outputStreamBuilder)
      case RdfSource.Tsv => RdfSource.Tsv.writeToOutputStream(col, compressedOutputStreamBuilder(outputStreamBuilder, compression))
      case RdfSource.Sql => RdfSource.Sql.writeToOutputStream(col, compressedOutputStreamBuilder(outputStreamBuilder, compression))
    }
    case CompressedRdfSource.RdfFormat(format, compression) => format.writeToOutputStream(col, compressedOutputStreamBuilder(outputStreamBuilder, compression))
  }

}