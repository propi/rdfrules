import java.io._

import GraphSpec.dataDbpedia
import com.github.propi.rdfrules.data.Quad.PimpedQuad
import com.github.propi.rdfrules.data._
import com.github.propi.rdfrules.index._
import com.github.propi.rdfrules.rule._
import objectexplorer.MemoryMeasurer
import org.apache.jena.riot.Lang
import org.scalatest.{FlatSpec, Inside, Matchers}

/**
  * Created by Vaclav Zeman on 13. 3. 2018.
  */
class IndexSpec extends FlatSpec with Matchers with Inside {

  private lazy val dataset1 = Dataset(GraphSpec.dataYago)

  private lazy val dataset2 = Dataset() + Graph("yago", GraphSpec.dataYago) + Graph("dbpedia", dataDbpedia)(Lang.TTL)

  "Index" should "create from dataset and load items" in {
    val index = Index.apply(dataset1, true)
    MemoryMeasurer.measureBytes(index) should be(800L +- 100)
    index.tripleItemMap { tihi =>
      val items = dataset1.take(5).quads.flatMap(x => List(x.triple.subject, x.triple.predicate, x.triple.`object`)).toList
      for (item <- items) {
        val code = item.hashCode()
        tihi.getTripleItem(code) shouldBe item
        tihi.getIndex(item) shouldBe code
      }
    }
    val mem = MemoryMeasurer.measureBytes(index)
    mem should be(4600000L +- 100000)
    index.tripleItemMap { tihi =>
      tihi.iterator.size shouldBe 42980
      tihi.iterator.size shouldBe dataset1.quads.flatMap(x => List(x.graph, x.triple.subject, x.triple.predicate, x.triple.`object`)).toSet.size
    }
    MemoryMeasurer.measureBytes(index) should be(mem +- 100)
  }

  it should "create from dataset and load index" in {
    val index = Index.apply(dataset1, false)
    MemoryMeasurer.measureBytes(index) should be(800L +- 100)
    index.tripleMap { thi =>
      thi.size shouldBe dataset1.size
    }
    val mem = MemoryMeasurer.measureBytes(index)
    mem should be(41135848L +- 1000000)
    val cq = index.tripleItemMap { implicit tim =>
      dataset1.quads.head.toCompressedQuad
    }
    index.tripleMap { thi =>
      thi.predicates(cq.p).subjects(cq.s).contains(cq.o) shouldBe true
      thi.getGraphs(cq.s, cq.p, cq.o).contains(cq.g) shouldBe true
      thi.getGraphs(cq.p, TripleItemPosition.Subject(cq.s)).contains(cq.g) shouldBe true
      thi.getGraphs(cq.p, TripleItemPosition.Object(cq.o)).contains(cq.g) shouldBe true
      thi.getGraphs(cq.p).contains(cq.g) shouldBe true
      thi.predicates(cq.p).objects(cq.o).contains(cq.s) shouldBe true
      thi.subjects(cq.s).predicates.contains(cq.p) shouldBe true
      thi.subjects(cq.s).objects(cq.o).contains(cq.p) shouldBe true
      thi.objects(cq.o).predicates.contains(cq.p) shouldBe true
    }
    MemoryMeasurer.measureBytes(index) should be(40763664L +- 1000000)
  }

  it should "load dataset with more graphs" in {
    val index = Index.apply(dataset2, true)
    MemoryMeasurer.measureBytes(index) should be(3000L +- 500)
    index.tripleItemMap { tihi =>
      tihi.iterator.size shouldBe 72263
      tihi.iterator.size shouldBe dataset2.quads.flatMap(x => List(x.graph, x.triple.subject, x.triple.predicate, x.triple.`object`)).toSet.size
    }
    MemoryMeasurer.measureBytes(index) should be(8000000L +- 1000000)
    index.tripleMap { thi =>
      thi.size shouldBe dataset2.size
    }
    val indexMemory1 = MemoryMeasurer.measureBytes(index)
    indexMemory1 should be(79494960L +- 5000000)
    val index2 = index.withEvaluatedLazyVals
    val indexMemory2 = MemoryMeasurer.measureBytes(index2)
    indexMemory1 should be < indexMemory2
    index2.tripleMap { thi =>
      thi.size shouldBe dataset2.size
    }
    indexMemory2 should be < MemoryMeasurer.measureBytes(index2)
  }

  it should "work with graphs" in {
    val index = Index.apply(dataset1, false)
    val cq = index.tripleItemMap { implicit tim =>
      dataset1.quads.head.toCompressedQuad
    }
    index.tripleMap { thi =>
      thi.getGraphs(cq.p).iterator.toList should contain only cq.g
      thi.getGraphs(0).iterator.toList should contain only cq.g
      thi.getGraphs(cq.s, cq.p, cq.o).iterator.toList should contain only cq.g
      thi.getGraphs(cq.p, TripleItemPosition.Subject(cq.s)).iterator.toList should contain only cq.g
    }
    val index2 = Index.apply(dataset2, false)
    val cq2 = index2.tripleItemMap { implicit tim =>
      dataset2.toGraphs.map(_.quads.head.toCompressedQuad).toList
    }
    cq2.size shouldBe 2
    for (cq <- cq2) {
      index2.tripleMap { thi =>
        thi.getGraphs(cq.p).iterator.toList should contain only cq.g
        an[NoSuchElementException] should be thrownBy thi.getGraphs(0)
        thi.getGraphs(cq.s, cq.p, cq.o).iterator.toList should contain only cq.g
        thi.getGraphs(cq.p, TripleItemPosition.Subject(cq.s)).iterator.toList should contain only cq.g
      }
    }
  }

  it should "resolve sameAs" in {
    val graph = Graph(GraphSpec.dataSameAs)
    graph.size shouldBe 9
    val index = graph.index
    index.tripleMap { ti =>
      ti.subjects.size shouldBe 2
      ti.predicates.size shouldBe 1
      ti.objects.size shouldBe 5
      ti.quads.size shouldBe 6
    }
    index.tripleItemMap { implicit mapper =>
      mapper.iterator.size shouldBe 7
    }
  }

  it should "cache" in {
    val index = Index.apply(dataset2, false)
    index.cache(new FileOutputStream("test.index"))
    val file = new File("test.index")
    file.exists() shouldBe true
    file.length() should be > 5000000L
  }

  it should "be loaded from cache" in {
    val index = Index.fromCache(new BufferedInputStream(new FileInputStream("test.index")), false)
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

}