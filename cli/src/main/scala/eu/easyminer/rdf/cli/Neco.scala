package eu.easyminer.rdf.cli

import org.apache.commons.cli.{DefaultParser, Options}
import sun.tools.jar.CommandLine

import scala.util.Try

/**
  * Created by Vaclav Zeman on 9. 10. 2017.
  */
object Neco {

  /*sealed trait ArgumentPattern

  object ArgumentPattern {

    case class Required[T <: ArgumentPattern](argumentPattern: T) extends ArgumentPattern

    case class Empty(name: String) extends ArgumentPattern

    case class Value(name: String) extends ArgumentPattern

  }

  sealed trait ParameterPattern

  object ParameterPattern {

    case class Required[T <: ParameterPattern](parameterPattern: T) extends ParameterPattern

    object One extends ParameterPattern

  }

  sealed trait Argument

  object Argument {

    case class Empty(exists: Boolean) extends Argument

    case class ValueOpt(value: Option[String]) extends Argument

    case class Value(value: String) extends Argument

  }

  sealed trait Parameter

  object Parameter {

    case class ValueOpt(value: Option[String]) extends Parameter

    case class Value(value: String) extends Parameter

  }

  trait ArgumentBuilder[AP <: ArgumentPattern, A <: Argument] {
    def apply(pattern: AP, input: String): (A, String)
  }

  trait ParameterBuilder[PP <: ParameterPattern, P <: Parameter] {
    def apply(pattern: PP, input: String): (P, String)
  }*/

  def apply[T](cmd: String, options: Options)(f: CommandLine => Try[T]): String => Option[Try[T]] = { input =>
    if (input.startsWith(cmd)) {
      val args = input.stripPrefix(cmd).toArgs
      val parser = new DefaultParser
      Some(Try(f(parser.parse(options, args))).flatten)
    } else {
      None
    }
  }

  /*implicit val emptyArgToArg: ArgumentBuilder[ArgumentPattern.Empty, Argument.Empty] = new ArgumentBuilder[ArgumentPattern.Empty, Argument.Empty] {
    def apply(pattern: ArgumentPattern.Empty, input: String): (Argument.Empty, String) = ("(\\s+|^)-" + pattern.name + "(\\s+|$)").r.
  }*/

  private implicit class PimpedCommandInput(input: String) {
    def stripQuotes: String = input.stripPrefix("\"").stripSuffix("\"").replaceAll("\\\\\"", "\"")

    def toArgs: Array[String] = "\".+?(?<!\\\\)\"|\\S+".r.findAllMatchIn(input).map(_.matched.stripQuotes).toArray

    /*def toArgumentIterator: Iterator[(String, Option[String])] = new Iterator[(String, Option[String])] {
      private val it = toWordsIterator
      private var arg: Option[(String, Option[String])] = None
      private var nextArg: Option[String] = None

      private def isArg(x: String) = x.matches("-\\S+")

      def hasNext: Boolean = {
        if (arg.isEmpty) {
          if (nextArg.isDefined) {
            arg = Some(nextArg.get, None)
            nextArg = None
          }
          while (arg.isEmpty && it.hasNext) {
            val newArg = it.next()
            if (isArg(newArg)) arg = Some(newArg, None)
          }
          if (it.hasNext) {
            val newArg = it.next()
            if (isArg(newArg)) nextArg = Some(newArg) else arg = arg.map(x => (x._1, Some(newArg)))
          }
        }
        arg.isDefined
      }

      def next(): (String, Option[String]) = if (hasNext) {
        val x = arg.get
        arg = None
        x
      } else {
        Iterator.empty.next()
      }
    }*/
  }

  /*def apply[T, AP <: ArgumentPattern, PP <: ParameterPattern, A <: Argument, P <: Parameter](cmd: String, argumentPattern: AP, parameterPattern: PP)
                                                                                            (f: (A, P) => T)
                                                                                            (implicit argumentBuilder: ArgumentBuilder[AP, A], ppToParameter: ParameterBuilder[PP, P]): String => T = { input =>
    if (input.startsWith(cmd)) {
      val ArgumentsParameters = "((?:\\s+-\\S+(?:\\s+(?:\".*?\"|\\S+)))*)((?:\\s+(?:\".*?\"|\\S+))*)\\s*".r
      input.stripPrefix(cmd) match {
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
    }
  }*/

}
