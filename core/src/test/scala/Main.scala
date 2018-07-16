import java.io.{File, FileOutputStream}

import com.github.propi.rdfrules.algorithm.amie.Amie
import com.github.propi.rdfrules.data.{Dataset, Graph, RdfSource, TripleItem}
import com.github.propi.rdfrules.data.RdfSource.JenaLang
import com.github.propi.rdfrules.data.formats.JenaLang._
import com.github.propi.rdfrules.data.formats.Tsv._
import com.github.propi.rdfrules.rule.{AtomPattern, Measure, RuleConstraint, Threshold}
import com.github.propi.rdfrules.utils.Debugger
import eu.easyminer.discretization.DiscretizationTask
import eu.easyminer.discretization.task.EquifrequencyDiscretizationTask
import org.apache.jena.riot.{Lang, RDFFormat}
import com.github.propi.rdfrules.rule.RulePattern._
import com.github.propi.rdfrules.rule.AtomPattern.AtomItemPattern._

/**
  * Created by Vaclav Zeman on 24. 4. 2018.
  */
object Main {

  def main(args: Array[String]): Unit = {
    val dbpedia = Graph("dbpedia", new File("temp/dbpedia.ttl"))(JenaLang(Lang.TTL))
    dbpedia.take(10).foreach(println)
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
    val yago = Graph[RdfSource.Tsv.type]("yago", new File("temp/yago.tsv"))
    val dataset = Dataset() /*+ dbpedia*/ + yago
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