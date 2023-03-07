package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.data.Compression
import com.github.propi.rdfrules.http.Workspace
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.index.IndexPart
import com.github.propi.rdfrules.rule.ResolvedRule
import com.github.propi.rdfrules.ruleset
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 7. 8. 2018.
  */
class LoadRuleset(rulesetSource: LoadRuleset.RulesetSource, parallelism: Option[Int])(implicit debugger: Debugger) extends Task[IndexPart, Ruleset] {
  val companion: TaskDefinition = LoadRuleset

  def execute(input: IndexPart): Ruleset = {
    val ruleset = rulesetSource.load(input)
    parallelism.map(ruleset.setParallelism).getOrElse(ruleset).withDebugger
  }
}

object LoadRuleset extends TaskDefinition {
  val name: String = "LoadRuleset"

  sealed trait RulesetSource {
    def load(index: IndexPart): Ruleset
  }

  object RulesetSource {
    case class File(path: String, format: ruleset.RulesetSource) extends RulesetSource {
      def load(index: IndexPart): Ruleset = Ruleset(index, Workspace.path(path))(Compression.fromPath(path) match {
        case Some(compression) => format.compressedBy(compression)
        case None => format
      })
    }

    case class Rules(rules: Iterable[ResolvedRule]) extends RulesetSource {
      def load(index: IndexPart): Ruleset = Ruleset(index, rules)
    }
  }
}