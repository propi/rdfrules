package com.github.propi.rdfrules.ruleset.formats

import com.github.propi.rdfrules.algorithm.consumer.RuleWriter
import com.github.propi.rdfrules.index.TripleItemIndex
import com.github.propi.rdfrules.rule.ResolvedRule
import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.ruleset.formats.Json._
import com.github.propi.rdfrules.ruleset.{RulesetReader, RulesetSource, RulesetWriter}
import com.github.propi.rdfrules.utils.{ForEach, InputStreamBuilder, OutputStreamBuilder}
import spray.json._

import java.io._
import scala.io.Source
import scala.language.{implicitConversions, reflectiveCalls}

/**
  * Created by Vaclav Zeman on 18. 4. 2018.
  */
object NDJson {

  implicit def ndjsonRulesetWriter(source: RulesetSource.NDJson.type): RulesetWriter = (rules: ForEach[ResolvedRule], outputStreamBuilder: OutputStreamBuilder) => {
    val writer = new PrintWriter(new OutputStreamWriter(outputStreamBuilder.build, "UTF-8"))
    try {
      rules.map(rule => rule.toJson.compactPrint).foreach(writer.println)
    } finally {
      writer.close()
    }
  }

  implicit def ndjsonRulesetReader(source: RulesetSource.NDJson.type): RulesetReader = (inputStreamBuilder: InputStreamBuilder) => (f: ResolvedRule => Unit) => {
    val is = new BufferedInputStream(inputStreamBuilder.build)
    val source = Source.fromInputStream(is, "UTF-8")
    try {
      source.getLines().map(_.parseJson.convertTo[ResolvedRule]).foreach(f)
    } finally {
      source.close()
      is.close()
    }
  }

  class NDJsonPrettyPrintedWriter(file: File)(implicit mapper: TripleItemIndex) extends RuleWriter {
    private val fos = new FileOutputStream(file)
    private val writer = new PrintWriter(new OutputStreamWriter(fos, "UTF-8"))

    def write(rule: FinalRule): Unit = writer.println(ResolvedRule(rule).toJson.compactPrint)

    def flush(): Unit = {
      writer.flush()
      fos.getFD.sync()
    }

    def close(): Unit = writer.close()
  }

  implicit def jsonPrettyPrintedWriter(source: RulesetSource.NDJson.type)(implicit mapper: TripleItemIndex): File => RuleWriter = new NDJsonPrettyPrintedWriter(_)

}