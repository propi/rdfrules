package eu.easyminer.rdf.data

import java.io.File

import eu.easyminer.rdf.data.RdfReader.TripleTraversableView

/**
  * Created by Vaclav Zeman on 3. 10. 2017.
  */
case class Graph(name: String, triples: TripleTraversableView)

object Graph {

  def apply(name: String, file: File): Graph = Graph(name, RdfReader(file))

}