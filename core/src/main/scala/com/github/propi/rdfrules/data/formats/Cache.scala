package com.github.propi.rdfrules.data.formats

import com.github.propi.rdfrules.data._
import com.github.propi.rdfrules.data.ops.PrefixesOps
import com.github.propi.rdfrules.serialization.QuadSerialization._
import com.github.propi.rdfrules.utils.serialization.{Deserializer, Serializer}
import com.github.propi.rdfrules.utils.{InputStreamBuilder, OutputStreamBuilder}

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 14. 1. 2018.
  */
trait Cache {

  implicit def cacheReader(rdfSource: RdfSource.Cache.type): RdfReader = (inputStreamBuilder: InputStreamBuilder) => (f: Quad => Unit) => {
    Deserializer.deserializeFromInputStream[Quad, Unit](inputStreamBuilder.build) { reader =>
      Iterator.continually(reader.read()).takeWhile(_.isDefined).foreach(x => f(x.get))
    }
  }

  implicit def cacheWriter(rdfSource: RdfSource.Cache.type): RdfWriter = (col: PrefixesOps[_], outputStreamBuilder: OutputStreamBuilder) => {
    Serializer.serializeToOutputStream[Quad](outputStreamBuilder.build) { writer =>
      col.quads.foreach(writer.write)
    }
  }

}