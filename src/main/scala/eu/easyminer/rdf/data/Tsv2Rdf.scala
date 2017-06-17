package eu.easyminer.rdf.data

import java.io.File

import scala.io.Source

/**
  * Created by propan on 16. 4. 2017.
  */
object Tsv2Rdf {

  def apply[T](file: File)(f: Iterator[Triple] => T): T = {
    val source = Source.fromFile(file)
    try {
      val it = source.getLines().map(_.split("\t")).collect {
        case Array(s, p, o) => Triple(s, p, TripleObject.NominalLiteral(o.stripSuffix(".")))
      }
      f(it)
    } finally {
      source.close()
    }
  }

}
