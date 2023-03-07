import java.io._
import GraphSpec.dataDbpedia
import com.github.propi.rdfrules.data.Quad.PimpedQuad
import com.github.propi.rdfrules.data._
import com.github.propi.rdfrules.index._
import com.github.propi.rdfrules.rule._
import objectexplorer.MemoryMeasurer
import org.apache.jena.riot.Lang
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{CancelAfterFailure, Inside}

/**
  * Created by Vaclav Zeman on 13. 3. 2018.
  */
class IndexSpec extends AnyFlatSpec with Matchers with Inside with CancelAfterFailure {

  private lazy val dataset1 = Dataset(GraphSpec.dataYago)

  private lazy val dataset2 = Dataset() + Graph("yago", GraphSpec.dataYago) + Graph("dbpedia", dataDbpedia)(Lang.TTL)

  "Index" should "create from dataset and load items" in {
    val index = IndexPart.apply(dataset1, true)
    MemoryMeasurer.measureBytes(index) should be(600L +- 100)
    val tihi = index.tripleItemMap
    val items = dataset1.take(5).quads.flatMap(x => List(x.triple.subject, x.triple.predicate, x.triple.`object`)).toSeq
    for (item <- items) {
      val code = item.hashCode()
      tihi.getTripleItem(code) shouldBe item
      tihi.getIndex(item) shouldBe code
    }
    val mem = MemoryMeasurer.measureBytes(index)
    mem should be(3880592L +- 100000)
    tihi.iterator.size shouldBe 42980
    tihi.iterator.size shouldBe dataset1.quads.flatMap(x => List(x.graph, x.triple.subject, x.triple.predicate, x.triple.`object`)).toSet.size
    MemoryMeasurer.measureBytes(index) should be(mem +- 100)
  }

  it should "create from dataset and load index" in {
    val index = IndexPart.apply(dataset1, false)
    MemoryMeasurer.measureBytes(index) should be(600L +- 100)
    val thi = index.tripleMap
    thi.size shouldBe dataset1.size
    val mem = MemoryMeasurer.measureBytes(index)
    mem should be(42513144L +- 1000000)
    implicit val tim: TripleItemIndex = index.tripleItemMap
    val cq = dataset1.quads.head.toCompressedQuad
    thi.predicates(cq.p).subjects(cq.s).contains(cq.o) shouldBe true
    thi.getGraphs(cq.s, cq.p, cq.o).contains(cq.g) shouldBe true
    thi.getGraphs(cq.p, TripleItemPosition.Subject(cq.s)).contains(cq.g) shouldBe true
    thi.getGraphs(cq.p, TripleItemPosition.Object(cq.o)).contains(cq.g) shouldBe true
    thi.getGraphs(cq.p).contains(cq.g) shouldBe true
    thi.predicates(cq.p).objects(cq.o).contains(cq.s) shouldBe true
    thi.subjects(cq.s).predicates.contains(cq.p) shouldBe true
    thi.subjects(cq.s).objects(cq.o).contains(cq.p) shouldBe true
    thi.objects(cq.o).predicates.contains(cq.p) shouldBe true
    MemoryMeasurer.measureBytes(index) should be(42513144L +- 1000000)
  }

  it should "load dataset with more graphs" in {
    val index = IndexPart.apply(dataset2, true)
    MemoryMeasurer.measureBytes(index) should be(3000L +- 500)
    val tihi = index.tripleItemMap
    tihi.iterator.size shouldBe 72263
    tihi.iterator.size shouldBe dataset2.quads.flatMap(x => List(x.graph, x.triple.subject, x.triple.predicate, x.triple.`object`)).toSet.size
    MemoryMeasurer.measureBytes(index) should be(8000000L +- 1000000)
    val thi1 = index.tripleMap
    thi1.size shouldBe dataset2.size
    val indexMemory1 = MemoryMeasurer.measureBytes(index)
    indexMemory1 should be(79494960L +- 5000000)
    val index2 = index.withEvaluatedLazyVals
    val indexMemory2 = MemoryMeasurer.measureBytes(index2)
    indexMemory1 should be < indexMemory2
    val thi2 = index2.tripleMap
    thi2.size shouldBe dataset2.size
    indexMemory2 should be < MemoryMeasurer.measureBytes(index2)
  }

  it should "work with graphs" in {
    {
      val index = IndexPart.apply(dataset1, false)
      implicit val tim: TripleItemIndex = index.tripleItemMap
      val cq = dataset1.quads.head.toCompressedQuad
      val thi = index.tripleMap
      thi.getGraphs(cq.p).iterator.toList should contain only cq.g
      thi.getGraphs(0).iterator.toList should contain only cq.g
      thi.getGraphs(cq.s, cq.p, cq.o).iterator.toList should contain only cq.g
      thi.getGraphs(cq.p, TripleItemPosition.Subject(cq.s)).iterator.toList should contain only cq.g
    }
    {
      val index2 = IndexPart.apply(dataset2, false)
      implicit val tim: TripleItemIndex = index2.tripleItemMap
      val cq2 = dataset2.toGraphs.map(_.quads.head.toCompressedQuad).toSeq
      cq2.size shouldBe 2
      for (cq <- cq2) {
        val thi = index2.tripleMap
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
    val ti = index.tripleMap
    ti.subjects.size shouldBe 2
    ti.predicates.size shouldBe 1
    ti.objects.size shouldBe 5
    ti.quads.size shouldBe 6
    implicit val mapper: TripleItemIndex = index.tripleItemMap
    mapper.iterator.size shouldBe 7
  }

  it should "cache" in {
    val index = IndexPart.apply(dataset2, false)
    index.cache(new FileOutputStream("test.index"))
    val file = new File("test.index")
    file.exists() shouldBe true
    file.length() should be > 5000000L
  }

  it should "be loaded from cache" in {
    val index = IndexPart.fromCache(new BufferedInputStream(new FileInputStream("test.index")), false)
    val tihi = index.tripleItemMap
    tihi.iterator.size shouldBe 72263
    val thi = index.tripleMap
    thi.size shouldBe dataset2.size
    val dataset = index.toDataset
    dataset.size shouldBe dataset2.size
    dataset.toGraphs.map(_.name).toSeq should contain only(TripleItem.Uri("yago"), TripleItem.Uri("dbpedia"))
    index.cache(new BufferedOutputStream(new FileOutputStream("test2.index")))
    new File("test.index").length() shouldBe new File("test2.index").length()
    new File("test2.index").delete() shouldBe true
  }

}