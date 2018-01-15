package eu.easyminer.rdf.cli.impl

import java.io.File

import eu.easyminer.rdf.cli.Command.CommandException
import eu.easyminer.rdf.printer.Printer

import scala.util.{Failure, Success, Try}

/**
  * Created by Vaclav Zeman on 11. 10. 2017.
  */
object IndexCommands {

  class Save(directory: File)(implicit msgPrinter: Printer[String]) extends ComplexStateCommand {
    def execute(state: ComplexState): Try[ComplexState] = if (state.dataset.isEmpty) {
      Failure(new CommandException("No dataset to be indexed."))
    } else {
      ComplexIndex(directory, state.dataset.get).map(state.withIndex)
    }
  }

  class Load(directory: File, forcedTripleHashIndex: Boolean, forcedTripleItemToInt: Boolean, forcedTripleIntToItem: Boolean) extends ComplexStateCommand {
    def execute(state: ComplexState): Try[ComplexState] = {
      ComplexIndex(directory, forcedTripleHashIndex, forcedTripleItemToInt, forcedTripleIntToItem)
        .map(state.withIndex)
        .map(Success.apply)
        .getOrElse(Failure(new CommandException(s"Invalid index '${directory.getAbsolutePath}'.")))
    }
  }

  class Export(file: File, graphName: Option[String]) extends ComplexStateCommand {
    def execute(state: ComplexState): Try[ComplexState] = if (state.index.isEmpty) {
      Failure(new CommandException("No index was loaded."))
    } else {
      val saveCommand = graphName match {
        case Some(graphName) => new DatasetCommands.Save(graphName, file, false)
        case None => new DatasetCommands.Save("all", file, true)
      }
      saveCommand.execute(state.withDataset(state.index.get.dataset)).map(_ => state)
    }
  }

  class Print(showSubjects: Boolean, showPredicates: Boolean, showObjects: Boolean, offset: Int, limit: Int, patterns: List[(Option[String], Option[String], Option[String])], inverse: Boolean)(implicit msgPrinter: Printer[String]) extends ComplexStateCommand {
    def execute(state: ComplexState): Try[ComplexState] = if (state.index.isEmpty) {
      Failure(new CommandException("No index was loaded."))
    } else {
      val tempState = state.withDataset(state.index.get.dataset)
      new DatasetCommands.Print(showSubjects, showPredicates, showObjects, offset, limit, patterns, inverse).execute(tempState)
      Success(state)
    }
  }

}
