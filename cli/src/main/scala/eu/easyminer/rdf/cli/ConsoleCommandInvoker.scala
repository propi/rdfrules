package eu.easyminer.rdf.cli

import scala.util.{Failure, Success, Try}

/**
  * Created by Vaclav Zeman on 8. 10. 2017.
  */
trait ConsoleCommandInvoker[T <: State] {

  implicit val consoleCommandPatterns: Seq[ConsoleCommandPattern]

  protected def printError(th: Throwable, state: T): Unit

  protected def preInvoke(command: Command[T], state: T): Unit = {}

  protected def postInvoke(command: Command[T], state: T): Unit = {}

  protected def consoleCommandToCommand(consoleCommand: ConsoleCommand): Try[Command[T]]

  @scala.annotation.tailrec
  final def processInput(state: T, readLine: => String): Unit = {
    val newState = ConsoleCommand(readLine) match {
      case Failure(th) =>
        printError(th, state)
        state
      case Success(consoleCommand) => consoleCommandToCommand(consoleCommand) match {
        case Failure(th) =>
          printError(th, state)
          state
        case Success(command) =>
          preInvoke(command, state)
          command.execute(state) match {
            case Failure(th) =>
              printError(th, state)
              state
            case Success(newState) =>
              postInvoke(command, newState)
              newState
          }
      }
    }
    if (!newState.isTerminated) processInput(newState, readLine)
  }

}
