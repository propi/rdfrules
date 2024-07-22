import com.github.propi.rdfrules.algorithm.amie.Amie
import com.github.propi.rdfrules.algorithm.clustering.DbScan
import com.github.propi.rdfrules.data._
import com.github.propi.rdfrules.index._
import com.github.propi.rdfrules.rule.Measure.{CwaConfidence, PcaConfidence}
import com.github.propi.rdfrules.rule.RuleConstraint.ConstantsAtPosition.ConstantsPosition
import com.github.propi.rdfrules.rule._
import com.github.propi.rdfrules.ruleset._
import org.scalatest.enablers.Sortable
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{CancelAfterFailure, Inside}

import java.io.{File, FileInputStream, FileOutputStream}
import scala.io.Source

/**
  * Created by Vaclav Zeman on 18. 4. 2018.
  */
class RulesetSpec extends AnyFlatSpec with Matchers with Inside with CancelAfterFailure {

  private lazy val dataset1 = Dataset(Graph("yago", GraphSpec.dataYago))

  private lazy val ruleset = {
    dataset1.mine(Amie()
      .addConstraint(RuleConstraint.WithoutDuplicatePredicates())
      .addConstraint(RuleConstraint.ConstantsAtPosition(ConstantsPosition.LowerCardinalitySide()))
      .addThreshold(Threshold.MinHeadCoverage(0.02))
    )
  }

  "Index" should "mine directly from index" in {
    Index(IndexPart(dataset1, false)).mineRules(Amie()
      .addConstraint(RuleConstraint.ConstantsAtPosition(ConstantsPosition.Nowhere))
      .addThreshold(Threshold.MinHeadCoverage(0.01))
    ).size shouldBe 124
  }

  "Dataset" should "mine directly from dataset" in {
    dataset1.mine(Amie()
      .addConstraint(RuleConstraint.ConstantsAtPosition(ConstantsPosition.Nowhere))
      .addThreshold(Threshold.MinHeadCoverage(0.01))
    ).size shouldBe 124
  }

  "Ruleset" should "count confidence" in {
    val rules = ruleset.computeConfidence[CwaConfidence](0.9)
    all(rules.rules.map(_.measures.apply[Measure.CwaConfidence].value).toSeq) should be >= 0.9
    rules.size shouldBe 12
    val rules2 = ruleset.computeConfidence[CwaConfidence](0).rules
    all(rules2.map(_.measures.apply[Measure.CwaConfidence].value).toSeq) should be >= 0.001
    rules2.size shouldBe 810
  }

  it should "count pca confidence" in {
    val rules = ruleset.computeConfidence[PcaConfidence](0.9).rules
    all(rules.map(_.measures.apply[Measure.PcaConfidence].value).toSeq) should be >= 0.9
    rules.size shouldBe 57
  }

  it should "count lift" in {
    val rules = ruleset.computeLift().rules
    all(rules.map(_.measures.apply[Measure.CwaConfidence].value).toSeq) should be >= 0.5
    for (rule <- rules) {
      rule.measures.exists[Measure.Lift] shouldBe true
//      rule.measures.exists[Measure.] shouldBe true
    }
    rules.size shouldBe 105
  }

  it should "sort by interest measures" in {
    ruleset.sorted.rules.toSeq.map(_.measures.apply[Measure.HeadCoverage].value).reverse shouldBe sorted
    ruleset.sortBy(_.measures.apply[Measure.HeadSize].value).rules.toSeq.map(_.measures.apply[Measure.HeadSize].value) shouldBe sorted
    ruleset.sortByResolved(_.measures[Measure.HeadSize].value).rules.toSeq.map(_.measures.apply[Measure.HeadSize].value) shouldBe sorted
    ruleset.sortBy(Measure.HeadSize, Measure.Support).rules
      .toSeq
      .map(x => x.measures.apply[Measure.HeadSize].value -> x.measures.apply[Measure.Support].value)
      .reverse shouldBe sorted
    ruleset.sortByRuleLength(Measure.Support).rules
      .toSeq
      .map(x => x.ruleLength -> x.measures.apply[Measure.Support].value)
      .shouldBe(sorted)(Sortable.sortableNatureOfSeq(Ordering.Tuple2(Ordering[Int], Ordering[Int].reverse)))
  }

  it should "make clusters" in {
    val rules = ruleset.sorted.take(500).makeClusters(DbScan()).cache
    for (rule <- rules) {
      rule.measures.exists[Measure.Cluster] shouldBe true
    }
    rules.rules.map(_.measures.apply[Measure.Cluster].number).toSet.size should be > 30
    rules.size shouldBe 500
  }

  it should "do transform operations" in {
    ruleset.filter(_.measures.apply[Measure.Support].value > 100).size shouldBe 2
    ruleset.filterResolved(_.measures[Measure.Support].value > 100).size shouldBe 2
    val emptyBodies = ruleset.map(x => Rule(x.head, Vector(), x.measures)).rules.map(_.body)
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
      source.getLines().size shouldBe 35226
    } finally {
      source.close()
    }
    new File("output.json").delete() shouldBe true
  }

  it should "filter by patterns" in {
    var x = ruleset.filter(AtomPattern(predicate = TripleItem.Uri("livesIn")) =>: None)
    for (rule <- x.resolvedRules) {
      rule.body.map(_.predicate) should contain(TripleItem.Uri("livesIn"))
    }
    x.size shouldBe 36
    x = ruleset.filter(
      AtomPattern(predicate = TripleItem.Uri("livesIn")) =>: AtomPattern(predicate = TripleItem.Uri("hasCurrency")),
      AtomPattern(predicate = TripleItem.Uri("isCitizenOf"))
    )
    x.resolvedRules.map(_.head.predicate).toSeq should contain allOf(TripleItem.Uri("hasCurrency"), TripleItem.Uri("isCitizenOf"))
    x.size shouldBe 30
  }

  it should "work with graph-based rules" in {
    ruleset.graphAwareRules.resolvedRules.flatMap(x => x.body :+ x.head).foreach { x =>
      x should matchPattern { case x: ResolvedAtom.GraphAware if x.graphs("yago") => }
    }
    ruleset.filter(AtomPattern(graph = TripleItem.Uri("yago"))).size shouldBe ruleset.size
  }

  it should "cache and export graph-based rules" in {
    //cache
    ruleset.graphAwareRules.cache("test.cache")
    val d = Ruleset.fromCache(ruleset.index, "test.cache")
    d.size shouldBe ruleset.size
    d.resolvedRules.flatMap(x => x.body :+ x.head).foreach { x =>
      x should matchPattern { case x: ResolvedAtom.GraphAware if x.graphs("yago") => }
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
      source.getLines().size shouldBe 37613
    } finally {
      source.close()
    }
    new File("output.json").delete() shouldBe true
    new File("test.cache").delete() shouldBe true
  }

  it should "find most similar or dissimilar rules" in {
    val rule = ruleset.sorted.headResolved
    ruleset.findSimilar(rule, 10).resolvedRules.map(x => x.body :+ x.head).map(_.map(_.predicate)).foreach { simRule =>
      simRule should contain atLeastOneOf(TripleItem.Uri("created"), TripleItem.Uri("directed"))
    }
    ruleset.findDissimilar(rule, 10).resolvedRules.map(x => x.body :+ x.head).map(_.map(_.predicate)).foreach { simRule =>
      simRule should contain noneOf(TripleItem.Uri("created"), TripleItem.Uri("directed"))
    }
  }

}