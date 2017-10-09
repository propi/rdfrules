package eu.easyminer.rdf.cli

import eu.easyminer.rdf.cli.ConsoleCommand.Argument
import eu.easyminer.rdf.cli.ConsoleCommandPattern.ArgumentPattern

/**
  * Created by Vaclav Zeman on 8. 10. 2017.
  */
case class ConsoleCommandPattern(cmd: String, arguments: Set[ArgumentPattern], minParameters: Int, maxParameters: Option[Int])

object ConsoleCommandPattern {

  sealed trait ArgumentPattern {
    val name: String

    def matchArgument(argument: Argument): Boolean
  }

  sealed trait RequiredArgument extends ArgumentPattern

  case class EmptyArgument(name: String) extends ArgumentPattern {
    def matchArgument(argument: Argument): Boolean = argument.name == name && argument.value.isEmpty
  }

  case class ValueArgument(name: String) extends ArgumentPattern {
    def matchArgument(argument: Argument): Boolean = argument.name == name
  }

}
