package eu.easyminer.rdf.data.formats

import eu.easyminer.rdf.data._
import eu.easyminer.rdf.utils.InputStreamBuilder

import scala.io.Source

/**
  * Created by Vaclav Zeman on 14. 1. 2018.
  */
object Tsv {

  implicit val tsvReader: RdfReader[RdfSource.Tsv.type] = (inputStreamBuilder: InputStreamBuilder) => new Traversable[Quad] {
    def foreach[U](f: (Quad) => U): Unit = {
      val is = inputStreamBuilder.build
      val source = Source.fromInputStream(is)
      try {
        val it = source.getLines().map(_.split("\t")).collect {
          case Array(s, p, o) => Triple(TripleItem.LongUri(s), TripleItem.LongUri(p), TripleItem.Text(o.stripSuffix(".")))
        }
        it.map(Quad(_, TripleItem.LongUri(""))).foreach(f)
      } finally {
        source.close()
        is.close()
      }
    }
  }.view

}
