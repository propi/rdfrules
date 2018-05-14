import java.util
import java.util.function.Consumer

import GraphSpec.dataDbpedia
import com.github.propi.rdfrules.java.algorithm.RulesMining
import com.github.propi.rdfrules.java.data.{Dataset, Graph, TripleItem}
import com.github.propi.rdfrules.java.index.{Index, TripleItemHashIndex}
import com.github.propi.rdfrules.java.rule.RulePattern
import com.github.propi.rdfrules.java.rule.RulePattern.AtomPattern
import com.github.propi.rdfrules.rule.{RuleConstraint, Threshold}
import org.apache.jena.riot.Lang
import org.scalatest.{FlatSpec, Inside, Matchers}

import scala.collection.JavaConverters._

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
    val index = dataset1.index()
    val rules = index.mine(RulesMining.amie())
    rules.size shouldBe 116
  }

  it should "mine without duplicit predicates" in {
    val rules = dataset1.mine(RulesMining.amie().withoutDuplicitPredicates())
    rules.size shouldBe 67
  }

  it should "mine with only specified predicates" in {
    dataset1.useMapper((mapper: TripleItemHashIndex) => new Consumer[Index] {
      def accept(index: Index): Unit = {
        val set = new util.HashSet[Integer]()
        set.add(mapper.getIndex(new TripleItem.LongUri("imports")))
        set.add(mapper.getIndex(new TripleItem.LongUri("exports")))
        set.add(mapper.getIndex(new TripleItem.LongUri("dealsWith")))
        val rules = index.mine(RulesMining.amie().withoutDuplicitPredicates().withOnlyPredicates(set))
        rules.size shouldBe 8
      }
    })
  }

  it should "mine without specified predicates" in {
    dataset1.useMapper((mapper: TripleItemHashIndex) => new Consumer[Index] {
      def accept(index: Index): Unit = {
        val set = new util.HashSet[Integer]()
        set.add(mapper.getIndex(new TripleItem.LongUri("imports")))
        set.add(mapper.getIndex(new TripleItem.LongUri("exports")))
        set.add(mapper.getIndex(new TripleItem.LongUri("dealsWith")))
        val rules = index.mine(RulesMining.amie().withoutDuplicitPredicates().withoutPredicates(set))
        rules.size shouldBe 59
      }
    })
  }

  it should "mine with instances" in {
    val rules = dataset1.mine(RulesMining.amie().withoutDuplicitPredicates().withInstances(false))
    rules.size shouldBe 20634
  }

  it should "mine with instances and with duplicit predicates" in {
    val rules = dataset1.mine(RulesMining.amie().withInstances(false))
    rules.size shouldBe 21674
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
    dataset1.useMapper { (mapper: TripleItemHashIndex) =>
      new Consumer[Index] {
        def accept(index: Index): Unit = {
          //livesIn antecedent
          var pattern = RulePattern.create().prependBodyAtom(new AtomPattern().withPredicate(new RulePattern.Constant(new TripleItem.LongUri("livesIn"), mapper)))
          index.mine(amie.addPattern(pattern)).size() shouldBe 1091
          //constant in object
          pattern = RulePattern.create().prependBodyAtom(new AtomPattern()
            .withPredicate(new RulePattern.Constant(new TripleItem.LongUri("livesIn"), mapper))
            .withObject(new RulePattern.Constant(new TripleItem.LongUri("Islamabad"), mapper))
          )
          index.mine(amie.addPattern(pattern)).size() shouldBe 10
          //variable in object
          pattern = RulePattern.create().prependBodyAtom(new AtomPattern()
            .withPredicate(new RulePattern.Constant(new TripleItem.LongUri("livesIn"), mapper))
            .withObject(new RulePattern.Variable('b'))
          )
          index.mine(amie.addPattern(pattern)).size() shouldBe 543
          //any variable in object
          pattern = RulePattern.create().prependBodyAtom(new AtomPattern()
            .withPredicate(new RulePattern.Constant(new TripleItem.LongUri("livesIn"), mapper))
            .withObject(RulePattern.AnyVariable.instance)
          )
          index.mine(amie.addPattern(pattern)).size() shouldBe 546
          //any constant in object
          pattern = RulePattern.create().prependBodyAtom(new AtomPattern()
            .withPredicate(new RulePattern.Constant(new TripleItem.LongUri("livesIn"), mapper))
            .withObject(RulePattern.AnyConstant.instance)
          )
          index.mine(amie.addPattern(pattern)).size() shouldBe 545
          //specified consequent
          pattern = RulePattern.create(new AtomPattern().withPredicate(new RulePattern.Constant(new TripleItem.LongUri("hasAcademicAdvisor"), mapper))).prependBodyAtom(new AtomPattern()
            .withPredicate(new RulePattern.Constant(new TripleItem.LongUri("livesIn"), mapper))
          )
          index.mine(amie.addPattern(pattern)).size() shouldBe 20
          //two patterns in body
          pattern = RulePattern.create(new AtomPattern().withPredicate(new RulePattern.Constant(new TripleItem.LongUri("hasAcademicAdvisor"), mapper))).prependBodyAtom(new AtomPattern()
            .withPredicate(new RulePattern.Constant(new TripleItem.LongUri("livesIn"), mapper))
          ).prependBodyAtom(new AtomPattern()
            .withPredicate(new RulePattern.Constant(new TripleItem.LongUri("diedIn"), mapper))
          )
          index.mine(amie.addPattern(pattern)).size() shouldBe 2
          //exact pattern
          pattern = RulePattern.create().prependBodyAtom(new AtomPattern()
            .withPredicate(new RulePattern.Constant(new TripleItem.LongUri("livesIn"), mapper))
          ).withExact()
          index.mine(amie.addPattern(pattern)).size() shouldBe 4
          //oneOf pattern
          pattern = RulePattern.create().prependBodyAtom(new AtomPattern()
            .withPredicate(new RulePattern.OneOf(Iterable[RulePattern.AtomItemPattern](
              new RulePattern.Constant(new TripleItem.LongUri("livesIn"), mapper),
              new RulePattern.Constant(new TripleItem.LongUri("diedIn"), mapper)
            ).asJava))
          )
          index.mine(amie.addPattern(pattern)).size() shouldBe 1400
          //noneOf pattern
          pattern = RulePattern.create().prependBodyAtom(new AtomPattern()
            .withPredicate(new RulePattern.NoneOf(Iterable[RulePattern.AtomItemPattern](
              new RulePattern.Constant(new TripleItem.LongUri("participatedIn"), mapper),
              new RulePattern.Constant(new TripleItem.LongUri("imports"), mapper)
            ).asJava))
          )
          index.mine(amie.addPattern(pattern)).size() shouldBe 6786
          //several patterns
          index.mine(RulesMining.amie()
            .withoutDuplicitPredicates()
            .addPattern(RulePattern.create(new AtomPattern().withPredicate(new RulePattern.Constant(new TripleItem.LongUri("actedIn"), mapper))))
            .addPattern(RulePattern.create(new AtomPattern().withPredicate(new RulePattern.Constant(new TripleItem.LongUri("directed"), mapper))))
          ).size() shouldBe 11
        }
      }
    }
  }

  it should "mine across two graphs" in {
    dataset2.mine(RulesMining.amie().withoutDuplicitPredicates()).size() shouldBe 391
  }

  it should "mine across two graphs with pattern" in {
    dataset2.useMapper { (mapper: TripleItemHashIndex) =>
      new Consumer[Index] {
        def accept(index: Index): Unit = {
          val rules = index.mine(RulesMining.amie().withoutDuplicitPredicates().addPattern(
            RulePattern.create(new AtomPattern().withGraph(new RulePattern.Constant(new TripleItem.LongUri("yago"), mapper)))
          ))
          rules.size shouldBe 67
        }
      }
    }
  }

}