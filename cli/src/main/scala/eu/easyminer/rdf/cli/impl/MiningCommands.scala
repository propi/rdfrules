package eu.easyminer.rdf.cli.impl

import eu.easyminer.rdf.cli.Command.CommandException
import eu.easyminer.rdf.rule.Threshold

import scala.util.{Failure, Try}

/**
  * Created by Vaclav Zeman on 13. 10. 2017.
  */
object MiningCommands {

  class Mine(thresholds: Seq[Threshold], rulePatterns: Seq[String], predicates: Seq[String], predicatesInversed: Boolean, withInstances: Boolean, withoutDuplicitPredicates: Boolean) extends ComplexStateCommand {
    def execute(state: ComplexState): Try[ComplexState] = if (state.index.isEmpty) {
      Failure(new CommandException("No index was loaded."))
    } else {

    }
  }

}
