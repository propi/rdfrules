package eu.easyminer.rdf.data

import eu.easyminer.rdf.data.Triple.TripleTraversableView
import eu.easyminer.rdf.utils.OutputStreamBuilder

/**
  * Created by Vaclav Zeman on 4. 10. 2017.
  */
trait RdfWriter[+T <: RdfSource] {
  def writeToOutputStream(triples: TripleTraversableView, outputStreamBuilder: OutputStreamBuilder): Unit

  def writeToOutputStream(dataset: Dataset, outputStreamBuilder: OutputStreamBuilder): Unit = writeToOutputStream(dataset.toTriples, outputStreamBuilder)

  def writeToOutputStream(graph: Graph, outputStreamBuilder: OutputStreamBuilder): Unit = writeToOutputStream(Dataset(graph), outputStreamBuilder)
}