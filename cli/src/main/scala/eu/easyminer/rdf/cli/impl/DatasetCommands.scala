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
    } else if (state.dataset.exists(_.graphs.exists(_.name == name))) {
      Failure(new CommandException(s"Graph with this name '$name' already exists in the loaded dataset."))
    } else if (!file.isFile || !file.canRead) {
      Failure(new CommandException(s"File '${file.getAbsolutePath}' is not readable."))
    } else {
      Try {
        val graph = Graph(name, file)
        state.dataset.map(dataset => state.withDataset(dataset + graph)).getOrElse(state.withDataset(Dataset(graph)))
      }
    }
  }

  class ApplyPrefixes(file: File)(implicit msgPrinter: Printer[String]) extends ComplexStateCommand {
    def execute(state: ComplexState): Try[ComplexState] = if (!file.isFile || !file.canRead) {
      Failure(new CommandException(s"File '${file.getAbsolutePath}' is not readable."))
    } else if (state.dataset.isEmpty) {
      Failure(new CommandException("Any graph is not loaded."))
    } else {
      Try {
        val prefixes = Prefix(file)
        msgPrinter.println("Loaded prefixes: " + prefixes.size)
        state.withDataset(state.dataset.get.withPrefixes(prefixes))
      }
    }
  }

  class Filter(patterns: List[(Option[String], Option[String], Option[String])], inverse: Boolean) extends ComplexStateCommand {

    def isMatched(triple: Triple): Boolean = {
      val matched = patterns.exists { case (s, p, o) =>
        s.forall(compare(triple.subject, _)) && p.forall(compare(triple.predicate, _)) && o.forall(compare(triple.`object`, _))
      }
      if (inverse) !matched else matched
    }

    def execute(state: ComplexState): Try[ComplexState] = if (state.dataset.isEmpty) {
      Failure(new CommandException("Any graph is not loaded."))
    } else if (patterns.isEmpty) {
      Failure(new CommandException("Filter patterns are not specified."))
    } else {
      Try {
        state.withDataset(state.dataset.get.withFilter(isMatched))
      }
    }
  }

  class Replace(search: String, replacement: String, inSubjects: Boolean, inPredicates: Boolean, inObjects: Boolean, patterns: List[(Option[String], Option[String], Option[String])], inverse: Boolean) extends ComplexStateCommand {

    def replaceItem[T <: TripleItem](tripleItem: T): T = tripleItem match {
      case TripleItem.LongUri(uri) => TripleItem.LongUri(uri.replaceAll(search, replacement)).asInstanceOf[T]
      case x: TripleItem.PrefixedUri => x.copy(localName = x.localName.replaceAll(search, replacement)).asInstanceOf[T]
      case TripleItem.BlankNode(id) => TripleItem.BlankNode(id.replaceAll(search, replacement)).asInstanceOf[T]
      case TripleItem.Text(value) => TripleItem.Text(value.replaceAll(search, replacement)).asInstanceOf[T]
      case x => x
    }

    def replace(triple: Triple): Triple = {
      val s = if (inSubjects || (!inPredicates && !inObjects)) replaceItem(triple.subject) else triple.subject
      val p = if (inPredicates || (!inSubjects && !inObjects)) replaceItem(triple.predicate) else triple.predicate
      val o = if (inObjects || (!inSubjects && !inPredicates)) replaceItem(triple.`object`) else triple.`object`
      Triple(s, p, o)
    }

    def execute(state: ComplexState): Try[ComplexState] = if (state.dataset.isEmpty) {
      Failure(new CommandException("Any graph is not loaded."))
    } else if (search.isEmpty) {
      Failure(new CommandException("Search string is empty."))
    } else if (replacement.isEmpty) {
      Failure(new CommandException("Replacement string is empty."))
    } else {
      val isMatched: Triple => Boolean = if (patterns.isEmpty) {
        _ => true
      } else {
        new Filter(patterns, inverse).isMatched
      }
      Try {
        state.withDataset(state.dataset.get.withReplace { triple =>
          if (isMatched(triple)) {
            replace(triple)
          } else {
            triple
          }
        })
      }
    }
  }

  class Print(showSubjects: Boolean, showPredicates: Boolean, showObjects: Boolean, offset: Int, limit: Int, patterns: List[(Option[String], Option[String], Option[String])], inverse: Boolean)(implicit msgPrinter: Printer[String]) extends ComplexStateCommand {
    def execute(state: ComplexState): Try[ComplexState] = {
      val isMatched: Triple => Boolean = if (patterns.isEmpty) {
        _ => true
      } else {
        new Filter(patterns, inverse).isMatched
      }
      val triples = state.dataset.map(_.toTriples).getOrElse(Traversable.empty[Triple].view).filter(isMatched)
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

  class Save(name: String, file: File, allGraphs: Boolean) extends ComplexStateCommand {

    def getFile: File = if (file.getName.endsWith(".ttl")) {
      file
    } else {
      new File(file.getAbsolutePath + ".ttl")
    }

    def execute(state: ComplexState): Try[ComplexState] = if (state.dataset.isEmpty) {
      Failure(new CommandException("Any graph is not loaded."))
    } else if (name.isEmpty) {
      Failure(new CommandException("Graph name must not be empty."))
    } else if (!allGraphs && !state.dataset.get.graphs.exists(_.name == name)) {
      Failure(new CommandException(s"Graph with this name '$name' does not exist."))
    } else {
      val dataset = if (allGraphs) {
        Dataset(Graph(name, state.dataset.get.toTriples))
      } else {
        state.dataset.get
      }
      Try {
        val outputFile = getFile
        RdfSource.Ttl.writeToOutputStream(dataset.graphs.find(_.name == name).get, new FileOutputStream(outputFile))
        state.withDataset(dataset + Graph(name, outputFile))
      }
    }
  }

  class Clear(graph: Option[String]) extends ComplexStateCommand {
    def execute(state: ComplexState): Try[ComplexState] = graph match {
      case Some(graphName) => Success(state.dataset.map(dataset => state.withDataset(dataset - graphName)).getOrElse(state))
      case None => Success(state.clear)
    }
  }

}
