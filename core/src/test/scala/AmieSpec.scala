import GraphSpec.dataDbpedia
import eu.easyminer.rdf.algorithm.RulesMining
import eu.easyminer.rdf.algorithm.amie.Amie
import eu.easyminer.rdf.data.{Dataset, Graph, RdfSource}
import org.apache.jena.riot.Lang
import org.scalatest.{FlatSpec, Inside, Matchers}
import eu.easyminer.rdf.data.formats.Tsv._
import eu.easyminer.rdf.data.formats.JenaLang._
import eu.easyminer.rdf.index.Index
import eu.easyminer.rdf.rule.ExtendedRule.ClosedRule
import eu.easyminer.rdf.rule._
import eu.easyminer.rdf.stringifier.Stringifier
import eu.easyminer.rdf.utils.{Debugger, HowLong}
import eu.easyminer.rdf.stringifier.CommonStringifiers._

/**
  * Created by Vaclav Zeman on 14. 3. 2018.
  */
class AmieSpec extends FlatSpec with Matchers with Inside {

  private lazy val dataset = Dataset() + Graph[RdfSource.Tsv.type]("yago", GraphSpec.dataYago) + Graph("dbpedia", dataDbpedia)(RdfSource.JenaLang(Lang.TTL))

  private def mine(dataset: Dataset, rulesMining: RulesMining): IndexedSeq[ClosedRule] = {
    Index.fromDataset(dataset).tripleMap { thi =>
      rulesMining.mine(thi)
    }
  }

  "Amie" should "be created" in {
    val amie = Amie()
    amie.thresholds.get[Threshold.MinSupport] shouldBe Some(Threshold.MinSupport(100))
    amie.thresholds.apply[Threshold.MinSupport].value shouldBe 100
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
    Debugger { implicit debugger =>
      val amie = Amie().addConstraint(RuleConstraint.WithInstances(false)).addThreshold(Threshold.TopK(100))
      val index = Index.fromDataset(dataset.filter(_.graph.hasSameUriAs("yago")))
      val rules = index.tripleMap { thi =>
        amie.mine(thi)
      }
      index.tripleItemMap { implicit tihi =>
        rules.foreach(x => println(Stringifier(x.asInstanceOf[Rule])))
      }
      println(rules.size)

    }
    HowLong.flushAllResults()
  }

}
