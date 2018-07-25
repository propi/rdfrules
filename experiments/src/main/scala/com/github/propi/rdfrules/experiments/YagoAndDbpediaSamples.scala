package com.github.propi.rdfrules.experiments

import java.io._

import com.github.propi.rdfrules.algorithm.amie.Amie
import com.github.propi.rdfrules.data._
import com.github.propi.rdfrules.data.formats.JenaLang._
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.java.rule.RuleMeasures
import com.github.propi.rdfrules.rule.AtomPattern.AtomItemPattern.AnyConstant
import com.github.propi.rdfrules.rule._
import com.github.propi.rdfrules.ruleset.ResolvedRule.Atom
import com.github.propi.rdfrules.utils.Debugger
import org.apache.jena.riot.Lang

import scala.io.Source


/**
  * Created by Vaclav Zeman on 24. 4. 2018.
  */
object YagoAndDbpediaSamples {

  def main(args: Array[String]): Unit = {
    val data = Graph("yago", new File("experiments/data/yagoLiteralFacts.tsv")).toDataset +
      Graph("yago", new File("experiments/data/yagoFacts.tsv")) +
      Graph("yago", new File("experiments/data/yagoDBpediaInstances.tsv")) +
      Graph("dbpedia", new File("experiments/data/mappingbased_objects_sample.ttl")) +
      Graph("dbpedia", new File("experiments/data/mappingbased_literals_sample.ttl"))

    Debugger() { implicit debugger =>
      /*data
        .index()
        .cache(new File("index.cache"))*/

      Index.fromCache(new File("index.cache")).useMapper { implicit mapper =>
        _.mine(Amie()
          .addThreshold(Threshold.MinHeadCoverage(0.1))
          .addThreshold(Threshold.TopK(10000))
          .addConstraint(RuleConstraint.WithInstances(true))
          .addPattern(AtomPattern(`object` = AnyConstant) &: AtomPattern(`object` = AnyConstant) =>: AtomPattern(`object` = AnyConstant))
        )
          .countPcaConfidence(0.5)
          .countLift()
          .sortBy(Measure.PcaConfidence, Measure.Lift, Measure.HeadCoverage)
          .take(20)
          .graphBasedRules
          .resolvedRules
          /*.filter(rr => (rr.body :+ rr.head).iterator.collect {
            case x: Atom.GraphBased => x.graphs
          }.flatten.toSet.size > 1)*/
          .foreach(println)
      }

      /*Index.fromCache(new File("index.cache")).mine(Amie())
        .graphBasedRules
        .resolvedRules
        .filter(rr => (rr.body :+ rr.head).iterator.collect {
          case x: Atom.GraphBased => x.graphs
        }.flatten.toSet.size > 1)
        .foreach(println)*/
    }

    //println("******")
    //dbpediaResources.filter(x => x.toString == "<http://dbpedia.org/resource/Belgium>").foreach(println)

    //dbpediaResources.foreach(println)


    //val dbpedia = Graph("dbpedia", new File("temp/dbpedia.ttl"))(JenaLang(Lang.TS))
    //dbpedia.take(10).foreach(println)
    //dbpedia.filter(_.subject.hasSameUriAs("http://cs.dbpedia.org/resource/Biologie")).foreach(println)
    //dbpedia.filter(_.subject.hasSameUriAs("http://cs.dbpedia.org/resource/Biologie")).map(_.copy(subject = "Priroda")).foreach(println)
    //dbpedia.slice(100, 500).export(new FileOutputStream("test.nq"))(RDFFormat.NQUADS_ASCII)
    //dbpedia.take(10).cache.foreach(println)
    //dbpedia.take(10).cache(new FileOutputStream("test.idx")).foreach(println)
    //dbpedia.types().foreach(println)
    //dbpedia.filter(_.predicate.hasSameUriAs("http://cs.dbpedia.org/property/rok")).histogram(`object` = true).foreach(println)
    /*dbpedia.discretize(new EquifrequencyDiscretizationTask {
      def getNumberOfBins: Int = 5

      def getBufferSize: Int = 1000000
    })(_.triple.predicate.hasSameUriAs("http://cs.dbpedia.org/property/rok"))
      .filter(_.predicate.hasSameUriAs("http://cs.dbpedia.org/property/rok"))
      .foreach(println)*/
    //val yago = Graph[RdfSource.Tsv.type]("yago", new File("temp/yago.tsv"))
    //val dataset = Dataset() /*+ dbpedia*/ + yago
    //dataset.export(new FileOutputStream("test.nq"))(RDFFormat.NQUADS_ASCII)
    //dataset.mine(Amie()).resolvedRules.foreach(println)
    /*Debugger() { implicit debugger =>
      val amie = Amie()
        .addThreshold(Threshold.MinHeadCoverage(0.05))
        .addThreshold(Threshold.TopK(100))
        .addConstraint(RuleConstraint.WithoutDuplicitPredicates())
        .addConstraint(RuleConstraint.WithInstances(true))
      val ruleset = dataset.mine(amie)
      ruleset.resolvedRules.foreach(println)
      println(ruleset.size)
    }*/
    /*Debugger() { implicit debugger =>
      val index = dataset.index()
      val amie = index.tripleItemMap { implicit mapper =>
        Amie()
          .addConstraint(RuleConstraint.WithoutDuplicitPredicates())
          .addConstraint(RuleConstraint.WithInstances(true))
          //.addPattern(AtomPattern(predicate = TripleItem.Uri("livesIn")) =>: None)
        //.addPattern(AtomPattern(predicate = TripleItem.Uri("livesIn"), `object` = 'a') =>: None)
        //.addPattern(AtomPattern(predicate = TripleItem.Uri("livesIn"), `object` = AnyConstant) =>: None)
        //.addPattern(AtomPattern(predicate = TripleItem.Uri("livesIn"), `object` = AnyConstant) =>: AtomPattern(predicate = TripleItem.Uri("hasAcademicAdvisor")))
        .addPattern(AtomPattern(predicate = TripleItem.Uri("hasWonPrize")) &: AtomPattern(predicate = TripleItem.Uri("livesIn"), `object` = AnyConstant) =>: AtomPattern(predicate = TripleItem.Uri("hasAcademicAdvisor")))
      }
      val ruleset = index.mine(amie)
      ruleset.resolvedRules.foreach(println)
      println(ruleset.size)
    }*/
    /*Debugger() { implicit debugger =>
      val amie = Amie()
        .addThreshold(Threshold.MinHeadCoverage(0.05))
        .addThreshold(Threshold.TopK(100))
        .addConstraint(RuleConstraint.WithoutDuplicitPredicates())
        .addConstraint(RuleConstraint.WithInstances(true))
      val ruleset = dataset.mine(amie)
      //ruleset.cou
      //ruleset.sortBy(Measure.HeadCoverage).resolvedRules.foreach(println)
      ruleset.
      println(ruleset.size)
    }*/
  }

}