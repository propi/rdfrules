import com.github.propi.rdfrules.algorithm.amie.Amie
import com.github.propi.rdfrules.data.{Dataset, Graph, RdfSource, TripleItem}
import com.github.propi.rdfrules.algorithm.amie.RuleCounting._
import com.github.propi.rdfrules.data.formats.JenaLang._
import com.github.propi.rdfrules.data.formats.Tsv._
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.rule._
import com.github.propi.rdfrules.stringifier.CommonStringifiers._
import com.github.propi.rdfrules.stringifier.Stringifier
import com.github.propi.rdfrules.utils.{Debugger, HowLong}
import org.apache.jena.riot.Lang
import org.scalatest.{FlatSpec, Inside, Matchers}

/**
  * Created by Vaclav Zeman on 14. 3. 2018.
  */
class AmieSpec extends FlatSpec with Matchers with Inside {

  private lazy val dataset1 = Dataset[RdfSource.Tsv.type](GraphSpec.dataYago)

  private lazy val dataset2 = Dataset() + Graph[RdfSource.Tsv.type]("yago", GraphSpec.dataYago) + Graph("dbpedia", GraphSpec.dataDbpedia)(RdfSource.JenaLang(Lang.TTL))

  "Amie" should "be created" in {
    val amie = Amie()
    amie.thresholds.get[Threshold.MinHeadSize] shouldBe Some(Threshold.MinHeadSize(100))
    amie.thresholds.apply[Threshold.MinHeadSize].value shouldBe 100
    amie.thresholds.iterator.size shouldBe 3
    amie.addThreshold(Threshold.TopK(10))
    amie.thresholds.iterator.size shouldBe 4
    amie.thresholds.apply[Threshold.TopK].value shouldBe 10
    amie.constraints.iterator.size shouldBe 0
    amie.addConstraint(RuleConstraint.WithInstances(true))
    amie.constraints.iterator.size shouldBe 1
    amie.constraints.apply[RuleConstraint.WithInstances].onlyObjects shouldBe true
    amie.addPattern(RulePattern(AtomPattern(AtomPattern.AtomItemPattern.AnyConstant, AtomPattern.AtomItemPattern.AnyConstant, AtomPattern.AtomItemPattern.AnyConstant)))
    amie.patterns should not be empty
    amie.patterns.head.exact shouldBe false
    amie.patterns.head.consequent should not be empty
    amie.patterns.head.antecedent shouldBe empty
  }

  it should "mine with default params" in {
    val index = Index.fromDataset(dataset1)
    val amie = Amie()
    val rules = index.tripleMap { implicit thi =>
      amie.mine
    }
    rules.size shouldBe 116
  }

  it should "mine without duplicit predicates" in {
    val index = Index.fromDataset(dataset1)
    val amie = Amie().addConstraint(RuleConstraint.WithoutDuplicitPredicates())
    val rules = index.tripleMap { implicit thi =>
      amie.mine.sortBy(_.measures[Measure.HeadCoverage])(Ordering.by[Measure.HeadCoverage, Double](_.value).reverse)
    }
    rules.size shouldBe 67
    rules(1).measures[Measure.HeadCoverage].value shouldBe 0.22784810126582278
    rules(2).measures[Measure.HeadCoverage].value shouldBe 0.16033755274261605
  }

  it should "mine with only specified predicates" in {
    val index = Index.fromDataset(dataset1)
    val onlyPredicates = index.tripleItemMap { implicit tihi =>
      RuleConstraint.OnlyPredicates("imports", "exports", "dealsWith")
    }
    val amie = Amie().addConstraint(RuleConstraint.WithoutDuplicitPredicates()).addConstraint(onlyPredicates)
    val rules = index.tripleMap { implicit thi =>
      amie.mine.sortBy(_.measures[Measure.HeadCoverage])(Ordering.by[Measure.HeadCoverage, Double](_.value).reverse)
    }
    rules.size shouldBe 8
    rules(0).measures[Measure.HeadCoverage].value shouldBe 0.22784810126582278
    rules(1).measures[Measure.HeadCoverage].value shouldBe 0.16033755274261605
    index.tripleItemMap { implicit tihi =>
      rules.iterator.flatMap(x => x.body :+ x.head).map(_.predicate).toSet should contain only(tihi.getIndex(TripleItem.Uri("imports")), tihi.getIndex(TripleItem.Uri("exports")), tihi.getIndex(TripleItem.Uri("dealsWith")))
    }
  }

  it should "mine without specified predicates" in {
    val index = Index.fromDataset(dataset1)
    val onlyPredicates = index.tripleItemMap { implicit tihi =>
      RuleConstraint.WithoutPredicates("imports", "exports", "dealsWith")
    }
    val amie = Amie().addConstraint(RuleConstraint.WithoutDuplicitPredicates()).addConstraint(onlyPredicates)
    val rules = index.tripleMap { implicit thi =>
      amie.mine
    }
    rules.size shouldBe 59
    index.tripleItemMap { implicit tihi =>
      rules.iterator.flatMap(x => x.body :+ x.head).map(_.predicate).toSet should contain noneOf(tihi.getIndex(TripleItem.Uri("imports")), tihi.getIndex(TripleItem.Uri("exports")), tihi.getIndex(TripleItem.Uri("dealsWith")))
    }
  }

  it should "mine with instances" in {
    val index = Index.fromDataset(dataset1)
    val amie = Amie().addConstraint(RuleConstraint.WithoutDuplicitPredicates()).addConstraint(RuleConstraint.WithInstances(false))
    val rules = index.tripleMap { implicit thi =>
      amie.mine
    }
    rules.size shouldBe 20634
  }

  it should "mine with instances and with duplicit predicates" in {
    val index = Index.fromDataset(dataset1)
    val amie = Amie().addConstraint(RuleConstraint.WithInstances(false))
    val rules = index.tripleMap { implicit thi =>
      amie.mine
    }
    rules.size shouldBe 21674
  }

  it should "mine only with object instances" in {
    val index = Index.fromDataset(dataset1)
    val amie = Amie().addConstraint(RuleConstraint.WithoutDuplicitPredicates()).addConstraint(RuleConstraint.WithInstances(true))
    val rules = index.tripleMap { implicit thi =>
      amie.mine.sortBy(_.measures[Measure.HeadCoverage])(Ordering.by[Measure.HeadCoverage, Double](_.value).reverse)
    }
    rules.size shouldBe 9955
    rules(1).measures[Measure.HeadCoverage].value shouldBe 0.22784810126582278
  }

  it should "mine with min length" in {
    val index = Index.fromDataset(dataset1)
    var amie = Amie().addConstraint(RuleConstraint.WithoutDuplicitPredicates()).addThreshold(Threshold.MaxRuleLength(2))
    var rules = index.tripleMap { implicit thi =>
      amie.mine
    }
    rules.size shouldBe 30
    amie = Amie().addConstraint(RuleConstraint.WithoutDuplicitPredicates()).addThreshold(Threshold.MaxRuleLength(4))
    rules = index.tripleMap { implicit thi =>
      amie.mine
    }
    rules.size shouldBe 127
  }

  it should "mine with min head size" in {
    val index = Index.fromDataset(dataset1)
    val amie = Amie().addConstraint(RuleConstraint.WithoutDuplicitPredicates()).addThreshold(Threshold.MinHeadSize(1000))
    val rules = index.tripleMap { implicit thi =>
      amie.mine
    }
    rules.size shouldBe 11
    rules.forall(_.measures[Measure.HeadSize].value >= 1000) shouldBe true
  }

  it should "mine with topK threshold" in {
    val index = Index.fromDataset(dataset1)
    val amie = Amie().addConstraint(RuleConstraint.WithoutDuplicitPredicates()).addConstraint(RuleConstraint.WithInstances(false)).addThreshold(Threshold.TopK(10))
    val rules = index.tripleMap { implicit thi =>
      amie.mine.sortBy(_.measures[Measure.HeadCoverage])(Ordering.by[Measure.HeadCoverage, Double](_.value).reverse)
    }
    rules.size shouldBe 10
    rules(1).measures[Measure.HeadCoverage].value shouldBe 0.22784810126582278
  }

  it should "count confidence" in {
    val index = Index.fromDataset(dataset1)
    val amie = Amie().addConstraint(RuleConstraint.WithoutDuplicitPredicates())
    val rules = index.tripleMap { implicit thi =>
      amie.mine.map(_.withConfidence(0.2)).filter(_.measures.exists[Measure.Confidence])
    }
    rules.size shouldBe 6
    rules.forall(_.measures[Measure.Confidence].value >= 0.2) shouldBe true
  }

  it should "test" ignore {
    Debugger { implicit debugger =>
      val index = Index.fromDataset(dataset1)
      val pattern = index.tripleItemMap { implicit tihi =>
        AtomPattern(
          AtomPattern.AtomItemPattern.Variable('c'),
          AtomPattern.AtomItemPattern.Constant(TripleItem.Uri("exports")),
          AtomPattern.AtomItemPattern.Variable('b')
        ) :: AtomPattern(
          AtomPattern.AtomItemPattern.Variable('a'),
          AtomPattern.AtomItemPattern.Constant(TripleItem.Uri("dealsWith")),
          AtomPattern.AtomItemPattern.Constant(TripleItem.Uri("Germany"))
        ) :: RulePattern(
          AtomPattern(
            AtomPattern.AtomItemPattern.Variable('a'),
            AtomPattern.AtomItemPattern.Constant(TripleItem.Uri("imports")),
            AtomPattern.AtomItemPattern.Variable('b')
          ),
          true
        )
      }
      val constraint = index.tripleItemMap { implicit tihi =>
        RuleConstraint.WithoutPredicates("imports", "exports", "dealsWith")
      }
      val amie = Amie().addConstraint(RuleConstraint.WithoutDuplicitPredicates())
      //.addConstraint(RuleConstraint.WithoutDuplicitPredicates())
      //.addConstraint(RuleConstraint.WithInstances(true))
      //.addThreshold(Threshold.MinHeadCoverage(0.001))
      //.addThreshold(Threshold.TopK(100))
      //.addThreshold(Threshold.MaxRuleLength(5))
      //.addPattern(pattern)
      val rules = index.tripleMap { implicit thi =>
        val rules = amie.mine.sortBy(_.measures[Measure.HeadCoverage])(Ordering.by[Measure.HeadCoverage, Double](_.value).reverse)
        println(rules.size)
        rules.map(_.withConfidence(0.2)).filter(_.measures.exists[Measure.Confidence])
      }
      index.tripleItemMap { implicit tihi =>
        rules.foreach(x => println(Stringifier(x.asInstanceOf[Rule])))
      }
      println(rules.size)

    }
    HowLong.flushAllResults()
  }

}
