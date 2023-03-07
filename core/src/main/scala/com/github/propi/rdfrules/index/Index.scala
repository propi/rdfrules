package com.github.propi.rdfrules.index

import com.github.propi.rdfrules.algorithm.consumer.InMemoryRuleConsumer
import com.github.propi.rdfrules.algorithm.{RuleConsumer, RulesMining}
import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.index.Index.PartType
import com.github.propi.rdfrules.index.ops.{CachedPartialIndex, CachedWholeIndex, SingleIndex}
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.Debugger

import java.io._

trait Index {
  def main: IndexPart

  def part(partType: PartType): Option[IndexPart]

  def parts: Iterator[(PartType, IndexPart)]

  def cache(os: () => OutputStream): Index

  def tripleItemMap: TripleItemIndex

  def withDebugger(implicit debugger: Debugger): Index

  final def cache(file: File): Index = cache(() => new FileOutputStream(file))

  final def cache(path: String): Index = cache(new File(path))

  final def mineRules(miner: RulesMining, ruleConsumer: RuleConsumer.Invoker[Ruleset] = RuleConsumer(InMemoryRuleConsumer())): Ruleset = {
    implicit val thi: TripleIndex[Int] = main.tripleMap
    implicit val mapper: TripleItemIndex = tripleItemMap
    ruleConsumer.invoke { ruleConsumer =>
      val result = miner.mine(ruleConsumer)
      Ruleset(this, result)
    }
  }
}

object Index {

  sealed trait PartType

  object PartType {
    case object Train extends PartType

    case object Test extends PartType
  }

  def apply(indexPart: IndexPart): Index = new SingleIndex(indexPart)

  def apply(dataset: Dataset, partially: Boolean)(implicit debugger: Debugger): Index = new SingleIndex(IndexPart(dataset, partially))

  def fromCache(is: () => InputStream, partially: Boolean)(implicit debugger: Debugger): Index = {
    if (partially) {
      new CachedPartialIndex(None, None, is)
    } else {
      new CachedWholeIndex(None, is)
    }
  }

  def fromCache(file: File, partially: Boolean)(implicit debugger: Debugger): Index = fromCache(() => new FileInputStream(file), partially)

  def fromCache(file: String, partially: Boolean)(implicit debugger: Debugger): Index = fromCache(new File(file), partially)

}