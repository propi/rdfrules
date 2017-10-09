package eu.easyminer.rdf.data

import eu.easyminer.rdf.data.RdfReader.TripleTraversableView

/**
  * Created by Vaclav Zeman on 3. 10. 2017.
  */
class Dataset private(val graphs: IndexedSeq[Graph]) {

  def +(graph: Graph): Dataset = new Dataset(graphs.filter(_.name != graph.name) :+ graph)

  def withPrefixes(prefixes: Seq[Prefix]): Dataset = {
    val map = prefixes.iterator.map(x => x.nameSpace -> x.prefix).toMap

    def uriToPrefixedUri(uri: TripleItem.Uri) = uri match {
      case x: TripleItem.LongUri =>
        val puri = TripleItem.PrefixedUri(x)
        map.get(puri.nameSpace).map(prefix => puri.copy(prefix = prefix)).getOrElse(x)
      case x => x
    }

    def tripleToTripleWithPrefixes(triple: Triple) = triple.copy(
      uriToPrefixedUri(triple.subject),
      uriToPrefixedUri(triple.predicate),
      triple.`object` match {
        case x: TripleItem.Uri => uriToPrefixedUri(x)
        case x => x
      }
    )

    new Dataset(
      graphs.map(graph => graph.copy(triples = graph.triples.map(tripleToTripleWithPrefixes)))
    )
  }

  def withFilter(f: Triple => Boolean): Dataset = new Dataset(graphs.map(graph => graph.copy(triples = graph.triples.filter(f))))

  def toTriples: TripleTraversableView = graphs.iterator.map(_.triples).reduce(_ ++ _)

}

object Dataset {

  def apply(graph: Graph): Dataset = new Dataset(Vector(graph))

}