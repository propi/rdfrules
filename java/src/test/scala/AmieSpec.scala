import java.util

import GraphSpec.dataDbpedia
import com.github.propi.rdfrules.java.algorithm.{Debugger, RulesMining}
import com.github.propi.rdfrules.java.data.{Dataset, Graph, TripleItem}
import com.github.propi.rdfrules.java.rule.RulePattern
import com.github.propi.rdfrules.java.rule.RulePattern.AtomPattern
import com.github.propi.rdfrules.rule.{RuleConstraint, Threshold}
import org.apache.jena.riot.Lang
import org.scalatest.{FlatSpec, Inside, Matchers}

/**
  * Created by Vaclav Zeman on 14. 3. 2018.
  */
class AmieSpec extends FlatSpec with Matchers with Inside {

  private lazy val dataset1 = Dataset.fromTsv(GraphSpec.dataYago)

  private lazy val dataset2 = Dataset.empty().add(Graph.fromTsv(new TripleItem.LongUri("yago"), GraphSpec.dataYago)).add(Graph.fromRdfLang(new TripleItem.LongUri("dbpedia"), dataDbpedia, Lang.TTL))

  "Amie" should "be created" in {
    var amie = RulesMining.amie()
    amie.asScala().thresholds.get[Threshold.MinHeadSize] shouldBe Some(Threshold.MinHeadSize(100))
    amie.asScala().thresholds.apply[Threshold.MinHeadSize].value shouldBe 100
    amie.asScala().thresholds.iterator.size shouldBe 3
    amie = amie.withTopK(10)
    amie.asScala().thresholds.iterator.size shouldBe 4
    amie.asScala().thresholds.apply[Threshold.TopK].value shouldBe 10
    amie.asScala().constraints.iterator.size shouldBe 0
    amie = amie.withInstances(true)
    amie.asScala().constraints.iterator.size shouldBe 1
    amie.asScala().constraints.apply[RuleConstraint.WithInstances].onlyObjects shouldBe true
    amie = amie.addPattern(RulePattern.create(new RulePattern.AtomPattern()))
    amie.asScala().patterns should not be empty
    amie.asScala().patterns.head.exact shouldBe false
    amie.asScala().patterns.head.consequent should not be empty
    amie.asScala().patterns.head.antecedent shouldBe empty
  }

  it should "mine with default params" in {
    Debugger.use((t: Debugger) => {
      val index = dataset1.index(t)
      val rules = index.mine(RulesMining.amie(t))
      rules.size shouldBe 123
    })
  }

  it should "mine without duplicit predicates" in {
    val rules = dataset1.mine(RulesMining.amie().withoutDuplicitPredicates())
    rules.size shouldBe 67
  }

  it should "mine with only specified predicates" in {
    val set = new util.HashSet[TripleItem.Uri]()
    set.add(new TripleItem.LongUri("imports"))
    set.add(new TripleItem.LongUri("exports"))
    set.add(new TripleItem.LongUri("dealsWith"))
    val rules = dataset1.mine(RulesMining.amie().withoutDuplicitPredicates().withOnlyPredicates(set))
    rules.size shouldBe 8
  }

  it should "mine without specified predicates" in {
    val set = new util.HashSet[TripleItem.Uri]()
    set.add(new TripleItem.LongUri("imports"))
    set.add(new TripleItem.LongUri("exports"))
    set.add(new TripleItem.LongUri("dealsWith"))
    val rules = dataset1.mine(RulesMining.amie().withoutDuplicitPredicates().withoutPredicates(set))
    rules.size shouldBe 59
  }

  it should "mine with instances" in {
    val rules = dataset1.mine(RulesMining.amie().withoutDuplicitPredicates().withInstances(false))
    rules.size shouldBe 20634
  }

  it should "mine with instances and with duplicit predicates" in {
    val rules = dataset1.mine(RulesMining.amie().withInstances(false))
    rules.size shouldBe 40960
  }

  it should "mine only with object instances" in {
    val rules = dataset1.mine(RulesMining.amie().withoutDuplicitPredicates().withInstances(true))
    rules.size shouldBe 9955
  }

  it should "mine with min length" in {
    val rules = dataset1.mine(RulesMining.amie().withoutDuplicitPredicates().withMaxRuleLength(2))
    rules.size shouldBe 30
  }

  it should "mine with min head size" in {
    val rules = dataset1.mine(RulesMining.amie().withoutDuplicitPredicates().withMinHeadSize(1000))
    rules.size shouldBe 11
  }

  it should "mine with topK threshold" in {
    val rules = dataset1.mine(RulesMining.amie().withoutDuplicitPredicates().withInstances(false).withTopK(10))
    rules.size shouldBe 10
  }

  it should "mine with a rule pattern" in {
    val amie = RulesMining.amie().withoutDuplicitPredicates().withInstances(true)
    val index = dataset1.index()
    //livesIn antecedent
    var pattern = RulePattern.create().prependBodyAtom(new AtomPattern().withPredicate(new TripleItem.LongUri("livesIn")))
    index.mine(amie.addPattern(pattern)).size() shouldBe 1091
    //constant in object
    pattern = RulePattern.create().prependBodyAtom(new AtomPattern()
      .withPredicate(new TripleItem.LongUri("livesIn"))
      .withObject(new TripleItem.LongUri("Islamabad"))
    )
    index.mine(amie.addPattern(pattern)).size() shouldBe 10
    //variable in object
    pattern = RulePattern.create().prependBodyAtom(new AtomPattern()
      .withPredicate(new TripleItem.LongUri("livesIn"))
      .withObject('b')
    )
    index.mine(amie.addPattern(pattern)).size() shouldBe 543
    //any variable in object
    pattern = RulePattern.create().prependBodyAtom(new AtomPattern()
      .withPredicate(new TripleItem.LongUri("livesIn"))
      .withObject(RulePattern.AnyVariable.instance)
    )
    index.mine(amie.addPattern(pattern)).size() shouldBe 546
    //any constant in object
    pattern = RulePattern.create().prependBodyAtom(new AtomPattern()
      .withPredicate(new TripleItem.LongUri("livesIn"))
      .withObject(RulePattern.AnyConstant.instance)
    )
    index.mine(amie.addPattern(pattern)).size() shouldBe 545
    //specified consequent
    pattern = RulePattern.create(new AtomPattern().withPredicate(new TripleItem.LongUri("hasAcademicAdvisor"))).prependBodyAtom(new AtomPattern()
      .withPredicate(new TripleItem.LongUri("livesIn"))
    )
    index.mine(amie.addPattern(pattern)).size() shouldBe 20
    //two patterns in body
    pattern = RulePattern.create(new AtomPattern().withPredicate(new TripleItem.LongUri("hasAcademicAdvisor"))).prependBodyAtom(new AtomPattern()
      .withPredicate(new TripleItem.LongUri("livesIn"))
    ).prependBodyAtom(new AtomPattern()
      .withPredicate(new TripleItem.LongUri("diedIn"))
    )
    index.mine(amie.addPattern(pattern)).size() shouldBe 2
    //exact pattern
    pattern = RulePattern.create().prependBodyAtom(new AtomPattern()
      .withPredicate(new RulePattern.Constant(new TripleItem.LongUri("livesIn")))
    ).withExact()
    index.mine(amie.addPattern(pattern)).size() shouldBe 4
    //oneOf pattern
    pattern = RulePattern.create().prependBodyAtom(new AtomPattern()
      .withPredicate(new RulePattern.OneOf(
        new RulePattern.Constant(new TripleItem.LongUri("livesIn")),
        new RulePattern.Constant(new TripleItem.LongUri("diedIn"))
      ))
    )
    index.mine(amie.addPattern(pattern)).size() shouldBe 1400
    //noneOf pattern
    pattern = RulePattern.create().prependBodyAtom(new AtomPattern()
      .withPredicate(new RulePattern.NoneOf(
        new RulePattern.Constant(new TripleItem.LongUri("participatedIn")),
        new RulePattern.Constant(new TripleItem.LongUri("imports"))
      ))
    )
    index.mine(amie.addPattern(pattern)).size() shouldBe 6786
    //several patterns
    index.mine(RulesMining.amie()
      .withoutDuplicitPredicates()
      .addPattern(RulePattern.create(new AtomPattern().withPredicate(new TripleItem.LongUri("actedIn"))))
      .addPattern(RulePattern.create(new AtomPattern().withPredicate(new TripleItem.LongUri("directed"))))
    ).size() shouldBe 11
  }

  it should "mine across two graphs" in {
    dataset2.mine(RulesMining.amie().withoutDuplicitPredicates()).size() shouldBe 391
  }

  it should "mine across two graphs with pattern" in {
    val rules = dataset2.mine(RulesMining.amie().withoutDuplicitPredicates().addPattern(
      RulePattern.create(new AtomPattern().withGraph(new TripleItem.LongUri("yago")))
    ))
    rules.size shouldBe 67
  }

}