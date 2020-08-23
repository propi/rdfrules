import java.io.{File, FileOutputStream}

import GraphSpec.dataDbpedia
import com.github.propi.rdfrules.java.data._
import org.apache.jena.riot.{Lang, RDFFormat}
import org.scalatest.{FlatSpec, Inside, Matchers}

import scala.collection.JavaConverters._

/**
  * Created by Vaclav Zeman on 12. 3. 2018.
  */
class DatasetSpec extends FlatSpec with Matchers with Inside {

  private lazy val dataset = Dataset.empty().add(Graph.fromTsv(new TripleItem.LongUri("yago"), GraphSpec.dataYago)).add(Graph.fromRdfLang(new TripleItem.LongUri("dbpedia"), dataDbpedia, Lang.TTL))

  "Dataset" should "load graphs" in {
    var d = Dataset.fromTsv(GraphSpec.dataYago)
    d.size shouldBe 46654
    d.toGraphs.asScala.size shouldBe 1
    d.toGraphs.asScala.foreach(_.getName shouldBe Graph.DEFAULT)
    val dbpg = Graph.fromRdfLang(new TripleItem.LongUri("graph-uri"), dataDbpedia, Lang.TTL)
    d = d.add(dbpg)
    d.toGraphs.asScala.size shouldBe 2
    d.toGraphs.asScala.map(_.getName) should contain(new TripleItem.LongUri("graph-uri"))
    d.size shouldBe (46654 + dbpg.size)
  }

  it should "do all graph ops" in {
    var i = 0
    dataset.userDefinedPrefixes(_ => i += 1)
    i shouldBe 0
    dataset.addPrefixes(Prefix.fromInputStream(() => getClass.getResourceAsStream("/prefixes.ttl"))).userDefinedPrefixes(_ => i += 1)
    i shouldBe 2
    dataset.histogram(false, true, false).size shouldBe 1750
    val intervals = dataset.discretizeAndGetIntervals(new DiscretizationTask.Equifrequency(5), _.getTriple.getPredicate.hasSameUriAs(new TripleItem.LongUri("http://cs.dbpedia.org/property/rok"))).asScala
    intervals.size shouldBe 5
    intervals.head.getLeftBoundValue shouldBe 7.0
    intervals.last.getRightBoundValue shouldBe 20010.0
    dataset.types().size shouldBe 1750
    dataset.take(10).size shouldBe 10
    dataset.filter(q => q.getGraph.hasSameUriAs(new TripleItem.LongUri("yago"))).size shouldBe 46654
  }

  it should "cache" in {
    dataset.cache("test.cache")
    val d = Dataset.fromCache("test.cache")
    d.size shouldBe 96654
    d.toGraphs.asScala.size shouldBe 2
    d.toGraphs.asScala.toList.map(_.getName) should contain only(new TripleItem.LongUri("yago"), new TripleItem.LongUri("dbpedia"))
    new File("test.cache").delete() shouldBe true
  }

  it should "export" in {
    def repairTripleItem(tripleItem: TripleItem) = tripleItem match {
      case x: TripleItem.PrefixedUri => new TripleItem.PrefixedUri(x.getPrefix, x.getNameSpace, x.getLocalName.replaceAll("\"|\\W", ""))
      case x: TripleItem.LongUri => new TripleItem.LongUri(x.getUri.replaceAll("\"|\\W", ""))
      case x => x
    }

    dataset
      .map(q => new Quad(new Triple(repairTripleItem(q.getTriple.getSubject).asInstanceOf[TripleItem.Uri], repairTripleItem(q.getTriple.getPredicate).asInstanceOf[TripleItem.Uri], repairTripleItem(q.getTriple.getObject)), q.getGraph))
      .export(() => new FileOutputStream("test.data"), RDFFormat.NQUADS_ASCII)
    val d = Dataset.fromRdfLang(new File("test.data"), Lang.NQUADS)
    d.size shouldBe 96654
    d.toGraphs.asScala.size shouldBe 2
    d.toGraphs.asScala.toList.map(_.getName) should contain only(new TripleItem.LongUri("yago"), new TripleItem.LongUri("dbpedia"))
    new File("test.data").delete() shouldBe true
  }

}
