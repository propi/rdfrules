import java.io.{File, FileInputStream, FileOutputStream}

import GraphSpec.dataDbpedia
import com.github.propi.rdfrules.data._
import eu.easyminer.discretization.task.EquifrequencyDiscretizationTask
import com.github.propi.rdfrules.data.formats.JenaLang._
import com.github.propi.rdfrules.data.formats.Tsv._
import org.apache.jena.riot.{Lang, RDFFormat}
import org.scalatest.{FlatSpec, Inside, Matchers}

/**
  * Created by Vaclav Zeman on 12. 3. 2018.
  */
class DatasetSpec extends FlatSpec with Matchers with Inside {

  private lazy val dataset = Dataset() + Graph[RdfSource.Tsv.type]("yago", GraphSpec.dataYago) + Graph("dbpedia", dataDbpedia)(RdfSource.JenaLang(Lang.TTL))

  "Dataset" should "load graphs" in {
    var d = Dataset[RdfSource.Tsv.type](GraphSpec.dataYago)
    d.size shouldBe 46654
    d.toGraphs.size shouldBe 1
    d.toGraphs.foreach(_.name shouldBe Graph.default)
    val dbpg = Graph("graph-uri", dataDbpedia)(RdfSource.JenaLang(Lang.TTL))
    d = d + dbpg
    d.toGraphs.size shouldBe 2
    d.toGraphs.map(_.name) should contain(TripleItem.Uri("graph-uri"))
    d.size shouldBe (46654 + dbpg.size)
  }

  it should "do all graph ops" in {
    dataset.quads.size shouldBe dataset.triples.size
    dataset.prefixes.size shouldBe 0
    dataset.addPrefixes(Prefix(getClass.getResourceAsStream("/prefixes.ttl"))).prefixes.size shouldBe 2
    dataset.histogram(false, true).size shouldBe 1750
    val intervals = dataset.discretizeAndGetIntervals(new EquifrequencyDiscretizationTask {
      def getNumberOfBins: Int = 5

      def getBufferSize: Int = 1000000
    })(_.triple.predicate.hasSameUriAs("http://cs.dbpedia.org/property/rok"))
    intervals.length shouldBe 5
    intervals.head.getLeftBoundValue() shouldBe 7.0
    intervals.last.getRightBoundValue() shouldBe 20010.0
    dataset.types().size shouldBe 1750
    dataset.take(10).size shouldBe 10
    dataset.filter(q => q.graph == TripleItem.Uri("yago")).size shouldBe 46654
  }

  it should "cache" in {
    dataset.cache(new FileOutputStream("test.cache"))
    val d = Dataset.fromCache(new FileInputStream("test.cache"))
    d.size shouldBe 96654
    d.toGraphs.size shouldBe 2
    d.toGraphs.toList.map(_.name) should contain only(TripleItem.Uri("yago"), TripleItem.Uri("dbpedia"))
    new File("test.cache").delete() shouldBe true
  }

  it should "export" in {
    def repairTripleItem(tripleItem: TripleItem) = tripleItem match {
      case x: TripleItem.PrefixedUri => x.copy(localName = x.localName.replaceAll("\"|\\W", ""))
      case x: TripleItem.LongUri => TripleItem.LongUri(x.uri.replaceAll("\"|\\W", ""))
      case x => x
    }

    dataset
      .map(q => q.copy(triple = q.triple.copy(repairTripleItem(q.triple.subject).asInstanceOf[TripleItem.Uri], repairTripleItem(q.triple.predicate).asInstanceOf[TripleItem.Uri], repairTripleItem(q.triple.`object`))))
      .export(new FileOutputStream("test.data"))(RDFFormat.NQUADS_ASCII)
    val d = Dataset(new File("test.data"))(RdfSource.JenaLang(Lang.NQUADS))
    d.size shouldBe 96654
    d.toGraphs.size shouldBe 2
    d.toGraphs.toList.map(_.name) should contain only(TripleItem.Uri("yago"), TripleItem.Uri("dbpedia"))
    new File("test.data").delete() shouldBe true
  }

}
