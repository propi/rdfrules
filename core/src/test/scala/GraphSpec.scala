import java.io.{File, FileInputStream, FileOutputStream}

import GraphSpec._
import eu.easyminer.discretization.{RelativeSupport, Support}
import eu.easyminer.discretization.impl.{Interval, IntervalBound}
import eu.easyminer.discretization.task.{EquidistanceDiscretizationTask, EquifrequencyDiscretizationTask, EquisizeDiscretizationTask}
import eu.easyminer.rdf.data.formats.JenaLang._
import eu.easyminer.rdf.data.formats.Tsv._
import eu.easyminer.rdf.data.ops.Discretizable
import eu.easyminer.rdf.data.{Graph, Histogram, PredicateInfo, Prefix, RdfSource, Triple, TripleItem, TripleItemType}
import org.apache.jena.riot.{Lang, RDFFormat}
import org.scalatest.{FlatSpec, Inside, Matchers}

import scala.collection.SeqView
import scala.reflect.ClassTag

/**
  * Created by Vaclav Zeman on 14. 1. 2018.
  */
class GraphSpec extends FlatSpec with Matchers with Inside {

  private lazy val graph = Graph[RdfSource.Tsv.type](dataYago)
  private lazy val graphDbpedia = Graph(dataDbpedia)(RdfSource.JenaLang(Lang.TTL))

  "Graph object" should "be loaded" in {
    graph.name shouldBe Graph.default
    graph.size shouldBe 46654
    graph.triples.size shouldBe graph.quads.size
    var first: Option[Triple] = None
    var i = 0
    graph.foreach { triple =>
      i += 1
      if (first.isEmpty) first = Some(triple)
    }
    first shouldBe Some(Triple("Azerbaijan", "hasCapital", TripleItem.LongUri("Baku")))
    i shouldBe 46654
  }

  it should "be transformable" in {
    graph.filter(_.predicate.hasSameUriAs("dealsWith")).size shouldBe 520
    graph.filter(_.predicate.hasSameUriAs("dealsWith")).map(_.copy(predicate = "neco")).triples.forall(_.predicate.hasSameUriAs("neco"))
    graph.take(10).size shouldBe 10
    graph.take(2).triples.last shouldBe Triple("Azerbaijan", "dealsWith", TripleItem.LongUri("People's_Republic_of_China"))
    graph.drop(1).triples.head shouldBe Triple("Azerbaijan", "dealsWith", TripleItem.LongUri("People's_Republic_of_China"))
    graph.slice(1, 2).triples.toList shouldBe List(Triple("Azerbaijan", "dealsWith", TripleItem.LongUri("People's_Republic_of_China")))
  }

  it should "have triples ops" in {
    graph.types().size shouldBe 33
    val info = graph.types().head
    info.uri shouldBe TripleItem.LongUri("hasWonPrize")
    info.ranges.get(TripleItemType.Resource) shouldBe Some(1110)
    graphDbpedia.types().size shouldBe 1717
    inside(graphDbpedia.types().find(_.uri.hasSameUriAs("http://cs.dbpedia.org/property/rok"))) {
      case Some(PredicateInfo(uri, ranges)) =>
        uri shouldBe TripleItem.LongUri("http://cs.dbpedia.org/property/rok")
        ranges.get(TripleItemType.Text) shouldBe Some(13)
        ranges.get(TripleItemType.Number) shouldBe Some(2340)
    }
    val histogram = graph.histogram(false, true, false)
    histogram.size shouldBe 33
    histogram.get(Histogram.Key().withPredicate("hasGeonamesId")) shouldBe Some(2103)
    val histogram2 = graph.filter(_.predicate.hasSameUriAs("hasOfficialLanguage")).histogram(false, true, true)
    histogram2.size shouldBe 147
    histogram2.get(Histogram.Key().withPredicate("hasOfficialLanguage").withObject(TripleItem.LongUri("Russian_language"))) shouldBe Some(4)
    val histogram3 = graphDbpedia.filter(_.predicate.hasSameUriAs("http://cs.dbpedia.org/property/rok")).histogram(false, false, true)
    histogram3.size shouldBe 178
    histogram3.get(Histogram.Key().withObject(TripleItem.Number(1981))) shouldBe Some(12)
  }

  it should "have quads ops" in {
    val prefixes = Prefix(getClass.getResourceAsStream("/prefixes.ttl"))
    val gp = graphDbpedia.addPrefixes(prefixes)
    gp.prefixes.size shouldBe 2
    gp.take(10).triples.forall(x => x.subject.isInstanceOf[TripleItem.PrefixedUri] && x.predicate.isInstanceOf[TripleItem.PrefixedUri])
  }

  it should "discretize data" in {
    val intervals = graphDbpedia.discretizeAndGetIntervals(new EquidistanceDiscretizationTask {
      def getNumberOfBins: Int = 5

      def getBufferSize: Int = 1000000
    })(quad => quad.triple.predicate.hasSameUriAs("http://cs.dbpedia.org/property/rok"))
    intervals.length shouldBe 5
    intervals.last shouldBe Interval(IntervalBound.Inclusive(16009.4), IntervalBound.Inclusive(20010.0))
    val intervals2 = graphDbpedia.discretizeAndGetIntervals(new EquifrequencyDiscretizationTask {
      def getNumberOfBins: Int = 5

      def getBufferSize: Int = 1000000
    }, Discretizable.DiscretizationMode.OnDisc)(quad => quad.triple.predicate.hasSameUriAs("http://cs.dbpedia.org/property/rok"))
    intervals2.length shouldBe 5
    intervals2.head shouldBe Interval(IntervalBound.Inclusive(7.0), IntervalBound.Exclusive(1962.5))
    val intervals3 = graphDbpedia.discretizeAndGetIntervals(new EquisizeDiscretizationTask {
      def getMinSupport: Support = new RelativeSupport(0.2)

      def getBufferSize: Int = 1000000
    })(quad => quad.triple.predicate.hasSameUriAs("http://cs.dbpedia.org/property/rok"))
    intervals3.length shouldBe 4
    intervals3.head shouldBe Interval(IntervalBound.Inclusive(7.0), IntervalBound.Exclusive(1975.5))
    val dg = graphDbpedia.discretize(new EquifrequencyDiscretizationTask {
      def getNumberOfBins: Int = 5

      def getBufferSize: Int = 1000000
    })(quad => quad.triple.predicate.hasSameUriAs("http://cs.dbpedia.org/property/rok"))
    dg.size shouldBe 50000
    dg.types().find(_.uri.hasSameUriAs("http://cs.dbpedia.org/property/rok")).get.ranges.get(TripleItemType.Interval) shouldBe Some(2340)
    val histogram = dg.filter(_.predicate.hasSameUriAs("http://cs.dbpedia.org/property/rok")).histogram(false, false, true)
    histogram.filter(_._1.o.exists(_.isInstanceOf[TripleItem.Interval])).foreach(x => x._2 shouldBe 450 +- 60)
    histogram.filter(_._1.o.exists(_.isInstanceOf[TripleItem.Interval])).map(_._2).sum shouldBe 2340
  }

  it should "be cacheable" in {
    val `SeqView[Triple, _]` = implicitly[ClassTag[SeqView[Triple, _]]]
    graphDbpedia.filter(_.predicate.hasSameUriAs("http://cs.dbpedia.org/property/rok")).cache.triples should matchPattern {
      case `SeqView[Triple, _]`(_) =>
    }
    val cached = graphDbpedia.cache(new FileOutputStream("test.cache"), new FileInputStream("test.cache"))
    cached.size shouldBe 50000
    val g2 = Graph.fromCache(new FileInputStream("test.cache"))
    g2.size shouldBe 50000
    g2.name shouldBe Graph.default
    new File("test.cache").delete() shouldBe true
    graph.cache(new FileOutputStream("test.cache"), new FileInputStream("test.cache")).size shouldBe 46654
    new File("test.cache").delete() shouldBe true
  }

  it should "export data" in {
    graphDbpedia.export(new FileOutputStream("test.data"))(RDFFormat.NTRIPLES_ASCII)
    Graph(new File("test.data"))(RdfSource.JenaLang(Lang.NTRIPLES)).size shouldBe 50000
    new File("test.data").delete() shouldBe true
  }

}

object GraphSpec {

  val dataYago = new File(getClass.getResource("/yago.tsv").toURI)
  val dataDbpedia = new File(getClass.getResource("/dbpedia.ttl").toURI)

}