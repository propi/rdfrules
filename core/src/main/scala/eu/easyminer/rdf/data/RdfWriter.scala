package eu.easyminer.rdf.data

import eu.easyminer.rdf.data.Quad.QuadTraversableView
import eu.easyminer.rdf.data.Triple.TripleTraversableView
import eu.easyminer.rdf.utils.OutputStreamBuilder

/**
  * Created by Vaclav Zeman on 4. 10. 2017.
  */
trait RdfWriter[+T <: RdfSource] {
  def writeToOutputStream(quads: QuadTraversableView, outputStreamBuilder: OutputStreamBuilder): Unit

  def writeToOutputStream(triples: TripleTraversableView, outputStreamBuilder: OutputStreamBuilder): Unit = writeToOutputStream(triples.map(_.toQuad), outputStreamBuilder)

  def writeToOutputStream(dataset: Dataset, outputStreamBuilder: OutputStreamBuilder): Unit = writeToOutputStream(dataset.toQuads, outputStreamBuilder)

  def writeToOutputStream(graph: Graph, outputStreamBuilder: OutputStreamBuilder): Unit = writeToOutputStream(Dataset(graph), outputStreamBuilder)
}