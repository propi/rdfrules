package eu.easyminer.rdf.cli

import eu.easyminer.rdf.cli.ConsoleCommand.ExecuteConsoleCommand

import scala.util.{Failure, Success}

/**
  * Created by Vaclav Zeman on 8. 10. 2017.
  */
trait ConsoleCommandInvoker[T <: State] {

  protected val consoleCommands: Seq[ExecuteConsoleCommand[Command[T]]]

  protected def printError(th: Throwable, state: T): Unit

  protected def preInvoke(command: Command[T], state: T): Unit = {}

  protected def postInvoke(command: Command[T], state: T): Unit = {}

  @scala.annotation.tailrec
  final def processInput(state: T, readLine: => String): Unit = {
    val consoleInput = readLine
    val newState = consoleCommands.iterator.map(_ (consoleInput)).collectFirst {
      case Some(x) => x
    } match {
      case Some(Success(command)) =>
        preInvoke(command, state)
        command.execute(state) match {
          case Failure(th) =>
            printError(th, state)
            state
          case Success(newState) =>
            postInvoke(command, newState)
            newState
        }
      case Some(Failure(th)) =>
        printError(th, state)
        state
      case None =>
        printError(new ConsoleCommand.UnknownCommandException(consoleInput), state)
        state
    }
    if (!newState.isTerminated) processInput(newState, readLine)
  }

}
