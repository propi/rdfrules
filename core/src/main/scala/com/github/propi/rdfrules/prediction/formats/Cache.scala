package com.github.propi.rdfrules.prediction.formats

import com.github.propi.rdfrules.prediction.{PredictionReader, PredictionSource, PredictionWriter, ResolvedPredictedTriple}
import com.github.propi.rdfrules.serialization.TripleSerialization.{resolvedPredictedTripleDeserializer, resolvedPredictedTripleSerializer}
import com.github.propi.rdfrules.utils.serialization.{Deserializer, Serializer}
import com.github.propi.rdfrules.utils.{ForEach, InputStreamBuilder, OutputStreamBuilder}

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 18. 4. 2018.
  */
object Cache {

  implicit def cachePredictionWriter(source: PredictionSource.Cache.type): PredictionWriter = (triples: ForEach[ResolvedPredictedTriple], outputStreamBuilder: OutputStreamBuilder) => {
    Serializer.serializeToOutputStream[ResolvedPredictedTriple](outputStreamBuilder.build) { writer =>
      triples.foreach(writer.write)
    }
  }

  implicit def cachePredictionReader(source: PredictionSource.Cache.type): PredictionReader = (inputStreamBuilder: InputStreamBuilder) => (f: ResolvedPredictedTriple => Unit) => {
    Deserializer.deserializeFromInputStream[ResolvedPredictedTriple, Unit](inputStreamBuilder.build) { reader =>
      Iterator.continually(reader.read()).takeWhile(_.isDefined).map(_.get).foreach(f)
    }
  }

}