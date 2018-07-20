import java.util.concurrent.atomic.AtomicBoolean

import com.github.propi.rdfrules.algorithm.amie.Amie
import com.github.propi.rdfrules.algorithm.amie.RuleCounting._
import com.github.propi.rdfrules.data.formats.JenaLang._
import com.github.propi.rdfrules.data.formats.Tsv._
import com.github.propi.rdfrules.data.{Dataset, Graph, RdfSource, TripleItem}
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.rule.AtomPattern.AtomItemPattern.{AnyConstant, AnyVariable, NoneOf, OneOf}
import com.github.propi.rdfrules.rule.RuleConstraint.WithInstances
import com.github.propi.rdfrules.rule.RulePattern._
import com.github.propi.rdfrules.rule._
import org.apache.jena.riot.Lang
import org.scalatest.{FlatSpec, Inside, Matchers}

/**
  * Created by Vaclav Zeman on 14. 3. 2018.
  */
class AmieSpec extends FlatSpec with Matchers with Inside {

  private lazy val dataset1 = Dataset[RdfSource.Tsv.type](GraphSpec.dataYago)

  private lazy val dataset2 = Dataset() + Graph[RdfSource.Tsv.type]("yago", GraphSpec.dataYago) + Graph("dbpedia", GraphSpec.dataDbpedia)(RdfSource.JenaLang(Lang.TTL))

  /*"huhu" should "mine with default params" in {
    val index = Index.apply(dataset1)
    /*val amieStart = Amie().addConstraint(WithInstances(false))//.addThreshold(Threshold.MinHeadCoverage(0.2))
    index.mine(amieStart)*/
    val amie = Amie().addConstraint(WithInstances(false)).addThreshold(Threshold.MinHeadCoverage(0.2))
    val rules1 = index.mine(amie)
    println(rules1.size)
    rules1.sorted.resolvedRules.foreach(println)
    //AMIE.main("datasets/yago.tsv -const -minhc 0.01".split(' '))
    //rules2.resolvedRules.foreach(println)
    HowLong.flushAllResults()
  }*/

  "Amie" should "be created" in {
    var amie = Amie()
    amie.thresholds.get[Threshold.MinHeadSize] shouldBe Some(Threshold.MinHeadSize(100))
    amie.thresholds.apply[Threshold.MinHeadSize].value shouldBe 100
    amie.thresholds.iterator.size shouldBe 3
    amie = amie.addThreshold(Threshold.TopK(10))
    amie.thresholds.iterator.size shouldBe 4
    amie.thresholds.apply[Threshold.TopK].value shouldBe 10
    amie.constraints.iterator.size shouldBe 0
    amie = amie.addConstraint(RuleConstraint.WithInstances(true))
    amie.constraints.iterator.size shouldBe 1
    amie.constraints.apply[RuleConstraint.WithInstances].onlyObjects shouldBe true
    amie = amie.addPattern(RulePattern(AtomPattern(AtomPattern.AtomItemPattern.AnyConstant, AtomPattern.AtomItemPattern.AnyConstant, AtomPattern.AtomItemPattern.AnyConstant)))
    amie.patterns should not be empty
    amie.patterns.head.exact shouldBe false
    amie.patterns.head.consequent should not be empty
    amie.patterns.head.antecedent shouldBe empty
    amie = amie.addThreshold(Threshold.MinHeadCoverage(0)).addThreshold(Threshold.MaxRuleLength(1)).addThreshold(Threshold.Timeout(-5))
    amie.thresholds.apply[Threshold.MinHeadCoverage].value shouldBe 0.001
    amie.thresholds.apply[Threshold.MaxRuleLength].value shouldBe 2
    amie.thresholds.apply[Threshold.Timeout].value shouldBe 1
  }

  it should "mine with default params" in {
    val index = Index.apply(dataset1)
    val amie = Amie()
    val rules = index.tripleMap { implicit thi =>
      amie.mine
    }
    rules.size shouldBe 123
  }

  it should "mine without duplicit predicates" in {
    val index = Index.apply(dataset1)
    val amie = Amie().addConstraint(RuleConstraint.WithoutDuplicitPredicates())
    val rules = index.tripleMap { implicit thi =>
      amie.mine.sortBy(_.measures[Measure.HeadCoverage])(Ordering.by[Measure.HeadCoverage, Double](_.value).reverse)
    }
    rules.size shouldBe 67
    rules(1).measures[Measure.HeadCoverage].value shouldBe 0.22784810126582278
    rules(2).measures[Measure.HeadCoverage].value shouldBe 0.16033755274261605
  }

  it should "mine with only specified predicates" in {
    val index = Index.apply(dataset1)
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
    val index = Index.apply(dataset1)
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
    val index = Index.apply(dataset1)
    val amie = Amie().addConstraint(RuleConstraint.WithoutDuplicitPredicates()).addConstraint(RuleConstraint.WithInstances(false))
    val rules = index.tripleMap { implicit thi =>
      amie.mine
    }
    rules.size shouldBe 20634
  }

  it should "mine with instances quickly with evaluated lazy vals" in {
    val index1 = Index.apply(dataset1)
    val index2 = Index.apply(dataset1).withEvaluatedLazyVals
    val amie = Amie().addConstraint(RuleConstraint.WithoutDuplicitPredicates()).addConstraint(RuleConstraint.WithInstances(false))
    val time1 = index1.tripleMap { implicit thi =>
      val time = System.nanoTime()
      amie.mine
      System.nanoTime() - time
    }
    val time2 = index2.tripleMap { implicit thi =>
      val time = System.nanoTime()
      amie.mine
      System.nanoTime() - time
    }
    time1 should be > time2
  }

  it should "mine with instances and with duplicit predicates" in {
    val index = Index.apply(dataset1)
    val amie = Amie().addConstraint(RuleConstraint.WithInstances(false))
    val rules = index.tripleMap { implicit thi =>
      amie.mine
    }
    val rulesWithDuplicitPredicates = rules.iterator.count(x => (x.body :+ x.head).map(_.predicate).toSet.size != x.ruleLength)
    rules.size shouldBe 40960
    rulesWithDuplicitPredicates shouldBe 20326
  }

  it should "mine only with object instances" in {
    val index = Index.apply(dataset1)
    val amie = Amie().addConstraint(RuleConstraint.WithoutDuplicitPredicates()).addConstraint(RuleConstraint.WithInstances(true))
    val rules = index.tripleMap { implicit thi =>
      amie.mine.sortBy(_.measures[Measure.HeadCoverage])(Ordering.by[Measure.HeadCoverage, Double](_.value).reverse)
    }
    rules.size shouldBe 9955
    rules(1).measures[Measure.HeadCoverage].value shouldBe 0.22784810126582278
  }

  it should "mine with min length" in {
    val index = Index.apply(dataset1)
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
    val index = Index.apply(dataset1)
    val amie = Amie().addConstraint(RuleConstraint.WithoutDuplicitPredicates()).addThreshold(Threshold.MinHeadSize(1000))
    val rules = index.tripleMap { implicit thi =>
      amie.mine
    }
    rules.size shouldBe 11
    rules.forall(_.measures[Measure.HeadSize].value >= 1000) shouldBe true
  }

  it should "mine with topK threshold" in {
    val index = Index.apply(dataset1)
    val amie = Amie().addConstraint(RuleConstraint.WithoutDuplicitPredicates()).addConstraint(RuleConstraint.WithInstances(false)).addThreshold(Threshold.TopK(10))
    val rules = index.tripleMap { implicit thi =>
      amie.mine.sortBy(_.measures[Measure.HeadCoverage])(Ordering.by[Measure.HeadCoverage, Double](_.value).reverse)
    }
    rules.size shouldBe 10
    rules(1).measures[Measure.HeadCoverage].value shouldBe 0.22784810126582278
  }

  it should "count confidence" in {
    val index = Index.apply(dataset1)
    val amie = Amie().addConstraint(RuleConstraint.WithoutDuplicitPredicates())
    val rules = index.tripleMap { implicit thi =>
      amie.mine.map(_.withConfidence(0.2)).filter(_.measures.exists[Measure.Confidence])
    }
    rules.size shouldBe 6
    rules.forall(_.measures[Measure.Confidence].value >= 0.2) shouldBe true
  }

  it should "mine with timemout" in {
    val timeoutPrinted = new AtomicBoolean(false)
    val customLogger = CustomLogger("test") { (msg, _) =>
      if (msg.contains("timeout limit")) timeoutPrinted.set(true)
    }
    val index = Index.apply(dataset1)
    val amie = Amie(customLogger).addThreshold(Threshold.MaxRuleLength(5)).addThreshold(Threshold.Timeout(1)).addConstraint(RuleConstraint.WithInstances(false))
    index.tripleMap { implicit thi =>
      amie.mine
    }
    timeoutPrinted.get() shouldBe true
  }

  it should "mine with a rule pattern" in {
    val index = Index.apply(dataset1)
    val amie = Amie().addConstraint(RuleConstraint.WithoutDuplicitPredicates()).addConstraint(RuleConstraint.WithInstances(true))
    //livesIn antecedent
    var pattern = index.tripleItemMap { implicit tihi =>
      AtomPattern(predicate = TripleItem.Uri("livesIn")) =>: None
    }
    var rules = index.tripleMap { implicit thi =>
      amie.addPattern(pattern).mine
    }
    index.tripleItemMap { implicit mapper =>
      rules.map(_.body.last.predicate).map(mapper.getTripleItem) should contain only TripleItem.Uri("livesIn")
      rules.size shouldBe 1091
    }
    //constant in object
    pattern = index.tripleItemMap { implicit tihi =>
      AtomPattern(predicate = TripleItem.Uri("livesIn"), `object` = TripleItem.Uri("Islamabad")) =>: None
    }
    rules = index.tripleMap { implicit thi =>
      amie.addPattern(pattern).mine
    }
    index.tripleItemMap { implicit mapper =>
      rules.map(_.body.last.`object`) should contain only Atom.Constant(mapper.getIndex(TripleItem.Uri("Islamabad")))
      rules.size shouldBe 10
    }
    //variable in object
    pattern = index.tripleItemMap { implicit tihi =>
      AtomPattern(predicate = TripleItem.Uri("livesIn"), `object` = 'b') =>: None
    }
    rules = index.tripleMap { implicit thi =>
      amie.addPattern(pattern).mine
    }
    index.tripleItemMap { implicit mapper =>
      rules.map(_.body.last.`object`) should contain only Atom.Variable('b')
      rules.size shouldBe 543
    }
    //any variable in object
    pattern = index.tripleItemMap { implicit tihi =>
      AtomPattern(predicate = TripleItem.Uri("livesIn"), `object` = AnyVariable) =>: None
    }
    rules = index.tripleMap { implicit thi =>
      amie.addPattern(pattern).mine
    }
    index.tripleItemMap { implicit mapper =>
      rules.map(_.body.last.`object`) should contain allOf(Atom.Variable('a'), Atom.Variable('b'))
      rules.size shouldBe 546
    }
    //any constant in object
    pattern = index.tripleItemMap { implicit tihi =>
      AtomPattern(predicate = TripleItem.Uri("livesIn"), `object` = AnyConstant) =>: None
    }
    rules = index.tripleMap { implicit thi =>
      amie.addPattern(pattern).mine
    }
    index.tripleItemMap { implicit mapper =>
      rules.map(_.body.last.`object`.isInstanceOf[Atom.Constant]) should contain only true
      rules.size shouldBe 545
    }
    //specified consequent
    pattern = index.tripleItemMap { implicit tihi =>
      AtomPattern(predicate = TripleItem.Uri("livesIn")) =>: AtomPattern(predicate = TripleItem.Uri("hasAcademicAdvisor"))
    }
    rules = index.tripleMap { implicit thi =>
      amie.addPattern(pattern).mine
    }
    index.tripleItemMap { implicit mapper =>
      val desiredCouple = TripleItem.Uri("livesIn") -> TripleItem.Uri("hasAcademicAdvisor")
      rules.map(x => mapper.getTripleItem(x.body.last.predicate) -> mapper.getTripleItem(x.head.predicate)) should contain only desiredCouple
      rules.size shouldBe 20
    }
    //two patterns in body
    pattern = index.tripleItemMap { implicit tihi =>
      AtomPattern(predicate = TripleItem.Uri("diedIn")) &: AtomPattern(predicate = TripleItem.Uri("livesIn")) =>: AtomPattern(predicate = TripleItem.Uri("hasAcademicAdvisor"))
    }
    rules = index.tripleMap { implicit thi =>
      amie.addPattern(pattern).mine
    }
    index.tripleItemMap { implicit mapper =>
      rules.flatMap(x => x.body.map(_.predicate) :+ x.head.predicate).map(mapper.getTripleItem) should contain only(TripleItem.Uri("diedIn"), TripleItem.Uri("livesIn"), TripleItem.Uri("hasAcademicAdvisor"))
      rules.size shouldBe 2
    }
    //exact pattern
    pattern = index.tripleItemMap { implicit tihi =>
      (AtomPattern(predicate = TripleItem.Uri("livesIn")) =>: None).withExact()
    }
    rules = index.tripleMap { implicit thi =>
      amie.addPattern(pattern).mine
    }
    index.tripleItemMap { implicit mapper =>
      rules.map(_.ruleLength) should contain only 2
      rules.size shouldBe 4
    }
    //oneOf pattern
    pattern = index.tripleItemMap { implicit tihi =>
      AtomPattern(predicate = OneOf(TripleItem.Uri("livesIn"), TripleItem.Uri("diedIn"))) =>: None
    }
    rules = index.tripleMap { implicit thi =>
      amie.addPattern(pattern).mine
    }
    index.tripleItemMap { implicit mapper =>
      rules.map(_.body.last.predicate).map(mapper.getTripleItem) should contain only(TripleItem.Uri("livesIn"), TripleItem.Uri("diedIn"))
      rules.size shouldBe 1400
    }
    //noneOf pattern
    pattern = index.tripleItemMap { implicit tihi =>
      AtomPattern(predicate = NoneOf(TripleItem.Uri("participatedIn"), TripleItem.Uri("imports"))) =>: None
    }
    rules = index.tripleMap { implicit thi =>
      amie.addPattern(pattern).mine
    }
    index.tripleItemMap { implicit mapper =>
      rules.map(_.body.last.predicate).map(mapper.getTripleItem) should contain noneOf(TripleItem.Uri("participatedIn"), TripleItem.Uri("imports"))
      rules.size shouldBe 6786
    }
    //several patterns
    val amie2 = index.tripleItemMap { implicit mapper =>
      Amie()
        .addConstraint(RuleConstraint.WithoutDuplicitPredicates())
        .addPattern(AtomPattern(predicate = TripleItem.Uri("actedIn")))
        .addPattern(AtomPattern(predicate = TripleItem.Uri("directed")))
    }
    rules = index.tripleMap { implicit thi =>
      amie2.mine
    }
    index.tripleItemMap { implicit mapper =>
      rules.map(_.head.predicate).map(mapper.getTripleItem) should contain only(TripleItem.Uri("actedIn"), TripleItem.Uri("directed"))
      rules.size shouldBe 11
    }
  }

  it should "mine across two graphs" in {
    val index = Index.apply(dataset2)
    val amie = Amie().addConstraint(RuleConstraint.WithoutDuplicitPredicates())
    val rules = index.tripleMap { implicit thi =>
      amie.mine
    }
    index.tripleItemMap { implicit mapper =>
      rules.map(_.head.predicate).map(mapper.getTripleItem) should contain allOf(TripleItem.Uri("http://cs.dbpedia.org/property/hudba"), TripleItem.Uri("hasCapital"))
    }
    rules.size shouldBe 391
  }

  it should "mine across two graphs with pattern" in {
    val index = Index.apply(dataset2)
    val patterns: List[RulePattern] = index.tripleItemMap { implicit mapper =>
      List(
        AtomPattern(graph = TripleItem.Uri("yago")),
        AtomPattern(graph = AtomPattern.AtomItemPattern.OneOf(TripleItem.Uri("yago"))),
        AtomPattern(graph = AtomPattern.AtomItemPattern.NoneOf(TripleItem.Uri("dbpedia")))
      )
    }
    for (pattern <- patterns) {
      val amie = Amie().addConstraint(RuleConstraint.WithoutDuplicitPredicates()).addPattern(pattern)
      val rules = index.tripleMap { implicit thi =>
        amie.mine.map(_.head.predicate).map(thi.getGraphs).map(_.iterator.toSeq)
      }
      index.tripleItemMap { implicit mapper =>
        rules.flatten.map(mapper.getTripleItem) should contain only TripleItem.Uri("yago")
      }
      rules.size shouldBe 67
    }
  }

  /*it should "test" in {
    Debugger() { implicit debugger =>
      val index = Index.apply(dataset1)
      val amie = index.tripleItemMap { implicit mapper =>
        Amie()
          .addConstraint(RuleConstraint.WithoutDuplicitPredicates())
          .addPattern(AtomPattern(predicate = TripleItem.Uri("actedIn")))
          .addPattern(AtomPattern(predicate = TripleItem.Uri("directed")))
      }
      val rules = index.tripleMap { implicit thi =>
        amie.mine
      }
      index.tripleItemMap { implicit tihi =>
        rules.foreach(x => println(Stringifier(x)))
      }
      println(rules.size)
    }
    HowLong.flushAllResults()
  }*/

}