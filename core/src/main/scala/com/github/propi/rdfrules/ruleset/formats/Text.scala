package com.github.propi.rdfrules.ruleset.formats

import java.io.{OutputStreamWriter, PrintWriter}

import com.github.propi.rdfrules.data.{Prefix, TripleItem}
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
          case x: TripleItem.PrefixedUri => x.toPrefix
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

}