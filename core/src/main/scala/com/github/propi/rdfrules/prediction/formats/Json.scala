package com.github.propi.rdfrules.prediction.formats

import com.github.propi.rdfrules.prediction.{PredictionReader, PredictionSource, PredictionWriter, ResolvedPredictedTriple}
import com.github.propi.rdfrules.utils.{ForEach, InputStreamBuilder, OutputStreamBuilder}
import spray.json.DefaultJsonProtocol._
import spray.json._

import java.io.{BufferedInputStream, OutputStreamWriter, PrintWriter}
import scala.io.Source
import scala.language.{implicitConversions, reflectiveCalls}

/**
  * Created by Vaclav Zeman on 18. 4. 2018.
  */
object Json {

  implicit def jsonPredictionWriter(source: PredictionSource.Json.type): PredictionWriter = (triples: ForEach[ResolvedPredictedTriple], outputStreamBuilder: OutputStreamBuilder) => {
    val writer = new PrintWriter(new OutputStreamWriter(outputStreamBuilder.build, "UTF-8"))
    try {
      writer.println('[')
      triples.map(rule => rule.toJson.prettyPrint).foldLeft("") { (sep, rule) =>
        writer.println(sep + rule)
        ","
      }
      writer.println(']')
    } finally {
      writer.close()
    }
  }

  implicit def jsonPredictionReader(source: PredictionSource.Json.type): PredictionReader = (inputStreamBuilder: InputStreamBuilder) => {
    val is = new BufferedInputStream(inputStreamBuilder.build)
    val source = Source.fromInputStream(is, "UTF-8")
    try {
      ForEach.from(
        source.mkString.parseJson.convertTo[IndexedSeq[JsValue]]
          .flatMap(_.convertTo[IndexedSeq[ResolvedPredictedTriple]](ResolvedPredictedTriple.resolvedPredictedTriplesJsonReader))
      )
    } finally {
      source.close()
      is.close()
    }
  }

}