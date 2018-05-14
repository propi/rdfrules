import java.io.{File, FileInputStream, FileOutputStream}
import java.util.function.Consumer

import com.github.propi.rdfrules.java.algorithm.RulesMining
import com.github.propi.rdfrules.java.data.{Dataset, TripleItem}
import com.github.propi.rdfrules.java.rule.RulePattern
import com.github.propi.rdfrules.java.rule.RulePattern.AtomPattern
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
    val rules = ruleset.countConfidence(0.9)
    rules.forEach(rule => rule.getConfidence.doubleValue() should be >= 0.9)
    rules.size shouldBe 166
  }

  it should "count pca confidence" in {
    val rules = ruleset.countPcaConfidence(0.9)
    rules.forEach(rule => rule.getPcaConfidence.doubleValue() should be >= 0.9)
    rules.size shouldBe 1067
  }

  it should "count lift" in {
    val rules = ruleset.countLift()
    rules.forEach(rule => rule.getConfidence.doubleValue() should be >= 0.5)
    rules.forEach(rule => rule.getLift.doubleValue() should be >= 0.0)
    rules.size shouldBe 1333
  }

  it should "count pca lift" in {
    val rules = ruleset.countPcaLift()
    rules.forEach(rule => rule.getPcaConfidence.doubleValue() should be >= 0.5)
    rules.forEach(rule => rule.getPcaLift.doubleValue() should be >= 0.0)
    rules.size shouldBe 3059
  }

  it should "sort by interest measures" in {
    ruleset.sorted.getResolvedRules.asScala.toSeq.map(_.getHeadCoverage).reverse shouldBe sorted
    ruleset.sortBy(rule => rule.getHeadSize, (o1: Integer, o2: Integer) => o1.compareTo(o2)).getResolvedRules.asScala.toSeq.map(_.getHeadSize) shouldBe sorted
  }

  it should "count clusters" in {
    val rules = ruleset.sorted.take(500).countClusters().cache
    rules.forEach(rule => rule.getCluster.intValue() should be >= 0)
    rules.getResolvedRules.asScala.map(_.getCluster.intValue()).toSet.size should be > 30
    rules.size shouldBe 500
  }

  it should "do transform operations" in {
    ruleset.filter(rule => rule.getSupport > 100).size shouldBe 3
    ruleset.take(10).size shouldBe 10
    ruleset.drop(500).size shouldBe (ruleset.size - 500)
    ruleset.slice(100, 200).size shouldBe 100
  }

  it should "use mapper" in {
    ruleset.sorted().useMapper { mapper =>
      new Consumer[Ruleset] {
        def accept(t: Ruleset): Unit = t.take(1).forEach(rule => mapper.getTripleItem(rule.getHead.getPredicate) shouldBe new TripleItem.LongUri("directed"))
      }
    }
  }

  it should "cache" in {
    ruleset.cache(() => new FileOutputStream("test.cache"))
    val d = Ruleset.fromCache(() => new FileInputStream("test.cache"), ruleset.getIndex)
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
    ruleset.export(() => new FileOutputStream("output.json"), new RulesetWriter.Json)
    source = Source.fromFile("output.json", "UTF-8")
    try {
      source.getLines().size shouldBe 437722
    } finally {
      source.close()
    }
    new File("output.json").delete() shouldBe true
  }

  it should "filter by patterns" in {
    ruleset.useMapper(mapper => new Consumer[Ruleset] {
      def accept(t: Ruleset): Unit = {
        var x = ruleset
          .filter(RulePattern.create().prependBodyAtom(new AtomPattern().withPredicate(new RulePattern.Constant(new TripleItem.LongUri("livesIn"), mapper))))
          .cache()
        x.getResolvedRules.asScala.map(_.getBody.asScala.last.getPredicate) should contain only new TripleItem.LongUri("livesIn")
        x.size shouldBe 371
        x = ruleset.filter(
          RulePattern.create(new AtomPattern().withPredicate(new RulePattern.Constant(new TripleItem.LongUri("hasCapital"), mapper))).prependBodyAtom(new AtomPattern().withPredicate(new RulePattern.Constant(new TripleItem.LongUri("livesIn"), mapper))),
          RulePattern.create(new AtomPattern().withPredicate(new RulePattern.Constant(new TripleItem.LongUri("isMarriedTo"), mapper)))
        ).cache()
        x.getResolvedRules.asScala.map(_.getHead.getPredicate) should contain allOf(new TripleItem.LongUri("hasCapital"), new TripleItem.LongUri("isMarriedTo"))
        x.size shouldBe 3
      }
    })
  }

}