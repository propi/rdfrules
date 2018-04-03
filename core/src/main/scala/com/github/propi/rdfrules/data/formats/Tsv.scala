package com.github.propi.rdfrules.data.formats

import com.github.propi.rdfrules.data
import com.github.propi.rdfrules.data._
import com.github.propi.rdfrules.utils.InputStreamBuilder

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
          case Array(s, p, o) =>
            val stippedObject = o.trim.replaceFirst("\\s*\\.$", "")
            Triple(
              TripleItem.LongUri(s.trim.stripPrefix("<").stripSuffix(">")),
              TripleItem.LongUri(p.trim.stripPrefix("<").stripSuffix(">")),
              if (stippedObject.headOption.contains('<') && stippedObject.lastOption.contains('>'))
                TripleItem.LongUri(stippedObject.stripPrefix("<").stripSuffix(">"))
              else
                TripleItem.Text(stippedObject)
            )
        }
        it.map(data.Quad(_, Graph.default)).foreach(f)
      } finally {
        source.close()
        is.close()
      }
    }
  }.view

}
