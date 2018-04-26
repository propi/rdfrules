import java.io.{File, FileInputStream, FileOutputStream}

import com.github.propi.rdfrules.algorithm.amie.Amie
import com.github.propi.rdfrules.data.formats.Tsv._
import com.github.propi.rdfrules.data.{Dataset, RdfSource, TripleItem}
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.rule.{Measure, RuleConstraint}
import com.github.propi.rdfrules.ruleset.{Ruleset, RulesetSource}
import com.github.propi.rdfrules.stringifier.Stringifier
import org.scalatest.enablers.Sortable
import org.scalatest.{FlatSpec, Inside, Matchers}
import com.github.propi.rdfrules.ruleset.formats.Text._
import com.github.propi.rdfrules.ruleset.formats.Json._
import com.github.propi.rdfrules.stringifier.CommonStringifiers._

import scala.io.Source

/**
  * Created by Vaclav Zeman on 18. 4. 2018.
  */
class RulesetSpec extends FlatSpec with Matchers with Inside {

  private lazy val dataset1 = Dataset[RdfSource.Tsv.type](GraphSpec.dataYago)

  private lazy val ruleset = {
    dataset1.mine(Amie().addConstraint(RuleConstraint.WithoutDuplicitPredicates()).addConstraint(RuleConstraint.WithInstances(true)))
  }

  "Index" should "mine directly from index" ignore {
    Index(dataset1).mine(Amie()).size shouldBe 116
  }

  "Dataset" should "mine directly from dataset" ignore {
    dataset1.mine(Amie()).size shouldBe 116
  }

  "Ruleset" should "count confidence" ignore {
    val rules = ruleset.countConfidence(0.9).rules
    all(rules.map(_.measures[Measure.Confidence].value)) should be >= 0.9
    rules.size shouldBe 166
  }

  it should "count pca confidence" ignore {
    val rules = ruleset.countPcaConfidence(0.9).rules
    all(rules.map(_.measures[Measure.PcaConfidence].value)) should be >= 0.9
    rules.size shouldBe 1067
  }

  it should "count lift" ignore {
    val rules = ruleset.countLift().rules
    all(rules.map(_.measures[Measure.Confidence].value)) should be >= 0.5
    for (rule <- rules) {
      rule.measures.exists[Measure.Lift] shouldBe true
      rule.measures.exists[Measure.HeadConfidence] shouldBe true
    }
    rules.size shouldBe 1333
  }

  it should "count pca lift" ignore {
    val rules = ruleset.countPcaLift().rules
    all(rules.map(_.measures[Measure.PcaConfidence].value)) should be >= 0.5
    for (rule <- rules) {
      rule.measures.exists[Measure.PcaLift] shouldBe true
      rule.measures.exists[Measure.HeadConfidence] shouldBe true
    }
    rules.size shouldBe 3059
  }

  it should "sort by interest measures" ignore {
    ruleset.sorted.rules.map(_.measures[Measure.HeadCoverage].value).reverse shouldBe sorted
    ruleset.sortBy(_.measures[Measure.HeadSize].value).rules.map(_.measures[Measure.HeadSize].value) shouldBe sorted
    ruleset.sortBy(Measure.HeadSize, Measure.Support).rules
      .map(x => x.measures[Measure.HeadSize].value -> x.measures[Measure.Support].value)
      .reverse shouldBe sorted
    ruleset.sortByRuleLength(Measure.Support).rules
      .map(x => x.ruleLength -> x.measures[Measure.Support].value)
      .shouldBe(sorted)(Sortable.sortableNatureOfSeq(Ordering.Tuple2(Ordering[Int], Ordering[Int].reverse)))
  }

  it should "count clusters" ignore {
    val rules = ruleset.sorted.take(500).countClusters()
    for (rule <- rules) {
      rule.measures.exists[Measure.Cluster] shouldBe true
    }
    rules.rules.map(_.measures[Measure.Cluster].number).toSet.size should be > 30
    rules.size shouldBe 500
  }

  it should "do transform operations" ignore {
    ruleset.filter(_.measures[Measure.Support].value > 100).size shouldBe 3
    val emptyBodies = ruleset.map(x => x.copy(body = Vector())(x.measures)).rules.map(_.body)
    emptyBodies.size shouldBe ruleset.size
    for (body <- emptyBodies) {
      body shouldBe empty
    }
    ruleset.take(10).size shouldBe 10
    ruleset.drop(500).size shouldBe (ruleset.size - 500)
    ruleset.slice(100, 200).size shouldBe 100
  }

  it should "use mapper" ignore {
    ruleset.sorted.useMapper { implicit mapper =>
      _.take(1).foreach(x => mapper.getTripleItem(x.head.predicate) shouldBe TripleItem.Uri("directed"))
    }
    ruleset.sorted.useStringifier { implicit stringifier =>
      _.take(1).foreach(x => Stringifier(x) should (include("created") and include("directed")))
    }
  }

  it should "cache" ignore {
    ruleset.cache(new FileOutputStream("test.cache"))
    val d = Ruleset.fromCache(ruleset.index)(new FileInputStream("test.cache"))
    d.size shouldBe ruleset.size
    new File("test.cache").delete() shouldBe true
    ruleset.cache(new FileOutputStream("test.cache"), new FileInputStream("test.cache")).size shouldBe ruleset.size
    new File("test.cache").delete() shouldBe true
    ruleset.cache.size shouldBe ruleset.size
  }

  it should "export" in {
    ruleset.export[RulesetSource.Text.type](new FileOutputStream("output.txt"))
    var source = Source.fromFile("output.txt", "UTF-8")
    try {
      source.getLines().size shouldBe ruleset.size
    } finally {
      source.close()
    }
    new File("output.txt").delete() shouldBe true
    ruleset.export[RulesetSource.Json.type](new FileOutputStream("output.json"))
    source = Source.fromFile("output.json", "UTF-8")
    try {
      source.getLines().size shouldBe 437722
    } finally {
      source.close()
    }
    new File("output.json").delete() shouldBe true
  }

  //rules.view.map(Stringifier.apply(_)).foreach(println)
  //println(rules.size)

}
