package com.github.propi.rdfrules.ruleset.formats

import com.github.propi.rdfrules.rule.ResolvedRule
import com.github.propi.rdfrules.ruleset.{RulesetReader, RulesetSource, RulesetWriter}
import com.github.propi.rdfrules.serialization.RuleSerialization.{resolvedRuleDeserializer, resolvedRuleSerializer}
import com.github.propi.rdfrules.utils.serialization.{Deserializer, Serializer}
import com.github.propi.rdfrules.utils.{ForEach, InputStreamBuilder, OutputStreamBuilder}

import scala.language.{implicitConversions, reflectiveCalls}

/**
  * Created by Vaclav Zeman on 18. 4. 2018.
  */
object Cache {

  implicit def cacheRulesetWriter(source: RulesetSource.Cache.type): RulesetWriter = (rules: ForEach[ResolvedRule], outputStreamBuilder: OutputStreamBuilder) => {
    Serializer.serializeToOutputStream[ResolvedRule](outputStreamBuilder.build) { writer =>
      rules.foreach(writer.write)
    }
  }

  implicit def cacheRulesetReader(source: RulesetSource.Cache.type): RulesetReader = (inputStreamBuilder: InputStreamBuilder) => (f: ResolvedRule => Unit) => {
    Deserializer.deserializeFromInputStream[ResolvedRule, Unit](inputStreamBuilder.build) { reader =>
      Iterator.continually(reader.read()).takeWhile(_.isDefined).map(_.get).foreach(f)
    }
  }

}