package eu.easyminer.rdf.cli

import org.apache.commons.cli
import org.apache.commons.cli.{CommandLine, DefaultParser, Options}

import scala.util.Try

/**
  * Created by Vaclav Zeman on 9. 10. 2017.
  */
object ConsoleCommand {

  type ExecuteConsoleCommand[T] = String => Option[Try[T]]

  class UnknownCommandException(cmd: String) extends Exception(s"Unknown command '$cmd'.")

  def apply[T](cmd: String, options: cli.Option*)(f: CommandLine => Try[T]): ExecuteConsoleCommand[T] = { input =>
    if (input.startsWith(cmd)) {
      val args = input.stripPrefix(cmd).toArgs
      val parser = new DefaultParser
      Some(Try(f(parser.parse(options.foldLeft(new Options)(_.addOption(_)), args))).flatten)
    } else {
      None
    }
  }

  private implicit class PimpedCommandInput(input: String) {
    def stripQuotes: String = input.stripPrefix("\"").stripSuffix("\"").replaceAll("\\\\\"", "\"")

    def toArgs: Array[String] = "\".+?(?<!\\\\)\"|\\S+".r.findAllMatchIn(input).map(_.matched.stripQuotes).toArray
  }

}
