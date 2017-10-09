package eu.easyminer.rdf.cli.impl

import java.io.File

import eu.easyminer.rdf.cli.Command.CommandException
import eu.easyminer.rdf.cli.ConsoleCommand.{CommandInputException, UnknownCommand}
import eu.easyminer.rdf.cli.ConsoleCommandPattern.EmptyArgument
import eu.easyminer.rdf.cli.{Command, ConsoleCommand, ConsoleCommandInvoker, ConsoleCommandPattern}
import eu.easyminer.rdf.utils.BasicExtractors.AnyToInt
import eu.easyminer.rdf.utils.Printer

import scala.language.implicitConversions
import scala.util.Try

/**
  * Created by Vaclav Zeman on 9. 10. 2017.
  */
class ComplexStateInvoker(implicit printer: Printer[String]) extends ConsoleCommandInvoker[ComplexState] {

  private def println(str: String): Unit = printer.println(str)

  implicit private def intToOption(n: Int): Option[Int] = Some(n)

  implicit val consoleCommandPatterns: Seq[ConsoleCommandPattern] = List(
    ConsoleCommandPattern("load graph", Set.empty, 1, 2),
    ConsoleCommandPattern("apply prefixes", Set.empty, 1, 1),
    ConsoleCommandPattern("filter data", Set(EmptyArgument("i")), 3, None),
    ConsoleCommandPattern("print data", Set(EmptyArgument("s"), EmptyArgument("p"), EmptyArgument("o")), 0, 2),
    ConsoleCommandPattern("save graph", Set.empty, 2, 2),
    ConsoleCommandPattern("save dataset", Set.empty, 2, 2)
  )

  protected def printError(th: Throwable, state: ComplexState): Unit = th match {
    case x: CommandInputException => println("Input error: " + x.getMessage)
    case x: CommandException => println("Error: " + x.getMessage)
    case x =>
      println("Fatal error: " + x.getMessage)
      x.getStackTrace.take(6).foreach(x => println(x.toString))
      if (x.getStackTrace.length > 6) println("...")
  }

  protected def consoleCommandToCommand(consoleCommand: ConsoleCommand): Try[Command[ComplexState]] = Try {
    consoleCommand match {
      case ConsoleCommand("load graph", _, parameters) =>
        if (parameters.size == 1) {
          val file = new File(parameters.head)
          new DatasetCommands.LoadGraph(file.getName.replaceAll("\\.[^.]+$", ""), file)
        } else {
          new DatasetCommands.LoadGraph(parameters(0), new File(parameters(1)))
        }
      case ConsoleCommand("apply prefixes", _, IndexedSeq(path)) => new DatasetCommands.ApplyPrefixes(new File(path))
      case ConsoleCommand("filter data", arguments, parameters) =>
        val triplePatterns = parameters.grouped(4).map(_.take(3).map(x => if (x == "?") None else Some(x))).collect {
          case IndexedSeq(s, p, o) => (s, p, o)
        }.toList
        new DatasetCommands.Filter(triplePatterns, arguments.exists(_.name == "i"))
      case ConsoleCommand("print data", arguments, parameters) =>
        val limit = parameters.lift(0).collect { case AnyToInt(x) => x }.getOrElse(20)
        val offset = (parameters.lift(1).collect { case AnyToInt(x) => x }.getOrElse(1) - 1) * limit
        new DatasetCommands.Print(arguments.exists(_.name == "s"), arguments.exists(_.name == "p"), arguments.exists(_.name == "o"), offset, limit)
      case ConsoleCommand("save graph", _, IndexedSeq(name, path)) => new DatasetCommands.SaveGraph(name, new File(path))
      case ConsoleCommand("save dataset", _, IndexedSeq(name, path)) => new DatasetCommands.Save(name, new File(path))
      case _ => throw new UnknownCommand(consoleCommand.name + " args: " + consoleCommand.arguments + ", params: " + consoleCommand.parameters)
    }
  }

}
