package eu.easyminer.rdf.index

import java.io.OutputStream

import eu.easyminer.rdf.data.Dataset


/**
  * Created by Vaclav Zeman on 4. 10. 2017.
  */
object PrefixIndex extends Index[(String, String)] {

  def save(dataset: Dataset, buildOutputStream: => OutputStream): Unit = save(dataset.toTriples.toPrefixes, buildOutputStream)

}