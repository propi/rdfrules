import java.io.{File, FileInputStream, FileOutputStream}
import GraphSpec.dataDbpedia
import com.github.propi.rdfrules.data._
import org.apache.jena.riot.Lang
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{CancelAfterFailure, Inside}

/**
  * Created by Vaclav Zeman on 12. 3. 2018.
  */
class DatasetSpec extends AnyFlatSpec with Matchers with Inside with CancelAfterFailure {

  private lazy val dataset = Dataset() + Graph("yago", GraphSpec.dataYago) + Graph("dbpedia", dataDbpedia)(Lang.TTL)

  "Dataset" should "load graphs" in {
    var d = Dataset(GraphSpec.dataYago)
    d.size shouldBe 46654
    d.toGraphs.size shouldBe 1
    d.toGraphs.foreach(_.name shouldBe Graph.default)
    val dbpg = Graph("graph-uri", dataDbpedia)(Lang.TTL)
    d = d + dbpg
    d.toGraphs.size shouldBe 2
    d.toGraphs.map(_.name).toSeq should contain(TripleItem.Uri("graph-uri"))
    d.size shouldBe (46654 + dbpg.size)
  }

  it should "do all graph ops" in {
    dataset.quads.size shouldBe dataset.triples.size
    dataset.userDefinedPrefixes.size shouldBe 0
    dataset.addPrefixes(getClass.getResourceAsStream("/prefixes.ttl")).userDefinedPrefixes.size shouldBe 2
    dataset.histogram(false, true).iterator.size shouldBe 1750
    val intervals = dataset.discretizeAndGetIntervals(DiscretizationTask.Equifrequency(5))(_.triple.predicate.hasSameUriAs("http://cs.dbpedia.org/property/rok"))
    intervals.length shouldBe 5
    intervals.head.getLeftBoundValue() shouldBe 7.0
    intervals.last.getRightBoundValue() shouldBe 20010.0
    dataset.properties().iterator.size shouldBe 1750
    dataset.take(10).size shouldBe 10
    dataset.filter(q => q.graph == TripleItem.Uri("yago")).size shouldBe 46654
  }

  it should "cache" in {
    dataset.cache(new FileOutputStream("test.cache"))
    val d = Dataset.fromCache(new FileInputStream("test.cache"))
    d.size shouldBe 96654
    d.toGraphs.size shouldBe 2
    d.toGraphs.toSeq.map(_.name) should contain only(TripleItem.Uri("yago"), TripleItem.Uri("dbpedia"))
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
      .export("test.nq")
    val d = Dataset("test.nq")
    d.size shouldBe 96654
    d.toGraphs.size shouldBe 2
    d.toGraphs.toSeq.map(_.name) should contain only(TripleItem.Uri("yago"), TripleItem.Uri("dbpedia"))
    new File("test.nq").delete() shouldBe true
  }

}