package eu.easyminer.rdf.cli

import scala.util.Try

/**
  * Created by Vaclav Zeman on 7. 10. 2017.
  */
trait Command[T <: State] {

  def execute(state: T): Try[T]

}

object Command {

  class CommandException(msg: String) extends Exception(msg)

}
