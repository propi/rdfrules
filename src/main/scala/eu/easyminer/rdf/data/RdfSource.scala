package eu.easyminer.rdf.data

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 27. 6. 2017.
  */
sealed trait RdfSource

object RdfSource {

  object Tsv extends RdfSource

  object Nt extends RdfSource

  implicit def rdfSourceToRdfReader[T <: RdfSource](rdfSource: T)(implicit rdfReader: RdfReader[T]): RdfReader[T] = rdfReader

}
