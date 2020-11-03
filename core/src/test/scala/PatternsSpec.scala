import com.github.propi.rdfrules.algorithm.amie.Amie
import com.github.propi.rdfrules.data._
import com.github.propi.rdfrules.rule.RuleConstraint.ConstantsAtPosition.ConstantsPosition
import com.github.propi.rdfrules.rule._
import com.github.propi.rdfrules.utils.Debugger
import org.scalatest.{CancelAfterFailure, FlatSpec, Inside, Matchers}

/**
  * Created by Vaclav Zeman on 18. 4. 2018.
  */
class PatternsSpec extends FlatSpec with Matchers with Inside with CancelAfterFailure {

  private lazy val index = {
    Debugger() { implicit debugger =>
      val index = Dataset(Graph("yago", GraphSpec.dataYago)).index
      index.tripleItemMap(_ => Unit)
      index.tripleMap(_ => Unit)
      index
    }
  }

  private def amie(implicit debugger: Debugger) = Amie()
    .addThreshold(Threshold.MinHeadCoverage(0.01))
    .addConstraint(RuleConstraint.WithoutDuplicatePredicates())
    .addConstraint(RuleConstraint.ConstantsAtPosition(ConstantsPosition.LeastFunctionalVariable))

  "Amie" should "should mine with gradual partial pattern" in {
    Debugger() { implicit debugger =>
      val ruleset = index.withDebugger.mine(amie.addPattern(AtomPattern(predicate = TripleItem.Uri("dealsWith")) =>: AtomPattern(predicate = TripleItem.Uri("imports"))))
      ruleset.size shouldBe 1638
      ruleset.resolvedRules.forall(x => x.body.exists(_.predicate.hasSameUriAs("dealsWith")) && x.head.predicate.hasSameUriAs("imports")) shouldBe true
      ruleset.resolvedRules.view.map(_.ruleLength).toSet should contain only(2, 3)
    }
  }

  it should "should mine with gradual exact pattern" in {
    Debugger() { implicit debugger =>
      var ruleset = index.withDebugger.mine(amie.addPattern((AtomPattern(predicate = TripleItem.Uri("dealsWith")) =>: AtomPattern(predicate = TripleItem.Uri("imports"))).withExact()))
      ruleset.size shouldBe 52
      ruleset.resolvedRules.forall(x => x.body.length == 1 && x.body.exists(_.predicate.hasSameUriAs("dealsWith")) && x.head.predicate.hasSameUriAs("imports")) shouldBe true
      ruleset = index.withDebugger.mine(amie.addPattern((AtomPattern(predicate = TripleItem.Uri("hasCapital")) &: AtomPattern(predicate = TripleItem.Uri("dealsWith")) =>: AtomPattern(predicate = TripleItem.Uri("imports"))).withExact()))
      ruleset.size shouldBe 111
      ruleset = index.withDebugger.mine(amie.addPattern((AtomPattern(predicate = TripleItem.Uri("dealsWith")) &: AtomPattern(predicate = TripleItem.Uri("hasCapital")) =>: AtomPattern(predicate = TripleItem.Uri("imports"))).withExact()))
      ruleset.size shouldBe 0
    }
  }

  it should "should mine with orderless exact pattern" in {
    Debugger() { implicit debugger =>
      val ruleset = index.withDebugger.mine(amie.addPattern((AtomPattern(predicate = TripleItem.Uri("dealsWith")) &: AtomPattern(predicate = TripleItem.Uri("hasCapital")) =>: AtomPattern(predicate = TripleItem.Uri("imports"))).withExact().withOrderless()))
      ruleset.size shouldBe 111
      ruleset.resolvedRules.forall(x => x.body.length == 2 && x.body.forall(x => x.predicate.hasSameUriAs("dealsWith") || x.predicate.hasSameUriAs("hasCapital")) && x.head.predicate.hasSameUriAs("imports")) shouldBe true
    }
  }

  it should "should mine with orderless partial pattern" in {
    Debugger() { implicit debugger =>
      val ruleset = index.withDebugger.mine(amie.addPattern((AtomPattern(predicate = TripleItem.Uri("hasCapital")) =>: AtomPattern(predicate = TripleItem.Uri("imports"))).withOrderless()))
      ruleset.size shouldBe 129
      ruleset.resolvedRules.forall(x => x.body.exists(_.predicate.hasSameUriAs("hasCapital")) && x.head.predicate.hasSameUriAs("imports")) shouldBe true
    }
  }

}