import java.io.{File, FileInputStream, FileOutputStream}

import com.github.propi.rdfrules.algorithm.amie.Amie
import com.github.propi.rdfrules.algorithm.dbscan.DbScan
import com.github.propi.rdfrules.data._
import com.github.propi.rdfrules.rule._
import com.github.propi.rdfrules.index._
import com.github.propi.rdfrules.ruleset._
import org.scalatest.enablers.Sortable
import org.scalatest.{FlatSpec, Inside, Matchers}

import scala.io.Source

/**
  * Created by Vaclav Zeman on 18. 4. 2018.
  */
class RulesetSpec extends FlatSpec with Matchers with Inside {

  private lazy val dataset1 = Dataset(Graph("yago", GraphSpec.dataYago))

  private lazy val ruleset = {
    dataset1.mine(Amie().addConstraint(RuleConstraint.WithoutDuplicitPredicates()).addConstraint(RuleConstraint.WithInstances(true)))
  }

  "Index" should "mine directly from index" in {
    Index(dataset1).mine(Amie()).size shouldBe 123
  }

  "Dataset" should "mine directly from dataset" in {
    dataset1.mine(Amie()).size shouldBe 123
  }

  "Ruleset" should "count confidence" in {
    val rules = ruleset.computeConfidence(0.9).rules
    all(rules.map(_.measures[Measure.Confidence].value)) should be >= 0.9
    rules.size shouldBe 166
    val rules2 = ruleset.computeConfidence(0).rules
    all(rules2.map(_.measures[Measure.Confidence].value)) should be >= 0.01
    rules2.size shouldBe 9900
  }

  it should "count pca confidence" in {
    val rules = ruleset.computePcaConfidence(0.9).rules
    all(rules.map(_.measures[Measure.PcaConfidence].value)) should be >= 0.9
    rules.size shouldBe 1067
  }

  it should "count lift" in {
    val rules = ruleset.computeLift().rules
    all(rules.map(_.measures[Measure.Confidence].value)) should be >= 0.5
    for (rule <- rules) {
      rule.measures.exists[Measure.Lift] shouldBe true
      rule.measures.exists[Measure.HeadConfidence] shouldBe true
    }
    rules.size shouldBe 1333
  }

  it should "sort by interest measures" in {
    ruleset.sorted.rules.toSeq.map(_.measures[Measure.HeadCoverage].value).reverse shouldBe sorted
    ruleset.sortBy(_.measures[Measure.HeadSize].value).rules.toSeq.map(_.measures[Measure.HeadSize].value) shouldBe sorted
    ruleset.sortByResolved(_.measures[Measure.HeadSize].value).rules.toSeq.map(_.measures[Measure.HeadSize].value) shouldBe sorted
    ruleset.sortBy(Measure.HeadSize, Measure.Support).rules
      .toSeq
      .map(x => x.measures[Measure.HeadSize].value -> x.measures[Measure.Support].value)
      .reverse shouldBe sorted
    ruleset.sortByRuleLength(Measure.Support).rules
      .toSeq
      .map(x => x.ruleLength -> x.measures[Measure.Support].value)
      .shouldBe(sorted)(Sortable.sortableNatureOfSeq(Ordering.Tuple2(Ordering[Int], Ordering[Int].reverse)))
  }

  it should "make clusters" in {
    val rules = ruleset.sorted.take(500).makeClusters(DbScan()).cache
    for (rule <- rules) {
      rule.measures.exists[Measure.Cluster] shouldBe true
    }
    rules.rules.map(_.measures[Measure.Cluster].number).toSet.size should be > 30
    rules.size shouldBe 500
  }

  it should "do transform operations" in {
    ruleset.filter(_.measures[Measure.Support].value > 100).size shouldBe 3
    ruleset.filterResolved(_.measures[Measure.Support].value > 100).size shouldBe 3
    val emptyBodies = ruleset.map(x => x.copy(body = Vector())(x.measures)).rules.map(_.body)
    emptyBodies.size shouldBe ruleset.size
    for (body <- emptyBodies) {
      body shouldBe empty
    }
    ruleset.take(10).size shouldBe 10
    ruleset.drop(500).size shouldBe (ruleset.size - 500)
    ruleset.slice(100, 200).size shouldBe 100
  }

  it should "cache" in {
    ruleset.cache("test.cache")
    val d = Ruleset.fromCache(ruleset.index, "test.cache")
    d.size shouldBe ruleset.size
    new File("test.cache").delete() shouldBe true
    ruleset.cache(new FileOutputStream("test.cache"), new FileInputStream("test.cache")).size shouldBe ruleset.size
    new File("test.cache").delete() shouldBe true
    ruleset.cache.size shouldBe ruleset.size
  }

  it should "export" in {
    ruleset.export(new FileOutputStream("output.txt"))(RulesetSource.Text)
    var source = Source.fromFile("output.txt", "UTF-8")
    try {
      source.getLines().size shouldBe ruleset.size
    } finally {
      source.close()
    }
    new File("output.txt").delete() shouldBe true
    ruleset.export("output.json")
    source = Source.fromFile("output.json", "UTF-8")
    try {
      source.getLines().size shouldBe 437722
    } finally {
      source.close()
    }
    new File("output.json").delete() shouldBe true
  }

  it should "filter by patterns" in {
    var x = ruleset.filter(AtomPattern(predicate = TripleItem.Uri("livesIn")) =>: None)
    x.resolvedRules.view.map(_.body.last.predicate) should contain only TripleItem.Uri("livesIn")
    x.size shouldBe 371
    x = ruleset.filter(
      AtomPattern(predicate = TripleItem.Uri("livesIn")) =>: AtomPattern(predicate = TripleItem.Uri("hasCapital")),
      AtomPattern(predicate = TripleItem.Uri("isMarriedTo"))
    )
    x.resolvedRules.view.map(_.head.predicate) should contain allOf(TripleItem.Uri("hasCapital"), TripleItem.Uri("isMarriedTo"))
    x.size shouldBe 3
  }

  it should "work with graph-based rules" in {
    ruleset.graphBasedRules.resolvedRules.view.flatMap(x => x.body :+ x.head).foreach { x =>
      x should matchPattern { case x: ResolvedRule.Atom.GraphBased if x.graphs("yago") => }
    }
    ruleset.filter(AtomPattern(graph = TripleItem.Uri("yago"))).size shouldBe ruleset.size
  }

  it should "cache and export graph-based rules" in {
    //cache
    ruleset.graphBasedRules.cache("test.cache")
    val d = Ruleset.fromCache(ruleset.index, "test.cache")
    d.size shouldBe ruleset.size
    d.resolvedRules.view.flatMap(x => x.body :+ x.head).foreach { x =>
      x should matchPattern { case x: ResolvedRule.Atom.GraphBased if x.graphs("yago") => }
    }
    //export
    d.export("output.txt")
    var source = Source.fromFile("output.txt", "UTF-8")
    try {
      source.getLines().size shouldBe d.size
    } finally {
      source.close()
    }
    new File("output.txt").delete() shouldBe true
    d.export("output.json")
    source = Source.fromFile("output.json", "UTF-8")
    try {
      source.getLines().size shouldBe 467557
    } finally {
      source.close()
    }
    new File("output.json").delete() shouldBe true
    new File("test.cache").delete() shouldBe true
  }

  it should "find most similar or dissimilar rules" in {
    val rule = ruleset.sorted.headResolved
    ruleset.findSimilar(rule, 10).resolvedRules.view.map(x => x.body :+ x.head).map(_.map(_.predicate)).foreach { simRule =>
      simRule should contain atLeastOneOf(TripleItem.Uri("created"), TripleItem.Uri("directed"))
    }
    ruleset.findDissimilar(rule, 10).resolvedRules.view.map(x => x.body :+ x.head).map(_.map(_.predicate)).foreach { simRule =>
      simRule should contain noneOf(TripleItem.Uri("created"), TripleItem.Uri("directed"))
    }
  }

}