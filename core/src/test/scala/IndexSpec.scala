import java.io._

import GraphSpec.dataDbpedia
import eu.easyminer.rdf.data.{Dataset, Graph, RdfSource, TripleItem}
import org.apache.jena.riot.Lang
import org.scalatest.{FlatSpec, Inside, Matchers}
import eu.easyminer.rdf.data.formats.Tsv._
import eu.easyminer.rdf.data.formats.JenaLang._
import eu.easyminer.rdf.index.Index
import objectexplorer.MemoryMeasurer

/**
  * Created by Vaclav Zeman on 13. 3. 2018.
  */
class IndexSpec extends FlatSpec with Matchers with Inside {

  private lazy val dataset1 = Dataset[RdfSource.Tsv.type](GraphSpec.dataYago)

  private lazy val dataset2 = Dataset() + Graph[RdfSource.Tsv.type]("yago", GraphSpec.dataYago) + Graph("dbpedia", dataDbpedia)(RdfSource.JenaLang(Lang.TTL))

  "Index" should "create from dataset and load items" in {
    val index = Index.fromDataset(dataset1)
    index.toDataset shouldBe dataset1
    index.newIndex.toDataset shouldBe dataset1
    MemoryMeasurer.measureBytes(index) should be(500L +- 50)
    index.tripleItemMap { tihi =>
      val items = dataset1.take(5).quads.flatMap(x => List(x.triple.subject, x.triple.predicate, x.triple.`object`)).toList
      for (item <- items) {
        val code = item.hashCode()
        tihi.getTripleItem(code) shouldBe item
        tihi.getIndex(item) shouldBe code
      }
    }
    val mem = MemoryMeasurer.measureBytes(index)
    mem should be(6000000L +- 100000)
    index.tripleItemMap { tihi =>
      tihi.iterator.size shouldBe 42980
      tihi.iterator.size shouldBe dataset1.quads.flatMap(x => List(x.graph, x.triple.subject, x.triple.predicate, x.triple.`object`)).toSet.size
    }
    MemoryMeasurer.measureBytes(index) shouldBe mem
  }

  it should "create from dataset and load index" in {
    val index = Index.fromDataset(dataset1)
    MemoryMeasurer.measureBytes(index) should be(500L +- 50)
    index.tripleMap { thi =>
      thi.size shouldBe dataset1.size
    }
    val mem = MemoryMeasurer.measureBytes(index)
    mem should be(121000000L +- 1000000)
    val cq = index.tripleItemMap { implicit tim =>
      dataset1.quads.head.toCompressedQuad
    }
    index.tripleMap { thi =>
      thi.subjects(cq.subject).predicates(cq.predicate)(cq.`object`).head shouldBe cq.graph
    }
    MemoryMeasurer.measureBytes(index) shouldBe mem
  }

  it should "load dataset with more graphs" in {
    val index = Index.fromDataset(dataset2)
    MemoryMeasurer.measureBytes(index) should be(2500L +- 500)
    index.tripleItemMap { tihi =>
      tihi.iterator.size shouldBe 72263
      tihi.iterator.size shouldBe dataset2.quads.flatMap(x => List(x.graph, x.triple.subject, x.triple.predicate, x.triple.`object`)).toSet.size
    }
    MemoryMeasurer.measureBytes(index) should be(10000000L +- 1000000)
    index.tripleMap { thi =>
      thi.size shouldBe dataset2.size
    }
    MemoryMeasurer.measureBytes(index) should be(245000000L +- 5000000)
  }

  it should "use inUserInMemory mode" in {
    val index = Index.fromDataset(dataset1, Index.Mode.InUseInMemory)
    val mem = MemoryMeasurer.measureBytes(index)
    index.tripleItemMap(_.iterator.size)
    MemoryMeasurer.measureBytes(index) shouldBe mem
    index.tripleMap(_.size)
    MemoryMeasurer.measureBytes(index) shouldBe mem
  }

  it should "cache" in {
    val index = Index.fromDataset(dataset2)
    index.cache(new FileOutputStream("test.index"))
    val file = new File("test.index")
    file.exists() shouldBe true
    file.length() should be > 5000000L
  }

  it should "loaded from cache" in {
    val index = Index.fromCache(new BufferedInputStream(new FileInputStream("test.index")))
    index.tripleItemMap { tihi =>
      tihi.iterator.size shouldBe 72263
    }
    index.tripleMap { thi =>
      thi.size shouldBe dataset2.size
    }
    val dataset = index.toDataset
    dataset.size shouldBe dataset2.size
    dataset.toGraphs.map(_.name).toList should contain only(TripleItem.Uri("yago"), TripleItem.Uri("dbpedia"))
    index.cache(new BufferedOutputStream(new FileOutputStream("test2.index")))
    new File("test.index").length() shouldBe new File("test2.index").length()
    new File("test2.index").delete() shouldBe true
  }

  it should "loaded from cache with inUseInMemory mode" in {
    val index = Index.fromCache(new BufferedInputStream(new FileInputStream("test.index")), Index.Mode.InUseInMemory)
    val mem = MemoryMeasurer.measureBytes(index)
    mem should be(55L +- 10)
    val dsize = dataset2.size
    index.toDataset.size shouldBe dsize
    MemoryMeasurer.measureBytes(index) shouldBe mem
    index.toDataset.size shouldBe dsize
    MemoryMeasurer.measureBytes(index) shouldBe mem
    new File("test.index").delete() shouldBe true
  }

}