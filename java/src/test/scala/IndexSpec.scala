import java.io._

import GraphSpec.dataDbpedia
import com.github.propi.rdfrules.java.data.{Dataset, Graph, TripleItem}
import com.github.propi.rdfrules.java.index.Index
import org.apache.jena.riot.Lang
import org.scalatest.{FlatSpec, Inside, Matchers}

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

/**
  * Created by Vaclav Zeman on 13. 3. 2018.
  */
class IndexSpec extends FlatSpec with Matchers with Inside {

  private lazy val dataset1 = Dataset.fromTsv(GraphSpec.dataYago)

  private lazy val dataset2 = Dataset.empty().add(Graph.fromTsv(new TripleItem.LongUri("yago"), GraphSpec.dataYago)).add(Graph.fromRdfLang(new TripleItem.LongUri("dbpedia"), dataDbpedia, Lang.TTL))

  "Index" should "create from dataset and load items" in {
    val index = Index.fromDataset(dataset1)
    index.toDataset.size() shouldBe dataset1.size()
    index.newIndex.toDataset.size() shouldBe dataset1.size()
    val items = ListBuffer.empty[TripleItem]
    dataset1.take(5).forEach { x =>
      items += x.getTriple.getSubject
      items += x.getTriple.getPredicate
      items += x.getTriple.getObject
    }
    index.tripleItemMap { tihi =>
      for (item <- items) {
        val code = item.hashCode()
        tihi.getTripleItem(code) shouldBe item
        tihi.getIndex(item) shouldBe code
      }
    }
    index.useMapper { tihi => index =>
      index.toDataset.size() shouldBe dataset1.size()
      for (item <- items) {
        val code = item.hashCode()
        tihi.getTripleItem(code) shouldBe item
        tihi.getIndex(item) shouldBe code
      }
    }
  }

  it should "create from dataset and load index" in {
    val index = Index.fromDataset(dataset1)
    index.tripleMap { thi =>
      thi.size shouldBe dataset1.size
    }
  }

  it should "load dataset with more graphs" in {
    val index = Index.fromDataset(dataset2)
    index.tripleMap { thi =>
      thi.size shouldBe dataset2.size
    }
  }

  it should "cache" in {
    val index = Index.fromDataset(dataset2)
    index.cache(() => new FileOutputStream("test.index"))
    val file = new File("test.index")
    file.exists() shouldBe true
    file.length() should be > 5000000L
  }

  it should "be loaded from cache" in {
    val index = Index.fromCache(() => new BufferedInputStream(new FileInputStream("test.index")))
    index.tripleMap { thi =>
      thi.size shouldBe dataset2.size
    }
    val dataset = index.toDataset
    dataset.size shouldBe dataset2.size
    dataset.toGraphs.asScala.map(_.getName).toList should contain only(new TripleItem.LongUri("yago"), new TripleItem.LongUri("dbpedia"))
    index.cache(() => new BufferedOutputStream(new FileOutputStream("test2.index")))
    new File("test.index").length() shouldBe new File("test2.index").length()
    new File("test2.index").delete() shouldBe true
  }

}