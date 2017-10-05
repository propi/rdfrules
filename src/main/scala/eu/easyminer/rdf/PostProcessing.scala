package eu.easyminer.rdf

import java.io.{File, FileInputStream}

import eu.easyminer.rdf.clustering._
import eu.easyminer.rdf.data.RdfSource
import eu.easyminer.rdf.rule._
import eu.easyminer.rdf.utils.serialization.{Deserializer, Serializer}

import scala.util.Random

/**
  * Created by Vaclav Zeman on 1. 8. 2017.
  */
object PostProcessing extends App {

  import eu.easyminer.rdf.clustering.SimilarityCounting._

  /*val inputFile = new File(/*StdIn.readLine()*/ "yago.tsv")

  protected val mapper: Map[Int, String] = RdfSource.Tsv.fromFile(inputFile)(it => it.flatMap(triple => Iterator(triple.subject, triple.predicate, triple.`object`.toStringValue)).toSet)
    .iterator
    .zipWithIndex
    .map(_.swap)
    .toMap*/

  /*val rules = List(
    Rule.Simple(Atom(Atom.Variable(0), 1334, Atom.Constant(464)), IndexedSeq(Atom(Atom.Constant(4646), 1334, Atom.Constant(464)), Atom(Atom.Variable(0), 4949, Atom.Variable(464))))(collection.mutable.Map(
      Measure.HeadSize(45), Measure.BodySize(4545), Measure.Support(458), Measure.HeadCoverage(0.78), Measure.Confidence(0.333)
    )),
    Rule.Simple(Atom(Atom.Variable(2), 7984, Atom.Constant(446)), IndexedSeq.empty)(collection.mutable.Map(
      Measure.HeadSize(45), Measure.BodySize(4545), Measure.Support(458), Measure.HeadCoverage(0.78), Measure.Confidence(0.333)
    )),
    Rule.Simple(Atom(Atom.Constant(3), 2161, Atom.Variable(4)), IndexedSeq(Atom(Atom.Constant(4646), 1334, Atom.Constant(464))))(collection.mutable.Map(
      Measure.HeadSize(45), Measure.BodySize(4545), Measure.Support(1000), Measure.HeadCoverage(0.78), Measure.Confidence(0.333)
    ))
  )

  val a = Serializer.serialize(rules)*/

  /*val is = new FileInputStream("rules.bin")
  val b = try {
    val reader = Deserializer.mapInputStream[Rule](is)
    Stream.continually(reader.read()).takeWhile(_.nonEmpty).map(_.get).zipWithIndex.filter(_._2 % 100 == 0).map(_._1).toList
  } finally {
    is.close()
  }

  implicit val rulesSimilarityCounting = ((0.4 * AtomsSimilarityCounting) ~ (0.1 * LengthSimilarityCounting) ~ (0.1 * SupportSimilarityCounting) ~ (0.4 * ConfidenceSimilarityCounting)).apply _
  val clusters = DbScan(6, 0.87, b)

  for ((cluster, index) <- clusters.iterator.zipWithIndex) {
    println("**************************************")
    println("CLUSTER " + index)
    for (rule <- cluster) {
      println(stringifyRule(rule))
    }
  }*/

}
