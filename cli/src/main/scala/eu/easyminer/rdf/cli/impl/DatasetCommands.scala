package eu.easyminer.rdf.cli.impl

import java.io.{File, FileOutputStream}

import eu.easyminer.rdf.cli.Command.CommandException
import eu.easyminer.rdf.cli.TripleItemComparator.compare
import eu.easyminer.rdf.data._
import eu.easyminer.rdf.utils.{Printer, Stringifier}
import eu.easyminer.rdf.utils.TraversableViewExtension._

import scala.util.{Failure, Success, Try}

/**
  * Created by Vaclav Zeman on 7. 10. 2017.
  */
object DatasetCommands {

  class LoadGraph(name: String, file: File) extends ComplexStateCommand {
    def execute(state: ComplexState): Try[ComplexState] = if (name.isEmpty) {
      Failure(new CommandException("Graph name must not be empty."))
    } else if (state.dataset.graphs.exists(_.name == name)) {
      Failure(new CommandException(s"Graph with this name '$name' already exists in the loaded dataset."))
    } else if (!file.isFile || !file.canRead) {
      Failure(new CommandException(s"File '${file.getAbsolutePath}' is not readable."))
    } else {
      Try {
        state.withDataset(state.dataset + Graph(name, file))
      }
    }
  }

  class ApplyPrefixes(file: File)(implicit msgPrinter: Printer[String]) extends ComplexStateCommand {
    def execute(state: ComplexState): Try[ComplexState] = if (!file.isFile || !file.canRead) {
      Failure(new CommandException(s"File '${file.getAbsolutePath}' is not readable."))
    } else {
      Try {
        val prefixes = Prefix(file)
        msgPrinter.println("Loaded prefixes: " + prefixes.size)
        state.withDataset(state.dataset.withPrefixes(prefixes))
      }
    }
  }

  class Filter(patterns: List[(Option[String], Option[String], Option[String])], inverse: Boolean) extends ComplexStateCommand {
    def execute(state: ComplexState): Try[ComplexState] = Try {
      state.withDataset(
        state.dataset.withFilter { triple =>
          val isMatched = patterns.exists { case (s, p, o) =>
            s.forall(compare(triple.subject, _)) && p.forall(compare(triple.predicate, _)) && o.forall(compare(triple.`object`, _))
          }
          if (inverse) !isMatched else isMatched
        }
      )
    }
  }

  class Print(showSubjects: Boolean, showPredicates: Boolean, showObjects: Boolean, offset: Int, limit: Int)(implicit msgPrinter: Printer[String]) extends ComplexStateCommand {
    def execute(state: ComplexState): Try[ComplexState] = {
      val triples = state.dataset.toTriples
      val until = offset + limit
      val x = (showSubjects, showPredicates, showObjects) match {
        case (true, true, true) => triples.map(triple => Stringifier(triple)).slice(offset, until)
        case (true, true, false) => triples.map(triple => Stringifier(Triple(triple.subject, triple.predicate, TripleItem.Text("?")))).sliceDistinct(offset, until)
        case (true, false, true) => triples.map(triple => Stringifier(Triple(triple.subject, TripleItem.LongUri("?"), triple.`object`))).sliceDistinct(offset, until)
        case (true, false, false) => triples.map(triple => Stringifier(Triple(triple.subject, TripleItem.LongUri("?"), TripleItem.Text("?")))).sliceDistinct(offset, until)
        case (false, true, true) => triples.map(triple => Stringifier(Triple(TripleItem.LongUri("?"), triple.predicate, triple.`object`))).sliceDistinct(offset, until)
        case (false, true, false) => triples.map(triple => Stringifier(Triple(TripleItem.LongUri("?"), triple.predicate, TripleItem.Text("?")))).sliceDistinct(offset, until)
        case (false, false, true) => triples.map(triple => Stringifier(Triple(TripleItem.LongUri("?"), TripleItem.LongUri("?"), triple.`object`))).sliceDistinct(offset, until)
        case (false, false, false) => triples.map(triple => Stringifier(triple)).slice(offset, until)
      }
      x.foreach(msgPrinter.println)
      Success(state)
    }
  }

  class SaveGraph(name: String, file: File) extends ComplexStateCommand {
    def execute(state: ComplexState): Try[ComplexState] = if (!state.dataset.graphs.exists(_.name == name)) {
      Failure(new CommandException(s"Graph with this name '$name' does not exist."))
    } else {
      Try {
        val outputFile = if (file.getName.endsWith(".ttl")) {
          file
        } else {
          new File(file.getAbsolutePath + ".ttl")
        }
        RdfSource.Ttl.writeToOutputStream(state.dataset.graphs.find(_.name == name).get, new FileOutputStream(outputFile))
        state.withDataset(state.dataset + Graph(name, outputFile))
      }
    }
  }

  class Save(name: String, file: File) extends ComplexStateCommand {
    def execute(state: ComplexState): Try[ComplexState] = if (name.isEmpty) {
      Failure(new CommandException("Graph name must not be empty."))
    } else {
      new SaveGraph(name, file).execute(state.withDataset(Dataset(Graph(name, state.dataset.toTriples))))
    }
  }

}
