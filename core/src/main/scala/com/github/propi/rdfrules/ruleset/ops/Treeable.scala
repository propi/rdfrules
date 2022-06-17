package com.github.propi.rdfrules.ruleset.ops

import com.github.propi.rdfrules.rule.{Atom, ExtendedRule, Measure, Rule}
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.ruleset.ops.Treeable._
import com.github.propi.rdfrules.utils.TypedKeyMap.Key
import com.github.propi.rdfrules.utils.extensions.IterableOnceExtension._

/**
  * Created by Vaclav Zeman on 27. 4. 2020.
  */
trait Treeable extends Sortable[Rule.Simple, Ruleset] {

  private def tree: Traversable[TreeItem] = new Traversable[TreeItem] {
    def foreach[U](f: TreeItem => U): Unit = {
      val orphans = collection.mutable.Set.empty[TreeItem]
      for (rule <- sortBy(_.ruleLength)(implicitly[Ordering[Int]].reverse).rules) {
        val children = orphans
          .iterator
          .filter(orphan => rule.ruleLength < orphan.rule.ruleLength && ExtendedRule.checkParenthood(rule.body, orphan.ruleBody, rule.head, orphan.rule.head))
          .toList
        val treeItem = if (children.isEmpty) {
          Leaf(rule)
        } else {
          children.foreach(orphans -= _)
          Node(rule, children)
        }
        orphans += treeItem
      }
      orphans.foreach(f)
    }
  }

  /**
    * this avoids outputting rules that do not improve the measure w.r.t their parents.
    *
    * @param measure some measure by which to do this pruning strategy
    * @return
    */
  def onlyBetterDescendant(measure: Key[Measure]): Ruleset = {
    val ordering = implicitly[Ordering[Measure]]
    transform(tree.dfs.view.filter(x => x.ancestors.forall(y => ordering.gt(x.rule.measures(measure), y.rule.measures(measure)))).map(_.rule))
  }

  /**
    * Get most specific rules (only rules with no descendants)
    *
    * @return
    */
  def maximal: Ruleset = {
    transform(tree.dfs.view.collect {
      case TreeItemWithParent(Leaf(rule), _) => rule
    })
  }

  /**
    * Get closed rules. Rule is closed if its parent has different measure
    *
    * @param measure some measure by which to do this pruning strategy
    * @return
    */
  def closed(measure: Key[Measure]): Ruleset = {
    val ordering = implicitly[Ordering[Measure]]
    transform(tree.dfs.view.filter(x => x.parent.forall(y => !ordering.equiv(x.rule.measures(measure), y.rule.measures(measure)))).map(_.rule))
  }

}

object Treeable {

  private trait TreeItem {
    val rule: Rule.Simple
    lazy val ruleBody: Set[Atom] = rule.body.toSet
  }

  private case class Node(rule: Rule.Simple, children: Seq[TreeItem]) extends TreeItem

  private case class Leaf(rule: Rule.Simple) extends TreeItem

  private case class TreeItemWithParent(treeItem: TreeItem, parent: Option[TreeItemWithParent]) {
    def rule: Rule.Simple = treeItem.rule

    def ancestors: Iterator[TreeItemWithParent] = parent match {
      case Some(x) => Iterator(x) ++ x.ancestors
      case None => Iterator.empty
    }
  }

  private def _dfs(children: Traversable[TreeItem], parent: Option[TreeItemWithParent]): Traversable[TreeItemWithParent] = children.view.flatMap {
    case treeItem@Node(_, children) =>
      val newParent = TreeItemWithParent(treeItem, parent)
      List(newParent).concat(_dfs(children, Some(newParent)))
    case treeItem@Leaf(_) =>
      List(TreeItemWithParent(treeItem, parent))
  }

  private implicit class PimpedTree(val tree: Traversable[TreeItem]) extends AnyVal {
    def dfs: Traversable[TreeItemWithParent] = _dfs(tree, None)
  }

}