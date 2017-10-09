package eu.easyminer.rdf.index

import java.io.OutputStream

import eu.easyminer.rdf.data.{Dataset, Prefix}
import eu.easyminer.rdf.data.PrefixSerialization._

/**
  * Created by Vaclav Zeman on 4. 10. 2017.
  */
object PrefixIndex extends Index[Prefix] {

  def save(dataset: Dataset, buildOutputStream: => OutputStream): Unit = save(dataset.toTriples.toPrefixes, buildOutputStream)

}