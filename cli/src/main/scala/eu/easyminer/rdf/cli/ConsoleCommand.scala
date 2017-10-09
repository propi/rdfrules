package eu.easyminer.rdf.cli

import eu.easyminer.rdf.cli.ConsoleCommand.Argument
import eu.easyminer.rdf.cli.ConsoleCommandPattern.{ArgumentPattern, RequiredArgument}

import scala.util.Try

/**
  * Created by Vaclav Zeman on 7. 10. 2017.
  */
case class ConsoleCommand(name: String, arguments: IndexedSeq[Argument], parameters: IndexedSeq[String])

object ConsoleCommand {

  case class Argument(name: String, value: String)

  def apply(string: String)(implicit commandPatterns: Seq[ConsoleCommandPattern]): Try[ConsoleCommand] = Try {
    commandPatterns.find(cp => string.startsWith(cp.cmd)).map { cp =>
      val ArgumentsParameters = "((?:\\s+-\\S+(?:\\s+(?:\".*?\"|\\S+)))*)((?:\\s+(?:\".*?\"|\\S+))*)\\s*".r
      string.stripPrefix(cp.cmd) match {
        case ArgumentsParameters(strArguments, strParameters) =>
          val arguments = "\\s+-(\\S+)(?:\\s+(\".*?\"|\\S+))??(?=\\s+-\\S+|\\s*$)".r
            .findAllMatchIn(strArguments)
            .map(m => Argument(m.group(1), if (m.group(2) == null) "" else m.group(2)))
            .toIndexedSeq
          val parameters = "(?:\\s+(\".*?\"|\\S+))".r
            .findAllMatchIn(strParameters)
            .map(m => m.group(1))
            .toIndexedSeq
          val filteredArguments = cp.arguments.iterator.flatMap { ap =>
            val matchedArguments = arguments.filter(ap.matchArgument)
            if (matchedArguments.isEmpty && ap.isInstanceOf[RequiredArgument]) {
              throw new MissingArgumentException(ap)
            }
            matchedArguments
          }.toIndexedSeq
          if (parameters.size < cp.minParameters || cp.maxParameters.exists(parameters.size > _)) {
            throw new NumberOfParametersException(cp.minParameters, cp.maxParameters, parameters.size)
          }
          ConsoleCommand(cp.cmd, filteredArguments, parameters)
        case _ =>
          ConsoleCommand(cp.cmd, IndexedSeq.empty, IndexedSeq.empty)
      }
    }.getOrElse(throw new UnknownCommand(string))
  }

  abstract class CommandInputException(msg: String) extends Exception(msg)

  class UnknownCommand(cmd: String) extends CommandInputException(s"Unknown command '$cmd'.")

  class MissingArgumentException(argumentPattern: ArgumentPattern) extends CommandInputException(s"Missing argument '${argumentPattern.name}'.")

  class NumberOfParametersException(min: Int, max: Option[Int], given: Int) extends CommandInputException(s"Invalid number of parameters: expected $min to ${max.map(_.toString).getOrElse("infinity")}, given $given.")

}