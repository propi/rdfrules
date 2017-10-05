package eu.easyminer.rdf.data

import java.io.OutputStream

import eu.easyminer.rdf.data.RdfReader.TripleTraversableView
import eu.easyminer.rdf.data.RdfSource.RdfSourceToLang
import org.apache.jena.riot.system.StreamRDFWriter

/**
  * Created by Vaclav Zeman on 4. 10. 2017.
  */
trait RdfWriter[T <: RdfSource] {
  def writeToOutputStream(triples: TripleTraversableView, buildOutputStream: => OutputStream): Unit

  def writeToOutputStream(dataset: Dataset, buildOutputStream: => OutputStream): Unit = writeToOutputStream(dataset.toTriples, buildOutputStream)

  def writeToOutputStream(graph: Graph, buildOutputStream: => OutputStream): Unit = writeToOutputStream(Dataset(graph), buildOutputStream)
}

object RdfWriter {

  implicit def anyForLangRdfWriter[T <: RdfSource](implicit lang: RdfSourceToLang[T], flatAscii: (Boolean, Boolean) = (true, true)): RdfWriter[T] = new RdfWriter[T] {
    def writeToOutputStream(triples: TripleTraversableView, buildOutputStream: => OutputStream): Unit = {
      val os = buildOutputStream
      val stream = StreamRDFWriter.getWriterStream(os, lang.format(flatAscii._1, flatAscii._2))
      try {
        stream.start()
        for ((prefix, nameSpace) <- triples.toPrefixes) {
          stream.prefix(prefix, nameSpace)
        }
        triples.foreach(triple => stream.triple(triple))
      } finally {
        stream.finish()
        os.close()
      }
    }
  }

}