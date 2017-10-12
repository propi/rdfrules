package eu.easyminer.rdf.cli.impl

import java.io.{File, FileInputStream, FileOutputStream}

import eu.easyminer.rdf.cli.Command.CommandException
import eu.easyminer.rdf.data.{Dataset, Graph, Triple, TripleItem}
import eu.easyminer.rdf.index._
import eu.easyminer.rdf.utils.{OptionalLazyVal, Printer}

import scala.language.implicitConversions
import scala.util.{Failure, Try}
import CompressedTripleSerialization._

import scala.collection.SeqView

/**
  * Created by Vaclav Zeman on 11. 10. 2017.
  */
class ComplexIndex private(val directory: File, val forcedTripleHashIndex: Boolean, val forcedTripleItemToInt: Boolean, val forcedTripleIntToItem: Boolean) {

  private val graphNameIndexName = "graphs.idx"

  private def triplesIndexName(num: Int) = s"triples$num.idx"

  private def tripleItemIndexName(num: Int) = s"items$num.idx"

  implicit private def fileNameToFile(name: String): File = new File(directory, name)

  lazy val dataset: Dataset = {
    val graphNames = StringIndex.loadToTraversable(new FileInputStream(graphNameIndexName: File)).toIndexedSeq
    graphNames.indices.map { i =>
      Graph(
        graphNames(i),
        new Traversable[Triple] {
          def foreach[U](f: (Triple) => U): Unit = tripleIntToItem { implicit mapper =>
            for (ct <- CompressedTripleIndex.loadToTraversable(new FileInputStream(triplesIndexName(i + 1): File)))
              f(CompressedTriple.toTriple(ct))
          }
        }.view
      )
    }.foldLeft(Dataset())(_ + _)
  }

  lazy val tripleHashIndex: OptionalLazyVal[TripleHashIndex] = OptionalLazyVal(!forcedTripleHashIndex) {
    TripleHashIndex(dataset.graphs.indices.iterator.flatMap(i => CompressedTripleIndex.loadToTraversable(new FileInputStream(triplesIndexName(i + 1): File))).toTraversable)
  }

  lazy val tripleItemToInt: OptionalLazyVal[collection.Map[TripleItem, Int]] = OptionalLazyVal(!forcedTripleItemToInt) {
    val seq = dataset.graphs.indices.map(i => TripleItemIndex.loadToMap(new FileInputStream(tripleItemIndexName(i + 1): File)))
    val iseq = seq.iterator.scanLeft(0)(_ + _.size).take(seq.size).zip(seq.iterator).map { case (start, map) => map.mapValues(_ + start) }.toList
    new collection.Map[TripleItem, Int] {
      def get(key: TripleItem): Option[Int] = iseq.iterator.map(_.get(key)).collectFirst {
        case Some(x) => x
      }

      def iterator: Iterator[(TripleItem, Int)] = iseq.iterator.flatMap(_.iterator)

      def +[V1 >: Int](kv: (TripleItem, V1)): collection.Map[TripleItem, V1] = this

      def -(key: TripleItem): collection.Map[TripleItem, Int] = this
    }
  }

  lazy val tripleIntToItem: OptionalLazyVal[IndexedSeq[TripleItem]] = OptionalLazyVal(!forcedTripleIntToItem) {
    val seqs = dataset.graphs.indices
      .map(i => TripleItemIndex.loadToIndexedSeq(new FileInputStream(tripleItemIndexName(i + 1): File)).view.asInstanceOf[SeqView[TripleItem, Seq[_]]])
      .reduce(_ ++ _)
    new IndexedSeq[TripleItem] {
      def length: Int = seqs.length

      def apply(idx: Int): TripleItem = seqs(idx)
    }
  }

  private def index(dataset: Dataset)(implicit msgPrinter: Printer[String]): Unit = {
    msgPrinter.println("Triple items indexing...")
    StringIndex.save(dataset.graphs.map(_.name), new FileOutputStream(graphNameIndexName: File))
    val numOfGraphs = dataset.graphs.size
    val bnodes = collection.mutable.Map.empty[(Int, Int, Int), TripleItem.BlankNode]

    def resolveBlankNode[T <: TripleItem](item: T, numberOfGraph: Int, numberOfTriple: Int, numberOfItem: Int): T = item match {
      case x: TripleItem.BlankNode =>
        bnodes.getOrElseUpdate((numberOfGraph, numberOfTriple, numberOfItem), x).asInstanceOf[T]
      case x => x
    }

    def graphWithResolvedBlankNodes(graph: Graph, index: Int): Graph = graph.copy(
      triples = graph.triples.zipWithIndex.map { case (triple, i) =>
        Triple(
          resolveBlankNode(triple.subject, index, i, 1),
          resolveBlankNode(triple.predicate, index, i, 2),
          resolveBlankNode(triple.`object`, index, i, 3)
        )
      }
    )

    for ((graph, index) <- dataset.graphs.zipWithIndex) {
      val tripleItemsFile: File = tripleItemIndexName(index + 1)
      TripleItemIndex.save(
        graphWithResolvedBlankNodes(graph, index),
        new FileOutputStream(tripleItemsFile)
      )
      msgPrinter.println(s"Graph '${graph.name}' items have been indexed (${index + 1} of $numOfGraphs).")
    }

    msgPrinter.println("Triples indexing...")
    tripleItemToInt { implicit map =>
      for ((graph, index) <- dataset.graphs.zipWithIndex) {
        val triplesFile: File = triplesIndexName(index + 1)
        CompressedTripleIndex.save(
          graphWithResolvedBlankNodes(graph, index),
          new FileOutputStream(triplesFile)
        )
        msgPrinter.println(s"Graph '${graph.name}' triples have been indexed (${index + 1} of $numOfGraphs).")
      }
    }
  }

}

object ComplexIndex {

  def apply(directory: File, forcedTripleHashIndex: Boolean, forcedTripleItemToInt: Boolean, forcedTripleIntToItem: Boolean): Option[ComplexIndex] = {
    val ci = new ComplexIndex(directory, forcedTripleHashIndex, forcedTripleItemToInt, forcedTripleIntToItem)
    Try {
      if (directory.isDirectory &&
        ci.fileNameToFile(ci.graphNameIndexName).exists() &&
        ci.dataset.graphs.nonEmpty &&
        ci.dataset.graphs.indices.forall(i => ci.fileNameToFile(ci.triplesIndexName(i + 1)).exists() && ci.fileNameToFile(ci.tripleItemIndexName(i + 1)).exists())) {
        Some(ci)
      } else {
        None
      }
    }.toOption.flatten
  }

  def apply(directory: File, dataset: Dataset)(implicit msgPrinter: Printer[String]): Try[ComplexIndex] = if (dataset.graphs.isEmpty) {
    Failure(new CommandException(s"No graph to be indexed."))
  } else if (directory.exists() && !directory.isDirectory) {
    Failure(new CommandException(s"'${directory.getAbsolutePath}' is not directory."))
  } else if (directory.exists() && directory.list().nonEmpty) {
    Failure(new CommandException(s"'${directory.getAbsolutePath}' is not empty directory."))
  } else if (!directory.exists() && !directory.mkdir()) {
    Failure(new CommandException(s"Directory '${directory.getAbsolutePath}' can not be created."))
  } else {
    val ci = new ComplexIndex(directory, false, false, false)
    Try {
      ci.index(dataset)
      ci
    }
  }

}