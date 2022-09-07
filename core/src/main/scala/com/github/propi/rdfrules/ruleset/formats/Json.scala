package com.github.propi.rdfrules.ruleset.formats

import com.github.propi.rdfrules.rule.ResolvedRule
import com.github.propi.rdfrules.ruleset.{RulesetReader, RulesetSource, RulesetWriter}
import com.github.propi.rdfrules.utils.{ForEach, InputStreamBuilder, OutputStreamBuilder}
import spray.json.DefaultJsonProtocol._
import spray.json._

import java.io.{BufferedInputStream, OutputStreamWriter, PrintWriter}
import scala.io.Source
import scala.language.{implicitConversions, reflectiveCalls}

/**
  * Created by Vaclav Zeman on 18. 4. 2018.
  */
object Json {

  implicit def jsonRulesetWriter(source: RulesetSource.Json.type): RulesetWriter = (rules: ForEach[ResolvedRule], outputStreamBuilder: OutputStreamBuilder) => {
    val writer = new PrintWriter(new OutputStreamWriter(outputStreamBuilder.build, "UTF-8"))
    try {
      writer.println('[')
      rules.map(rule => rule.toJson.prettyPrint).foldLeft("") { (sep, rule) =>
        writer.println(sep + rule)
        ","
      }
      writer.println(']')
    } finally {
      writer.close()
    }
  }

  implicit def jsonRulesetReader(source: RulesetSource.Json.type): RulesetReader = (inputStreamBuilder: InputStreamBuilder) => {
    val is = new BufferedInputStream(inputStreamBuilder.build)
    val source = Source.fromInputStream(is, "UTF-8")
    try {
      ForEach.from(source.mkString.parseJson.convertTo[IndexedSeq[ResolvedRule]])
    } finally {
      source.close()
      is.close()
    }
  }

}