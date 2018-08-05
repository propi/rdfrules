package com.github.propi.rdfrules.ruleset.formats

import java.io.{OutputStreamWriter, PrintWriter}

import com.github.propi.rdfrules.ruleset.{ResolvedRule, RulesetSource, RulesetWriter}
import com.github.propi.rdfrules.utils.{OutputStreamBuilder, Stringifier}

/**
  * Created by Vaclav Zeman on 18. 4. 2018.
  */
object Text {

  implicit def textRulesetWriter(implicit stringifier: Stringifier[ResolvedRule]): RulesetWriter[RulesetSource.Text.type] = (rules: Traversable[ResolvedRule], outputStreamBuilder: OutputStreamBuilder) => {
    val writer = new PrintWriter(new OutputStreamWriter(outputStreamBuilder.build, "UTF-8"))
    try {
      for (rule <- rules) {
        writer.println(stringifier.toStringValue(rule))
      }
    } finally {
      writer.close()
    }
  }

}
