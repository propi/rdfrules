import java.io.File

import com.github.propi.rdfrules.algorithm.RuleConsumer
import com.github.propi.rdfrules.algorithm.amie.Amie
import com.github.propi.rdfrules.algorithm.consumer.{InMemoryRuleConsumer, OnDiskRuleConsumer, TopKRuleConsumer}
import com.github.propi.rdfrules.data._
import com.github.propi.rdfrules.rule.RuleConstraint.ConstantsAtPosition.ConstantsPosition
import com.github.propi.rdfrules.rule._
import com.github.propi.rdfrules.ruleset.{Ruleset, RulesetSource}
import com.github.propi.rdfrules.utils.Debugger
import org.scalatest.{CancelAfterFailure, FlatSpec, Inside, Matchers}

import scala.io.Source

/**
  * Created by Vaclav Zeman on 18. 4. 2018.
  */
class ConsumersSpec extends FlatSpec with Matchers with Inside with CancelAfterFailure {

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

  "Amie" should "accept default consumer" in {
    Debugger() { implicit debugger =>
      val ruleset = index.withDebugger.mine(amie)
      ruleset.size shouldBe 7595
    }
  }

  it should "create pretty printed text output" in {
    Debugger() { implicit debugger =>
      val file = new File("temp/rules.txt")
      val nindex = index.withDebugger
      val ruleset = nindex.mine(amie.addThreshold(Threshold.Timeout(1)).addThreshold(Threshold.MaxRuleLength(5)), RuleConsumer.withMapper { implicit mapper =>
        InMemoryRuleConsumer(file, RulesetSource.Text)(_)
      })
      val source = Source.fromFile(file)
      try {
        source.getLines().size shouldBe ruleset.size
      } finally {
        source.close()
      }
    }
  }

  it should "create pretty printed json output" in {
    Debugger() { implicit debugger =>
      val file = new File("temp/rules.json")
      val nindex = index.withDebugger
      val ruleset = nindex.mine(amie, RuleConsumer.withMapper { implicit mapper => InMemoryRuleConsumer(file, RulesetSource.NDJson)(_) })
      Ruleset(nindex, file)(RulesetSource.NDJson).size shouldBe ruleset.size
    }
  }

  it should "accept on-disk consumer" in {
    Debugger() { implicit debugger =>
      val file = new File("temp/rules.cache")
      val filep = new File("temp/rules.json")
      val nindex = index.withDebugger
      val ruleset = nindex.mine(amie, RuleConsumer.withMapper { implicit mapper => OnDiskRuleConsumer(file, filep, RulesetSource.NDJson)(_) })
      Ruleset(nindex, filep)(RulesetSource.NDJson).size shouldBe ruleset.size
      Ruleset.fromCache(nindex, file).size shouldBe ruleset.size
    }
  }

  it should "accept top-k" in {
    Debugger() { implicit debugger =>
      val filep = new File("temp/rules.txt")
      val nindex = index.withDebugger
      var ruleset = nindex.mine(amie, RuleConsumer(TopKRuleConsumer(100)))
      ruleset.size shouldBe 100
      ruleset = nindex.mine(amie, RuleConsumer(TopKRuleConsumer(100, true)))
      ruleset.size shouldBe 139
      ruleset = nindex.mine(amie, RuleConsumer.withMapper { implicit mapper => TopKRuleConsumer(100, false, filep, RulesetSource.Text) })
      val source = Source.fromFile(filep)
      try {
        source.getLines().size should be > ruleset.size
      } finally {
        source.close()
      }
    }
  }

}