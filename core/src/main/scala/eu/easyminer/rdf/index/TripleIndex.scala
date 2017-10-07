package eu.easyminer.rdf.index

import java.io.OutputStream

import eu.easyminer.rdf.data.{Dataset, Triple}

import eu.easyminer.rdf.data.TripleSerialization._

/**
  * Created by Vaclav Zeman on 5. 10. 2017.
  */
object TripleIndex extends Index[Triple] {

  def save(dataset: Dataset, buildOutputStream: => OutputStream): Unit = save(dataset.toTriples, buildOutputStream)

}
