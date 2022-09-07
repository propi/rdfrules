package com.github.propi.rdfrules.prediction.formats

import com.github.propi.rdfrules.prediction.{PredictionReader, PredictionSource, PredictionWriter, ResolvedPredictedTriple}
import com.github.propi.rdfrules.utils.{ForEach, InputStreamBuilder, OutputStreamBuilder}
import spray.json._

import java.io._
import scala.io.Source
import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 18. 4. 2018.
  */
object NDJson {

  implicit def ndjsonPredictionWriter(source: PredictionSource.NDJson.type): PredictionWriter = (triples: ForEach[ResolvedPredictedTriple], outputStreamBuilder: OutputStreamBuilder) => {
    val writer = new PrintWriter(new OutputStreamWriter(outputStreamBuilder.build, "UTF-8"))
    try {
      triples.map(triple => triple.toJson.compactPrint).foreach(writer.println)
    } finally {
      writer.close()
    }
  }

  implicit def ndjsonPredictionReader(source: PredictionSource.NDJson.type): PredictionReader = (inputStreamBuilder: InputStreamBuilder) => (f: ResolvedPredictedTriple => Unit) => {
    val is = new BufferedInputStream(inputStreamBuilder.build)
    val source = Source.fromInputStream(is, "UTF-8")
    try {
      source.getLines().map(_.parseJson.convertTo[ResolvedPredictedTriple]).foreach(f)
    } finally {
      source.close()
      is.close()
    }
  }

}