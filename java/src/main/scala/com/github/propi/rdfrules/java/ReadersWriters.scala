package com.github.propi.rdfrules.java

import com.github.propi.rdfrules.data.{RdfReader, RdfSource, RdfWriter}
import com.github.propi.rdfrules.ruleset.{RulesetSource, RulesetWriter}
import org.apache.jena.riot.{Lang, RDFFormat}

/**
  * Created by Vaclav Zeman on 6. 8. 2018.
  */
object ReadersWriters {

  def tsvReader: RdfReader = com.github.propi.rdfrules.data.tsvReader(RdfSource.Tsv)

  def tsvWriter: RdfWriter = com.github.propi.rdfrules.data.tsvWriter(RdfSource.Tsv)

  def jenaLangReader(lang: Lang): RdfReader = com.github.propi.rdfrules.data.jenaLangToRdfReader(lang)

  def jenaLangWriter(format: RDFFormat): RdfWriter = com.github.propi.rdfrules.data.jenaFormatToRdfWriter(format)

  def noRdfWriter: RdfWriter = RdfWriter.NoWriter

  def noRdfReader: RdfReader = RdfReader.NoReader

  def rulesJsonWriter: RulesetWriter[RulesetSource] = RulesetSource.Json

  def rulesTextWriter: RulesetWriter[RulesetSource] = RulesetSource.Text

  def rulesNoWriter: RulesetWriter[RulesetSource] = RulesetWriter.NoWriter

}
