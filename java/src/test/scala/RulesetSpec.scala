import java.io.{File, FileInputStream, FileOutputStream}

import com.github.propi.rdfrules.java.algorithm.RulesMining
import com.github.propi.rdfrules.java.data.{Dataset, TripleItem}
import com.github.propi.rdfrules.java.rule.RulePattern.AtomPattern
import com.github.propi.rdfrules.java.rule.{Measure, RulePattern}
import com.github.propi.rdfrules.java.ruleset.{Ruleset, RulesetWriter}
import org.scalatest.{FlatSpec, Inside, Matchers}

import scala.collection.JavaConverters._
import scala.io.Source

/**
  * Created by Vaclav Zeman on 18. 4. 2018.
  */
class RulesetSpec extends FlatSpec with Matchers with Inside {

  private lazy val dataset1 = Dataset.fromTsv(GraphSpec.dataYago)

  private lazy val ruleset = dataset1.mine(RulesMining.amie.withoutDuplicitPredicates().withInstances(true))

  "Ruleset" should "count confidence" in {
    val rules = ruleset.computeConfidence(0.9)
    rules.forEach(rule => rule.getConfidence.doubleValue() should be >= 0.9)
    rules.size shouldBe 166
  }

  it should "count pca confidence" in {
    val rules = ruleset.computePcaConfidence(0.9)
    rules.forEach(rule => rule.getPcaConfidence.doubleValue() should be >= 0.9)
    rules.size shouldBe 1067
  }

  it should "count lift" in {
    val rules = ruleset.computeLift()
    rules.forEach(rule => rule.getConfidence.doubleValue() should be >= 0.5)
    rules.forEach(rule => rule.getLift.doubleValue() should be >= 0.0)
    rules.size shouldBe 1333
  }

  it should "sort by interest measures" in {
    ruleset.sorted.getResolvedRules.asScala.toSeq.map(_.getHeadCoverage).reverse shouldBe sorted
    ruleset.sortBy(Measure.Key.HEADSIZE).getResolvedRules.asScala.toSeq.map(_.getHeadSize).reverse shouldBe sorted
  }

  it should "count clusters" in {
    val rules = ruleset.sorted.take(500).makeClusters().cache
    rules.forEach(rule => rule.getCluster.intValue() should be >= 0)
    rules.getResolvedRules.asScala.map(_.getCluster.intValue()).toSet.size should be > 30
    rules.size shouldBe 500
  }

  it should "do transform operations" in {
    ruleset.filterResolver(rule => rule.getSupport > 100).size shouldBe 3
    ruleset.take(10).size shouldBe 10
    ruleset.drop(500).size shouldBe (ruleset.size - 500)
    ruleset.slice(100, 200).size shouldBe 100
  }

  it should "cache" in {
    ruleset.cache("test.cache")
    val d = Ruleset.fromCache("test.cache", ruleset.getIndex)
    d.size shouldBe ruleset.size
    new File("test.cache").delete() shouldBe true
    ruleset.cache(() => new FileOutputStream("test.cache"), () => new FileInputStream("test.cache")).size shouldBe ruleset.size
    new File("test.cache").delete() shouldBe true
    ruleset.cache.size shouldBe ruleset.size
  }

  it should "export" in {
    ruleset.export(() => new FileOutputStream("output.txt"), new RulesetWriter.Text)
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
    var x = ruleset
      .filter(RulePattern.create().prependBodyAtom(new AtomPattern().withPredicate(new TripleItem.LongUri("livesIn"))))
      .cache()
    x.getResolvedRules.asScala.map(_.getBody.asScala.last.getPredicate) should contain only new TripleItem.LongUri("livesIn")
    x.size shouldBe 371
    x = ruleset.filter(
      RulePattern.create(new AtomPattern().withPredicate(new TripleItem.LongUri("hasCapital"))).prependBodyAtom(new AtomPattern().withPredicate(new RulePattern.Constant(new TripleItem.LongUri("livesIn")))),
      RulePattern.create(new AtomPattern().withPredicate(new TripleItem.LongUri("isMarriedTo")))
    ).cache()
    x.getResolvedRules.asScala.map(_.getHead.getPredicate) should contain allOf(new TripleItem.LongUri("hasCapital"), new TripleItem.LongUri("isMarriedTo"))
    x.size shouldBe 3
  }

  it should "find most similar or dissimilar rules" in {
    val rule = ruleset.sorted.headResolved
    ruleset.findSimilar(rule, 10).getResolvedRules.asScala.map(x => x.getHead :: x.getBody.asScala.toList).map(_.map(_.getPredicate)).foreach { simRule =>
      simRule should contain atLeastOneOf(new TripleItem.LongUri("created"), new TripleItem.LongUri("directed"))
    }
    ruleset.findDissimilar(rule, 10).getResolvedRules.asScala.map(x => x.getHead :: x.getBody.asScala.toList).map(_.map(_.getPredicate)).foreach { simRule =>
      simRule should contain noneOf(new TripleItem.LongUri("created"), new TripleItem.LongUri("directed"))
    }
  }

}