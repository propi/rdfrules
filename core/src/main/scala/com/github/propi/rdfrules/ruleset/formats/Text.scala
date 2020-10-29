package com.github.propi.rdfrules.ruleset.formats

import java.io.{File, FileOutputStream, OutputStreamWriter, PrintWriter}

import com.github.propi.rdfrules.algorithm.consumer.PrettyPrintedWriter
import com.github.propi.rdfrules.data.{Prefix, TripleItem}
import com.github.propi.rdfrules.index.TripleItemIndex
import com.github.propi.rdfrules.rule.Rule
import com.github.propi.rdfrules.ruleset.{ResolvedRule, RulesetSource, RulesetWriter}
import com.github.propi.rdfrules.utils.{OutputStreamBuilder, Stringifier}

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 18. 4. 2018.
  */
trait Text {

  implicit def textRulesetWriter(source: RulesetSource.Text.type)(implicit stringifier: Stringifier[ResolvedRule]): RulesetWriter = (rules: Traversable[ResolvedRule], outputStreamBuilder: OutputStreamBuilder) => {
    val writer = new PrintWriter(new OutputStreamWriter(outputStreamBuilder.build, "UTF-8"))
    try {
      val prefixes = collection.mutable.Set.empty[Prefix]
      for (rule <- rules) {
        (rule.body :+ rule.head).iterator.flatMap(x => x.predicate :: List(x.subject, x.`object`).collect {
          case ResolvedRule.Atom.Item.Constant(x) => x
        }).collect {
          case x: TripleItem.PrefixedUri => x.prefix
        }.foreach(prefixes += _)
        writer.println(stringifier.toStringValue(rule))
      }
      if (prefixes.nonEmpty) {
        writer.println("")
        for (prefix <- prefixes) {
          writer.println(s"# @prefix ${prefix.prefix} : ${prefix.nameSpace}")
        }
      }
    } finally {
      writer.close()
    }
  }

  class TextPrettyPrintedWriter(file: File)(implicit mapper: TripleItemIndex, stringifier: Stringifier[ResolvedRule]) extends PrettyPrintedWriter {
    private val fos = new FileOutputStream(file)
    private val writer = new PrintWriter(new OutputStreamWriter(fos, "UTF-8"))

    def write(rule: Rule.Simple): Unit = writer.println(stringifier.toStringValue(ResolvedRule(rule)))

    def flush(): Unit = {
      writer.flush()
      fos.getFD.sync()
    }

    def close(): Unit = writer.close()
  }

  implicit def textPrettyPrintedWriter(source: RulesetSource.Text.type)(implicit mapper: TripleItemIndex, stringifier: Stringifier[ResolvedRule]): File => PrettyPrintedWriter = new TextPrettyPrintedWriter(_)

}