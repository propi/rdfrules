package com.github.propi.rdfrules.ruleset.formats

import com.github.propi.rdfrules.algorithm.consumer.RuleIO
import com.github.propi.rdfrules.data.{Prefix, TripleItem}
import com.github.propi.rdfrules.index.TripleItemIndex
import com.github.propi.rdfrules.rule.ResolvedAtom.ResolvedItem
import com.github.propi.rdfrules.rule.ResolvedRule
import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.ruleset.{RulesetSource, RulesetWriter}
import com.github.propi.rdfrules.utils.{ForEach, OutputStreamBuilder, Stringifier}

import java.io.{File, FileOutputStream, OutputStreamWriter, PrintWriter}
import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 18. 4. 2018.
  */
object Text {

  implicit def textRulesetWriter(source: RulesetSource.Text.type)(implicit stringifier: Stringifier[ResolvedRule]): RulesetWriter = (rules: ForEach[ResolvedRule], outputStreamBuilder: OutputStreamBuilder) => {
    val writer = new PrintWriter(new OutputStreamWriter(outputStreamBuilder.build, "UTF-8"))
    try {
      val prefixes = collection.mutable.Set.empty[Prefix]
      for (rule <- rules) {
        (rule.body :+ rule.head).iterator.flatMap(x => x.predicate :: List(x.subject, x.`object`).collect {
          case ResolvedItem.Constant(x) => x
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

  private class TextIO(file: File)(implicit mapper: TripleItemIndex, stringifier: Stringifier[ResolvedRule]) extends RuleIO {
    def writer[T](f: RuleIO.Writer => T): T = {
      val fos = new FileOutputStream(file)
      val writer = new PrintWriter(new OutputStreamWriter(fos, "UTF-8"))
      try {
        f(new RuleIO.Writer {
          def write(rule: FinalRule): Unit = writer.println(stringifier.toStringValue(ResolvedRule(rule)))

          def flush(): Unit = {
            writer.flush()
            fos.getFD.sync()
          }
        })
      } finally {
        writer.close()
      }
    }

    def reader[T](f: RuleIO.Reader => T): T = throw new UnsupportedOperationException("Text format is not parsable")
  }

  implicit def textWriter(source: RulesetSource.Text.type)(implicit mapper: TripleItemIndex, stringifier: Stringifier[ResolvedRule]): File => RuleIO = new TextIO(_)

}