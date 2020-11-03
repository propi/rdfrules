import java.util.concurrent.atomic.AtomicBoolean

import com.github.propi.rdfrules.algorithm.amie.Amie
import com.github.propi.rdfrules.algorithm.amie.RuleCounting._
import com.github.propi.rdfrules.algorithm.consumer.{InMemoryRuleConsumer, TopKRuleConsumer}
import com.github.propi.rdfrules.data._
import com.github.propi.rdfrules.index._
import com.github.propi.rdfrules.rule.RuleConstraint.ConstantsAtPosition.ConstantsPosition
import com.github.propi.rdfrules.rule._
import com.github.propi.rdfrules.ruleset.ResolvedRule
import com.github.propi.rdfrules.utils.{CustomLogger, Debugger}
import org.apache.jena.riot.Lang
import org.scalatest.{CancelAfterFailure, FlatSpec, Inside, Matchers}

/**
  * Created by Vaclav Zeman on 14. 3. 2018.
  */
class AmieSpec extends FlatSpec with Matchers with Inside with CancelAfterFailure {

  private lazy val dataset1 = Dataset(GraphSpec.dataYago)

  private lazy val dataset2 = Dataset() + Graph("yago", GraphSpec.dataYago) + Graph("dbpedia", GraphSpec.dataDbpedia)(Lang.TTL)

  "Amie" should "be created" ignore {
    var amie = Amie()
    //amie.thresholds.get[Threshold.MinHeadSize] shouldBe Some(Threshold.MinHeadSize(100))
    //amie.thresholds.apply[Threshold.MinHeadSize].value shouldBe 100
    amie.thresholds.iterator.size shouldBe 0
    amie.constraints.iterator.size shouldBe 0
    amie = amie.addConstraint(RuleConstraint.ConstantsAtPosition(ConstantsPosition.Nowhere))
    amie.constraints.iterator.size shouldBe 1
    amie.constraints.apply[RuleConstraint.ConstantsAtPosition] shouldBe RuleConstraint.ConstantsAtPosition(ConstantsPosition.Nowhere)
    amie = amie.addPattern(RulePattern(AtomPattern(AtomPattern.AtomItemPattern.AnyConstant, AtomPattern.AtomItemPattern.AnyConstant, AtomPattern.AtomItemPattern.AnyConstant)))
    amie.patterns.isEmpty shouldBe false
    amie.patterns.head.exact shouldBe false
    amie.patterns.head.head shouldBe defined
    amie.patterns.head.body shouldBe empty
    amie = amie.addThreshold(Threshold.MinHeadCoverage(0)).addThreshold(Threshold.MaxRuleLength(1)).addThreshold(Threshold.Timeout(-5))
    amie.thresholds.apply[Threshold.MinHeadCoverage].value shouldBe 0.001
    amie.thresholds.apply[Threshold.MaxRuleLength].value shouldBe 2
    amie.thresholds.apply[Threshold.Timeout].value shouldBe 1
  }

  it should "mine with default params" ignore {
    val index = Index.apply(dataset1, false)
    val amie = Amie().addConstraint(RuleConstraint.ConstantsAtPosition(ConstantsPosition.Nowhere)).addThreshold(Threshold.MinHeadCoverage(0.01))
    val rules = index.mine(amie)
    rules.size shouldBe 124
  }

  it should "mine without duplicit predicates" ignore {
    val index = Index.apply(dataset1, false)
    val amie = Amie()
      .addConstraint(RuleConstraint.WithoutDuplicatePredicates())
      .addConstraint(RuleConstraint.ConstantsAtPosition(ConstantsPosition.Nowhere))
      .addThreshold(Threshold.MinHeadCoverage(0.01))
    val rules = index.tripleItemMap { implicit tihi =>
      index.tripleMap { implicit thi =>
        InMemoryRuleConsumer { consumer =>
          thi.subjects
          thi.objects
          amie.mine(consumer).rules.toSeq.sortBy(_.measures[Measure.HeadCoverage])(Ordering.by[Measure.HeadCoverage, Double](_.value).reverse)
        }
      }
    }
    rules.size shouldBe 67
    rules(1).measures[Measure.HeadCoverage].value shouldBe 0.22784810126582278
    rules(2).measures[Measure.HeadCoverage].value shouldBe 0.16033755274261605
  }

  it should "mine with only specified predicates" ignore {
    val index = Index.apply(dataset1, false)
    val onlyPredicates = RuleConstraint.OnlyPredicates("imports", "exports", "dealsWith")
    val amie = Amie()
      .addConstraint(RuleConstraint.WithoutDuplicatePredicates())
      .addConstraint(onlyPredicates)
      .addConstraint(RuleConstraint.ConstantsAtPosition(ConstantsPosition.Nowhere))
      .addThreshold(Threshold.MinHeadCoverage(0.01))
    index.tripleItemMap { implicit tihi =>
      val rules = index.tripleMap { implicit thi =>
        InMemoryRuleConsumer { consumer =>
          thi.subjects
          thi.objects
          amie.mine(consumer).rules.toVector.sortBy(_.measures[Measure.HeadCoverage])(Ordering.by[Measure.HeadCoverage, Double](_.value).reverse)
        }
      }
      rules.size shouldBe 8
      rules(0).measures[Measure.HeadCoverage].value shouldBe 0.22784810126582278
      rules(1).measures[Measure.HeadCoverage].value shouldBe 0.16033755274261605
      rules.iterator.flatMap(x => x.body :+ x.head).map(_.predicate).toSet should contain only(tihi.getIndex(TripleItem.Uri("imports")), tihi.getIndex(TripleItem.Uri("exports")), tihi.getIndex(TripleItem.Uri("dealsWith")))
    }
  }

  it should "mine without specified predicates" ignore {
    val index = Index.apply(dataset1, false)
    val onlyPredicates = RuleConstraint.WithoutPredicates("imports", "exports", "dealsWith")
    val amie = Amie()
      .addConstraint(RuleConstraint.WithoutDuplicatePredicates())
      .addConstraint(onlyPredicates)
      .addConstraint(RuleConstraint.ConstantsAtPosition(ConstantsPosition.Nowhere))
      .addThreshold(Threshold.MinHeadCoverage(0.01))
    index.tripleItemMap { implicit tihi =>
      val rules = index.tripleMap { implicit thi =>
        InMemoryRuleConsumer { consumer =>
          thi.subjects
          thi.objects
          amie.mine(consumer).rules.toVector
        }
      }
      rules.size shouldBe 59
      rules.iterator.flatMap(x => x.body :+ x.head).map(_.predicate).toSet should contain noneOf(tihi.getIndex(TripleItem.Uri("imports")), tihi.getIndex(TripleItem.Uri("exports")), tihi.getIndex(TripleItem.Uri("dealsWith")))
    }
  }

  it should "mine with instances" ignore {
    val index = Index.apply(dataset1, false)
    Debugger() { implicit debugger =>
      val amie = Amie().addConstraint(RuleConstraint.WithoutDuplicatePredicates()).addThreshold(Threshold.MinHeadCoverage(0.01))
      val rules = index.tripleItemMap { implicit mapper =>
        index.tripleMap { implicit thi =>
          InMemoryRuleConsumer { consumer =>
            thi.subjects
            thi.objects
            amie.mine(consumer).rules.toVector
          }
        }
      }
      rules.size shouldBe 211527
      rules.iterator.map(x => x.body.toSet -> x.head).toSet.size shouldBe 211527
    }
  }

  it should "mine with instances quickly with evaluated lazy vals" ignore {
    val index1 = Index.apply(dataset1, false)
    val index2 = Index.apply(dataset1, false).withEvaluatedLazyVals
    val amie = Amie().addConstraint(RuleConstraint.WithoutDuplicatePredicates()).addThreshold(Threshold.MinHeadCoverage(0.01))
    val time1 = index1.tripleItemMap { implicit mapper =>
      index1.tripleMap { implicit thi =>
        InMemoryRuleConsumer { consumer =>
          thi.subjects
          thi.objects
          System.gc()
          val time = System.nanoTime()
          amie.mine(consumer)
          System.nanoTime() - time
        }
      }
    }
    val time2 = index2.tripleItemMap { implicit mapper =>
      index2.tripleMap { implicit thi =>
        InMemoryRuleConsumer { consumer =>
          thi.subjects
          thi.objects
          System.gc()
          val time = System.nanoTime()
          amie.mine(consumer)
          System.nanoTime() - time
        }
      }
    }
    time1 should be > time2
  }

  it should "mine with instances and with duplicit predicates" ignore {
    Debugger() { implicit debugger =>
      val index = Index.apply(dataset1, false)
      val amie = Amie().addThreshold(Threshold.MinHeadCoverage(0.02))
      val rules = index.tripleItemMap { implicit mapper =>
        index.tripleMap { implicit thi =>
          InMemoryRuleConsumer { consumer =>
            thi.subjects
            thi.objects
            amie.mine(consumer).rules.toVector
          }
        }
      }
      val rulesWithDuplicitPredicates = rules.iterator.count(x => (x.body :+ x.head).map(_.predicate).toSet.size != x.ruleLength)
      rules.size shouldBe 51159
      rules.iterator.map(x => x.body.toSet -> x.head).toSet.size shouldBe 51159
      rulesWithDuplicitPredicates shouldBe 40146
    }
  }

  it should "mine only with object instances" ignore {
    Debugger() { implicit debugger =>
      val index = Index.apply(dataset1, false)
      val amie = Amie()
        .addConstraint(RuleConstraint.WithoutDuplicatePredicates())
        .addConstraint(RuleConstraint.ConstantsAtPosition(ConstantsPosition.Object))
        .addThreshold(Threshold.MinHeadCoverage(0.01))
      val rules = index.tripleItemMap { implicit mapper =>
        index.tripleMap { implicit thi =>
          InMemoryRuleConsumer { consumer =>
            thi.subjects
            thi.objects
            amie.mine(consumer).rules.toVector.sortBy(_.measures[Measure.HeadCoverage])(Ordering.by[Measure.HeadCoverage, Double](_.value).reverse)
          }
        }
      }
      rules.forall(_.measures[Measure.HeadCoverage].value >= 0.01) shouldBe true
      rules.size shouldBe 74993
      rules(1).measures[Measure.HeadCoverage].value shouldBe 0.22784810126582278
    }
  }

  it should "mine with min length" ignore {
    val index = Index.apply(dataset1, false)
    var amie = Amie()
      .addConstraint(RuleConstraint.WithoutDuplicatePredicates())
      .addThreshold(Threshold.MaxRuleLength(2))
      .addConstraint(RuleConstraint.ConstantsAtPosition(ConstantsPosition.Nowhere))
      .addThreshold(Threshold.MinHeadCoverage(0.01))
    var rules = index.tripleItemMap { implicit mapper =>
      index.tripleMap { implicit thi =>
        InMemoryRuleConsumer { consumer =>
          thi.subjects
          thi.objects
          amie.mine(consumer).rules.toVector
        }
      }
    }
    rules.size shouldBe 30
    amie = Amie()
      .addConstraint(RuleConstraint.WithoutDuplicatePredicates())
      .addThreshold(Threshold.MaxRuleLength(4))
      .addConstraint(RuleConstraint.ConstantsAtPosition(ConstantsPosition.Nowhere))
      .addThreshold(Threshold.MinHeadCoverage(0.01))
    rules = index.tripleItemMap { implicit mapper =>
      index.tripleMap { implicit thi =>
        InMemoryRuleConsumer { consumer =>
          thi.subjects
          thi.objects
          amie.mine(consumer).rules.toVector
        }
      }
    }
    rules.size shouldBe 139
  }

  it should "mine with min head size" ignore {
    val index = Index.apply(dataset1, false)
    val amie = Amie()
      .addConstraint(RuleConstraint.WithoutDuplicatePredicates())
      .addThreshold(Threshold.MinHeadSize(1000))
      .addConstraint(RuleConstraint.ConstantsAtPosition(ConstantsPosition.Nowhere))
      .addThreshold(Threshold.MinHeadCoverage(0.01))
    val rules = index.tripleItemMap { implicit mapper =>
      index.tripleMap { implicit thi =>
        InMemoryRuleConsumer { consumer =>
          thi.subjects
          thi.objects
          amie.mine(consumer).rules.toVector
        }
      }
    }
    rules.size shouldBe 11
    rules.forall(_.measures[Measure.HeadSize].value >= 1000) shouldBe true
  }

  it should "mine with topK threshold" ignore {
    Debugger() { implicit debugger =>
      val index = Index.apply(dataset1, false)
      val amie = Amie()
        .addConstraint(RuleConstraint.WithoutDuplicatePredicates())
        .addThreshold(Threshold.MinHeadCoverage(0.01))
      val rules = index.tripleItemMap { implicit mapper =>
        index.tripleMap { implicit thi =>
          TopKRuleConsumer(10) { consumer =>
            thi.subjects
            thi.objects
            amie.mine(consumer).rules.toVector.sortBy(_.measures[Measure.HeadCoverage])(Ordering.by[Measure.HeadCoverage, Double](_.value).reverse)
          }
        }
      }
      rules.size shouldBe 10
      rules(1).measures[Measure.HeadCoverage].value shouldBe 0.22784810126582278
    }
  }

  it should "count confidence" ignore {
    val index = Index.apply(dataset1, false)
    val amie = Amie()
      .addConstraint(RuleConstraint.WithoutDuplicatePredicates())
      .addConstraint(RuleConstraint.ConstantsAtPosition(ConstantsPosition.Nowhere))
      .addThreshold(Threshold.MinHeadCoverage(0.01))
    val rules = index.tripleItemMap { implicit mapper =>
      index.tripleMap { implicit thi =>
        InMemoryRuleConsumer { consumer =>
          thi.subjects
          thi.objects
          amie.mine(consumer).rules.view.map(_.withConfidence(0.2)).filter(_.measures.exists[Measure.Confidence]).toVector
        }
      }
    }
    rules.size shouldBe 7
    rules.forall(_.measures[Measure.Confidence].value >= 0.2) shouldBe true
  }

  it should "mine with timemout" ignore {
    val timeoutPrinted = new AtomicBoolean(false)
    val customLogger = CustomLogger("test") { (msg, _) =>
      if (msg.contains("timeout limit")) timeoutPrinted.set(true)
    }
    Debugger(customLogger) { implicit debugger =>
      val index = Index.apply(dataset1, false)
      val amie = Amie()
        .addThreshold(Threshold.MaxRuleLength(5))
        .addThreshold(Threshold.Timeout(1))
        .addThreshold(Threshold.MinHeadCoverage(0.01))
      index.tripleItemMap { implicit mapper =>
        index.tripleMap { implicit thi =>
          InMemoryRuleConsumer { consumer =>
            thi.subjects
            thi.objects
            amie.mine(consumer)
          }
        }
      }
    }
    timeoutPrinted.get() shouldBe true
  }

  it should "mine with a rule pattern" in {
    val index = Index.apply(dataset1, false)
    val amie = Amie().addConstraint(RuleConstraint.WithoutDuplicatePredicates()).addThreshold(Threshold.MinHeadCoverage(0.01))
    //livesIn antecedent
    var pattern: RulePattern = AtomPattern(predicate = TripleItem.Uri("livesIn")) =>: None
    index.tripleItemMap { implicit mapper =>
      val rules = index.tripleMap { implicit thi =>
        InMemoryRuleConsumer { consumer =>
          thi.subjects
          thi.objects
          amie.addPattern(pattern).mine(consumer).rules.toVector
        }
      }
      for (rule <- rules) {
        rule.body.map(_.predicate).map(mapper.getTripleItem) should contain(TripleItem.Uri("livesIn"))
      }
      rules.size shouldBe 10826
    }
    //constant in object
    pattern = AtomPattern(predicate = TripleItem.Uri("livesIn"), `object` = TripleItem.Uri("Islamabad")) =>: None
    index.tripleItemMap { implicit mapper =>
      val rules = index.tripleMap { implicit thi =>
        InMemoryRuleConsumer { consumer =>
          amie.addPattern(pattern).mine(consumer).rules.toVector
        }
      }
      for (rule <- rules) {
        rule.body.map(_.`object`) should contain(Atom.Constant(mapper.getIndex(TripleItem.Uri("Islamabad"))))
      }
      rules.size shouldBe 15
    }
    //variable in object
    pattern = AtomPattern(predicate = TripleItem.Uri("livesIn"), `object` = 'b') =>: None
    index.tripleItemMap { implicit mapper =>
      val rules = index.tripleMap { implicit thi =>
        InMemoryRuleConsumer { consumer =>
          amie.addPattern(pattern).mine(consumer).rules.toVector
        }
      }
      for (rule <- rules) {
        rule.body.map(_.`object`) should contain(Atom.Item('b'))
      }
      rules.size shouldBe 1549
    }
    //any variable in object
    pattern = AtomPattern(predicate = TripleItem.Uri("livesIn"), `object` = AnyVariable) =>: None
    index.tripleItemMap { implicit mapper =>
      val rules = index.tripleMap { implicit thi =>
        InMemoryRuleConsumer { consumer =>
          amie.addPattern(pattern).mine(consumer).rules.toVector
        }
      }
      for (rule <- rules) {
        rule.body.map(x => mapper.getTripleItem(x.predicate) -> x.`object`) should contain atLeastOneOf(TripleItem.Uri("livesIn") -> Atom.Item('a'), TripleItem.Uri("livesIn") -> Atom.Item('b'), TripleItem.Uri("livesIn") -> Atom.Item('c'))
      }
      rules.size shouldBe 10229
    }
    //any constant in object
    pattern = AtomPattern(predicate = TripleItem.Uri("livesIn"), `object` = AnyConstant) =>: None
    index.tripleItemMap { implicit mapper =>
      val rules = index.tripleMap { implicit thi =>
        InMemoryRuleConsumer { consumer =>
          amie.addPattern(pattern).mine(consumer).rules.toVector
        }
      }
      for (rule <- rules) {
        rule.body.map(_.`object`.isInstanceOf[Atom.Constant]) should contain(true)
      }
      rules.size shouldBe 597
    }
    //specified consequent
    pattern = AtomPattern(predicate = TripleItem.Uri("livesIn")) =>: AtomPattern(predicate = TripleItem.Uri("hasAcademicAdvisor"))
    index.tripleItemMap { implicit mapper =>
      val rules = index.tripleMap { implicit thi =>
        InMemoryRuleConsumer { consumer =>
          amie.addPattern(pattern).mine(consumer).rules.toVector
        }
      }
      val desiredCouple = Some(TripleItem.Uri("livesIn")) -> TripleItem.Uri("hasAcademicAdvisor")
      rules.map(x => x.body.map(_.predicate).map(mapper.getTripleItem).find(_ == TripleItem.Uri("livesIn")) -> mapper.getTripleItem(x.head.predicate)) should contain only desiredCouple
      rules.size shouldBe 33
    }
    //two patterns in body
    pattern = AtomPattern(predicate = TripleItem.Uri("diedIn")) &: AtomPattern(predicate = TripleItem.Uri("livesIn")) =>: AtomPattern(predicate = TripleItem.Uri("hasAcademicAdvisor"))
    index.tripleItemMap { implicit mapper =>
      val rules = index.tripleMap { implicit thi =>
        InMemoryRuleConsumer { consumer =>
          amie.addPattern(pattern).mine(consumer).rules.toVector
        }
      }
      rules.flatMap(x => x.body.map(_.predicate) :+ x.head.predicate).map(mapper.getTripleItem) should contain only(TripleItem.Uri("diedIn"), TripleItem.Uri("livesIn"), TripleItem.Uri("hasAcademicAdvisor"))
      rules.map(ResolvedRule.apply(_)).foreach(println)
      rules.size shouldBe 4
    }
    //exact pattern
    pattern = (AtomPattern(predicate = TripleItem.Uri("livesIn")) =>: None).withExact()
    index.tripleItemMap { implicit mapper =>
      val rules = index.tripleMap { implicit thi =>
        InMemoryRuleConsumer { consumer =>
          amie.addPattern(pattern).mine(consumer).rules.toVector
        }
      }
      rules.map(_.ruleLength) should contain only 2
      rules.size shouldBe 50
    }
    //oneOf pattern
    pattern = AtomPattern(predicate = OneOf(TripleItem.Uri("livesIn"), TripleItem.Uri("diedIn"))) =>: None
    index.tripleItemMap { implicit mapper =>
      val rules = index.tripleMap { implicit thi =>
        InMemoryRuleConsumer { consumer =>
          amie.addPattern(pattern).mine(consumer).rules.toVector
        }
      }
      for (rule <- rules) {
        rule.body.map(_.predicate).map(mapper.getTripleItem) should contain atLeastOneOf(TripleItem.Uri("livesIn"), TripleItem.Uri("diedIn"))
      }
      rules.size shouldBe 11443
    }
    //noneOf pattern
    pattern = AtomPattern(predicate = NoneOf(TripleItem.Uri("participatedIn"), TripleItem.Uri("imports"))) =>: None
    index.tripleItemMap { implicit mapper =>
      val rules = index.tripleMap { implicit thi =>
        InMemoryRuleConsumer { consumer =>
          amie.addPattern(pattern).addThreshold(Threshold.MaxRuleLength(2)).mine(consumer).rules.toVector
        }
      }
      rules.map(_.body.last.predicate).map(mapper.getTripleItem) should contain noneOf(TripleItem.Uri("participatedIn"), TripleItem.Uri("imports"))
      rules.size shouldBe 1784
    }
    //several patterns
    val amie2 = Amie()
      .addConstraint(RuleConstraint.WithoutDuplicatePredicates())
      .addPattern(AtomPattern(predicate = TripleItem.Uri("actedIn")))
      .addPattern(AtomPattern(predicate = TripleItem.Uri("directed")))
      .addThreshold(Threshold.MinHeadCoverage(0.01))
    index.tripleItemMap { implicit mapper =>
      val rules = index.tripleMap { implicit thi =>
        InMemoryRuleConsumer { consumer =>
          amie2.mine(consumer).rules.toVector
        }
      }
      rules.map(_.head.predicate).map(mapper.getTripleItem) should contain only(TripleItem.Uri("actedIn"), TripleItem.Uri("directed"))
      rules.size shouldBe 36
    }
  }

  it should "mine across two graphs" ignore {
    val index = Index.apply(dataset2, false)
    val amie = Amie()
      .addConstraint(RuleConstraint.WithoutDuplicatePredicates())
      .addConstraint(RuleConstraint.ConstantsAtPosition(ConstantsPosition.Nowhere))
      .addThreshold(Threshold.MinHeadCoverage(0.01))
    index.tripleItemMap { implicit mapper =>
      val rules = index.tripleMap { implicit thi =>
        InMemoryRuleConsumer { consumer =>
          thi.subjects
          thi.objects
          amie.mine(consumer).rules.toVector
        }
      }
      rules.map(_.head.predicate).map(mapper.getTripleItem) should contain allOf(TripleItem.Uri("http://cs.dbpedia.org/property/hudba"), TripleItem.Uri("hasCapital"))
      rules.size shouldBe 400
    }
  }

  it should "mine across two graphs with pattern" ignore {
    val index = Index.apply(dataset2, false)
    val patterns: List[RulePattern] = List(
      AtomPattern(graph = TripleItem.Uri("yago")),
      AtomPattern(graph = AtomPattern.AtomItemPattern.OneOf(TripleItem.Uri("yago"))),
      AtomPattern(graph = AtomPattern.AtomItemPattern.NoneOf(TripleItem.Uri("dbpedia")))
    )
    for (pattern <- patterns) {
      val amie = Amie()
        .addConstraint(RuleConstraint.WithoutDuplicatePredicates())
        .addPattern(pattern)
        .addConstraint(RuleConstraint.ConstantsAtPosition(ConstantsPosition.Nowhere))
        .addThreshold(Threshold.MinHeadCoverage(0.01))
      index.tripleItemMap { implicit mapper =>
        val rules = index.tripleMap { implicit thi =>
          InMemoryRuleConsumer { consumer =>
            thi.subjects
            thi.objects
            amie.mine(consumer).rules.view.map(_.head.predicate).map(thi.getGraphs).map(_.iterator.toSeq).toVector
          }
        }
        rules.flatten.map(mapper.getTripleItem) should contain only TripleItem.Uri("yago")
        rules.size shouldBe 67
      }
    }
  }

}