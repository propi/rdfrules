package eu.easyminer.rdf.data

import scala.language.implicitConversions

/**
  * Created by propan on 16. 4. 2017.
  */
case class Triple(subject: String, predicate: String, `object`: TripleObject)