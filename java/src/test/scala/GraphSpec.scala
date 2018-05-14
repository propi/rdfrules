import java.io.{File, FileInputStream, FileOutputStream}

import GraphSpec._
import com.github.propi.rdfrules.data.{Triple, TripleItem}
import com.github.propi.rdfrules.java.data
import com.github.propi.rdfrules.java.data.{Discretizable, Graph, HistogramKey, Prefix, TripleItemType}
import eu.easyminer.discretization.impl.{Interval, IntervalBound}
import eu.easyminer.discretization.task.{EquidistanceDiscretizationTask, EquifrequencyDiscretizationTask, EquisizeDiscretizationTask}
import eu.easyminer.discretization.{RelativeSupport, Support}
import org.apache.jena.riot.{Lang, RDFFormat}
import org.scalatest.{FlatSpec, Inside, Matchers}

import scala.collection.JavaConverters._
import scala.collection.SeqView
import scala.reflect.ClassTag

/**
  * Created by Vaclav Zeman on 14. 1. 2018.
  */
class GraphSpec extends FlatSpec with Matchers with Inside {

  private lazy val graph = Graph.fromTsv(dataYago)
  private lazy val graphDbpedia = Graph.fromRdfLang(dataDbpedia, Lang.TTL)

  "Graph object" should "be loaded" in {
    graph.getName shouldBe Graph.DEFAULT
    graph.size shouldBe 46654
    var first: Option[data.Triple] = None
    var i = 0
    graph.forEach { triple =>
      i += 1
      if (first.isEmpty) first = Some(triple)
    }
    first.isDefined shouldBe true
    first.get.getSubject.hasSameUriAs(new data.TripleItem.LongUri("Azerbaijan")) shouldBe true
    first.get.getPredicate.hasSameUriAs(new data.TripleItem.LongUri("hasCapital")) shouldBe true
    inside(first.get.getObject) {
      case x: data.TripleItem.LongUri => x.hasSameUriAs(new data.TripleItem.LongUri("Baku")) shouldBe true
    }
    i shouldBe 46654
  }

  it should "be transformable" in {
    graph.filter(x => x.getPredicate.hasSameUriAs(new data.TripleItem.LongUri("dealsWith"))).size shouldBe 520
    graph.filter(x => x.getPredicate.hasSameUriAs(new data.TripleItem.LongUri("dealsWith"))).map(x => new data.Triple(x.getSubject, new data.TripleItem.LongUri("neco"), x.getObject)).forEach(x => x.getPredicate.hasSameUriAs(new data.TripleItem.LongUri("neco")) shouldBe true)
    graph.take(10).size shouldBe 10
    val buffer = collection.mutable.ListBuffer.empty[data.Triple]
    graph.take(2).forEach(x => buffer += x)
    buffer.last.asScala() shouldBe Triple("Azerbaijan", "dealsWith", TripleItem.LongUri("People's_Republic_of_China"))
    buffer.clear()
    graph.drop(1).forEach(x => buffer += x)
    buffer.head.asScala() shouldBe Triple("Azerbaijan", "dealsWith", TripleItem.LongUri("People's_Republic_of_China"))
    buffer.clear()
    graph.slice(1, 2).forEach(x => buffer += x)
    buffer.map(_.asScala()).toList shouldBe List(Triple("Azerbaijan", "dealsWith", TripleItem.LongUri("People's_Republic_of_China")))
  }

  it should "have triples ops" in {
    val types = graph.types()
    types.size shouldBe 33
    types.get(new data.TripleItem.LongUri("hasWonPrize")) should not be null
    types.get(new data.TripleItem.LongUri("hasWonPrize")).get(TripleItemType.RESOURCE) shouldBe 1110
    val dbpediaTypes = graphDbpedia.types()
    dbpediaTypes.size shouldBe 1717
    val rok = dbpediaTypes.get(new data.TripleItem.LongUri("http://cs.dbpedia.org/property/rok"))
    rok should not be null
    rok.get(TripleItemType.TEXT) shouldBe 13
    rok.get(TripleItemType.NUMBER) shouldBe 2340
    val histogram = graph.histogram(false, true, false)
    histogram.size shouldBe 33
    histogram.get(new HistogramKey(null, new data.TripleItem.LongUri("hasGeonamesId"), null)) shouldBe 2103
    val histogram2 = graph.filter(_.getPredicate.hasSameUriAs(new data.TripleItem.LongUri("hasOfficialLanguage"))).histogram(false, true, true)
    histogram2.size shouldBe 147
    histogram2.get(new HistogramKey(null, new data.TripleItem.LongUri("hasOfficialLanguage"), new data.TripleItem.LongUri("Russian_language"))) shouldBe 4
    val histogram3 = graphDbpedia.filter(_.getPredicate.hasSameUriAs(new data.TripleItem.LongUri("http://cs.dbpedia.org/property/rok"))).histogram(false, false, true)
    histogram3.size shouldBe 178
    histogram3.get(new HistogramKey(null, null, new data.TripleItem.Number(new Integer(1981)))) shouldBe 12
  }

  it should "have quads ops" in {
    val prefixes = Prefix.fromInputStream(() => getClass.getResourceAsStream("/prefixes.ttl"))
    val gp = graphDbpedia.addPrefixes(prefixes)
    var i = 0
    gp.prefixes(_ => i = i + 1)
    i shouldBe 2
    gp.take(10).forEach(x => x.getSubject.isInstanceOf[data.TripleItem.PrefixedUri] && x.getPredicate.isInstanceOf[data.TripleItem.PrefixedUri])
  }

  it should "discretize data" in {
    val intervals = graphDbpedia.discretizeAndGetIntervals(new EquidistanceDiscretizationTask {
      def getNumberOfBins: Int = 5

      def getBufferSize: Int = 1000000
    }, Discretizable.Mode.INMEMORY, quad => quad.getTriple.getPredicate.hasSameUriAs(new data.TripleItem.LongUri("http://cs.dbpedia.org/property/rok")))
    intervals.length shouldBe 5
    intervals.last shouldBe Interval(IntervalBound.Inclusive(16009.4), IntervalBound.Inclusive(20010.0))
    val intervals2 = graphDbpedia.discretizeAndGetIntervals(new EquifrequencyDiscretizationTask {
      def getNumberOfBins: Int = 5

      def getBufferSize: Int = 1000000
    }, data.Discretizable.Mode.ONDISC, quad => quad.getTriple.getPredicate.hasSameUriAs(new data.TripleItem.LongUri("http://cs.dbpedia.org/property/rok")))
    intervals2.length shouldBe 5
    intervals2.head shouldBe Interval(IntervalBound.Inclusive(7.0), IntervalBound.Exclusive(1962.5))
    val intervals3 = graphDbpedia.discretizeAndGetIntervals(new EquisizeDiscretizationTask {
      def getMinSupport: Support = new RelativeSupport(0.2)

      def getBufferSize: Int = 1000000
    }, data.Discretizable.Mode.INMEMORY, quad => quad.getTriple.getPredicate.hasSameUriAs(new data.TripleItem.LongUri("http://cs.dbpedia.org/property/rok")))
    intervals3.length shouldBe 4
    intervals3.head shouldBe Interval(IntervalBound.Inclusive(7.0), IntervalBound.Exclusive(1975.5))
    val dg = graphDbpedia.discretize(new EquifrequencyDiscretizationTask {
      def getNumberOfBins: Int = 5

      def getBufferSize: Int = 1000000
    }, data.Discretizable.Mode.INMEMORY, quad => quad.getTriple.getPredicate.hasSameUriAs(new data.TripleItem.LongUri("http://cs.dbpedia.org/property/rok")))
    dg.size shouldBe 50000
    dg.types().get(new data.TripleItem.LongUri("http://cs.dbpedia.org/property/rok")).get(TripleItemType.INTERVAL) shouldBe 2340
    val histogram = dg.filter(_.getPredicate.hasSameUriAs(new data.TripleItem.LongUri("http://cs.dbpedia.org/property/rok"))).histogram(false, false, true)
    histogram.keySet().asScala.filter(_.asScala().o.exists(_.isInstanceOf[TripleItem.Interval])).foreach(x => histogram.get(x).intValue() shouldBe 450 +- 60)
    histogram.keySet().asScala.toList.filter(_.asScala().o.exists(_.isInstanceOf[TripleItem.Interval])).map(x => histogram.get(x).intValue()).sum shouldBe 2340
  }

  it should "be cacheable" in {
    val `SeqView[Triple, _]` = implicitly[ClassTag[SeqView[Triple, _]]]
    graphDbpedia.filter(_.getPredicate.hasSameUriAs(new data.TripleItem.LongUri("http://cs.dbpedia.org/property/rok"))).cache.asScala().triples should matchPattern {
      case `SeqView[Triple, _]`(_) =>
    }
    val cached = graphDbpedia.cache(() => new FileOutputStream("test.cache"), () => new FileInputStream("test.cache"))
    cached.size shouldBe 50000
    val g2 = Graph.fromCache(() => new FileInputStream("test.cache"))
    g2.size shouldBe 50000
    g2.getName shouldBe Graph.DEFAULT
    new File("test.cache").delete() shouldBe true
    graph.cache(() => new FileOutputStream("test.cache"), () => new FileInputStream("test.cache")).size shouldBe 46654
    new File("test.cache").delete() shouldBe true
  }

  it should "export data" in {
    graphDbpedia.export(() => new FileOutputStream("test.data"), RDFFormat.NTRIPLES_ASCII)
    Graph.fromRdfLang(new File("test.data"), Lang.NTRIPLES).size shouldBe 50000
    new File("test.data").delete() shouldBe true
  }

}

object GraphSpec {

  val dataYago = new File(getClass.getResource("/yago.tsv").toURI)
  val dataDbpedia = new File(getClass.getResource("/dbpedia.ttl").toURI)

}