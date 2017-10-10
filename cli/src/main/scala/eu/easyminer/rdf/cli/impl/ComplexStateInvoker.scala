package eu.easyminer.rdf.cli.impl

import java.io.File

import eu.easyminer.rdf.cli.Command.CommandException
import eu.easyminer.rdf.cli.ConsoleCommand.{ExecuteConsoleCommand, UnknownCommandException}
import eu.easyminer.rdf.cli.{Command, ConsoleCommand, ConsoleCommandInvoker}
import eu.easyminer.rdf.utils.BasicExtractors.AnyToInt
import eu.easyminer.rdf.utils.Printer
import org.apache.commons.cli
import org.apache.commons.cli.{MissingArgumentException, ParseException}

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

/**
  * Created by Vaclav Zeman on 9. 10. 2017.
  */
class ComplexStateInvoker private(implicit printer: Printer[String]) extends ConsoleCommandInvoker[ComplexState] {

  private def println(str: String): Unit = printer.println(str)

  private def missingArguments(min: Int, max: Option[Int] = None): Failure[Command[ComplexState]] = {
    val numOfMissingArguments = max match {
      case Some(max) if max == min => min.toString
      case Some(max) => s"$min to $max"
      case None => s"minimal $min"
    }
    Failure[Command[ComplexState]](new MissingArgumentException("Missing required arguments: " + numOfMissingArguments))
  }

  private def printState(state: ComplexState): Unit = {
    println("Loaded graphs: " + state.dataset.map(_.graphs.map(_.name).mkString(", ")).getOrElse("NONE"))
  }

  implicit private def intToOption(n: Int): Option[Int] = Some(n)

  implicit private def stringToCliOptionBuilder(x: String): cli.Option.Builder = cli.Option.builder(x)

  implicit private def stringToCliOption(x: String): cli.Option = x.build()

  override protected def postInvoke(command: Command[ComplexState], state: ComplexState): Unit = command match {
    case x: DatasetCommands.Save =>
      println(s"All graphs were successfully saved into '${x.getFile.getName}'.")
      printState(state)
    case x: DatasetCommands.SaveGraph =>
      println(s"Graph '${x.name}' was successfully saved into '${x.getFile.getName}'.")
      printState(state)
    case _: DatasetCommands.Filter =>
      println("Filter was successfully added.")
      printState(state)
    case _: DatasetCommands.ApplyPrefixes | _: DatasetCommands.LoadGraph | _: DatasetCommands.Clear => printState(state)
    case _ =>
  }

  protected val consoleCommands: Seq[ExecuteConsoleCommand[Command[ComplexState]]] = List(
    ConsoleCommand("exit")(_ => Success(new Command[ComplexState] {
      def execute(state: ComplexState): Try[ComplexState] = Success(state.terminate)
    })),
    ConsoleCommand("info")(_ => Success(new Command[ComplexState] {
      def execute(state: ComplexState): Try[ComplexState] = {
        printState(state)
        Success(state)
      }
    })),
    ConsoleCommand("data load")(_.getArgs match {
      case Array(path) => Try {
        val file = new File(path)
        new DatasetCommands.LoadGraph(file.getName.replaceAll("\\.[^.]+$", ""), file)
      }
      case Array(name, path) => Try(new DatasetCommands.LoadGraph(name, new File(path)))
      case _ => missingArguments(1, 2)
    }),
    ConsoleCommand("data prefixes")(_.getArgs match {
      case Array(path) => Try(new DatasetCommands.ApplyPrefixes(new File(path)))
      case _ => missingArguments(1)
    }),
    ConsoleCommand("data filter", "i") { cp =>
      val triplePatterns = cp.getArgs.grouped(4).map(_.take(3).map(x => if (x == "?") None else Some(x))).collect {
        case Array(s, p, o) => (s, p, o)
      }.toList
      Success(new DatasetCommands.Filter(triplePatterns, cp.hasOption("i")))
    },
    ConsoleCommand("data replace", "i", "s", "p", "o", "f".numberOfArgs(3).build()) { cp =>
      val triplePatterns = if (cp.hasOption("f")) {
        cp.getOptionValues("f").grouped(3).map(_.map(x => if (x == "?") None else Some(x))).collect {
          case Array(s, p, o) => (s, p, o)
        }.toList
      } else {
        Nil
      }
      cp.getArgs match {
        case Array(search, replacement) => Success(new DatasetCommands.Replace(search, replacement, cp.hasOption("s"), cp.hasOption("p"), cp.hasOption("o"), triplePatterns, cp.hasOption("i")))
        case _ => missingArguments(2)
      }
    },
    ConsoleCommand("data print", "i", "s", "p", "o", "f".numberOfArgs(3).build()) { cp =>
      val args = cp.getArgs
      val limit = args.lift(0).collect { case AnyToInt(x) => x }.getOrElse(20)
      val offset = (args.lift(1).collect { case AnyToInt(x) => x }.getOrElse(1) - 1) * limit
      val triplePatterns = if (cp.hasOption("f")) {
        cp.getOptionValues("f").grouped(3).map(_.map(x => if (x == "?") None else Some(x))).collect {
          case Array(s, p, o) => (s, p, o)
        }.toList
      } else {
        Nil
      }
      Success(new DatasetCommands.Print(cp.hasOption("s"), cp.hasOption("p"), cp.hasOption("o"), offset, limit, triplePatterns, cp.hasOption("i")))
    },
    ConsoleCommand("data save", "all") { cp =>
      cp.getArgs match {
        case Array(name, path) => Try {
          if (cp.hasOption("all")) new DatasetCommands.Save(name, new File(path)) else new DatasetCommands.SaveGraph(name, new File(path))
        }
        case _ => missingArguments(2)
      }
    },
    ConsoleCommand("data clear")(_ => Success[Command[ComplexState]](new DatasetCommands.Clear))
  )

  protected def printError(th: Throwable, state: ComplexState): Unit = th match {
    case x: ParseException => println("Input error: " + x.getMessage)
    case x: UnknownCommandException => println("Input error: " + x.getMessage)
    case x: CommandException => println("Error: " + x.getMessage)
    case x =>
      println("Fatal error: " + x.getMessage)
      x.getStackTrace.take(6).foreach(x => println(x.toString))
      if (x.getStackTrace.length > 6) println("...")
  }

}

object ComplexStateInvoker {

  def apply(printer: Printer[String]): ComplexStateInvoker = new ComplexStateInvoker()(printer)

}