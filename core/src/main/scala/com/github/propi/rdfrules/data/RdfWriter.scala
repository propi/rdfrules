package com.github.propi.rdfrules.data

import com.github.propi.rdfrules.data.Quad.QuadTraversableView
import com.github.propi.rdfrules.utils.OutputStreamBuilder

/**
  * Created by Vaclav Zeman on 4. 10. 2017.
  */
trait RdfWriter[+T <: RdfSource] {
  def writeToOutputStream(quads: QuadTraversableView, outputStreamBuilder: OutputStreamBuilder): Unit

  def writeToOutputStream(dataset: Dataset, outputStreamBuilder: OutputStreamBuilder): Unit = writeToOutputStream(dataset.quads, outputStreamBuilder)

  def writeToOutputStream(graph: Graph, outputStreamBuilder: OutputStreamBuilder): Unit = writeToOutputStream(Dataset(graph), outputStreamBuilder)
}
