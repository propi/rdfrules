package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.Workspace
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.rule.ResolvedRule
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 7. 8. 2018.
  */
class LoadRuleset(rulesetSource: LoadRuleset.RulesetSource, parallelism: Option[Int])(implicit debugger: Debugger) extends Task[Index, Ruleset] {
  val companion: TaskDefinition = LoadRuleset

  def execute(input: Index): Ruleset = {
    val ruleset = rulesetSource.load(input)
    parallelism.map(ruleset.setParallelism).getOrElse(ruleset).withDebugger
  }
}

object LoadRuleset extends TaskDefinition {
  val name: String = "LoadRuleset"

  sealed trait RulesetSource {
    def load(index: Index): Ruleset
  }

  object RulesetSource {
    case class File(path: String, format: Option[ExportRules.Format]) extends RulesetSource {
      def load(index: Index): Ruleset = format match {
        case Some(ExportRules.Format.NonBinary(source)) => Ruleset(index, Workspace.path(path))(source)
        case Some(ExportRules.Format.Cache) => Ruleset.fromCache(index, Workspace.path(path))
        case None => Ruleset(index, Workspace.path(path))
      }
    }

    case class Rules(rules: Iterable[ResolvedRule]) extends RulesetSource {
      def load(index: Index): Ruleset = Ruleset(index, rules)
    }
  }
}