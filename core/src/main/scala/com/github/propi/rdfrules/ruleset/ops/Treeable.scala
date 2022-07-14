package com.github.propi.rdfrules.ruleset.ops

import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.rule.{Atom, ExpandingRule, Measure}
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.ruleset.ops.Treeable._
import com.github.propi.rdfrules.utils.ForEach
import com.github.propi.rdfrules.utils.TypedKeyMap.Key

/**
  * Created by Vaclav Zeman on 27. 4. 2020.
  */
trait Treeable extends Sortable[FinalRule, Ruleset] {

  private def tree: ForEach[TreeItem] = (f: TreeItem => Unit) => {
    val orphans = collection.mutable.Set.empty[TreeItem]
    for (rule <- sortBy(_.ruleLength)(implicitly[Ordering[Int]].reverse).rules) {
      val children = orphans
        .iterator
        .filter(orphan => rule.ruleLength < orphan.rule.ruleLength && ExpandingRule.checkParenthood(rule.body, orphan.ruleBody, rule.head, orphan.rule.head))
        .toList
      val treeItem = if (children.isEmpty) {
        Leaf(rule)
      } else {
        children.foreach(orphans -= _)
        Node(rule, ForEach.from(children))
      }
      orphans += treeItem
    }
    orphans.foreach(f)
  }

  /**
    * this avoids outputting rules that do not improve the measure w.r.t their parents.
    *
    * @param measure some measure by which to do this pruning strategy
    * @return
    */
  def onlyBetterDescendant(measure: Key[Measure]): Ruleset = {
    val ordering = implicitly[Ordering[Measure]]
    transform(tree.dfs.filter(x => x.ancestors.forall(y => ordering.gt(x.rule.measures(measure), y.rule.measures(measure)))).map(_.rule))
  }

  /**
    * Get most specific rules (only rules with no descendants)
    *
    * @return
    */
  def maximal: Ruleset = {
    transform(tree.dfs.collect {
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
    transform(tree.dfs.filter(x => x.parent.forall(y => !ordering.equiv(x.rule.measures(measure), y.rule.measures(measure)))).map(_.rule))
  }

}

object Treeable {

  private trait TreeItem {
    val rule: FinalRule
    lazy val ruleBody: Set[Atom] = rule.body.toSet
  }

  private case class Node(rule: FinalRule, children: ForEach[TreeItem]) extends TreeItem

  private case class Leaf(rule: FinalRule) extends TreeItem

  private case class TreeItemWithParent(treeItem: TreeItem, parent: Option[TreeItemWithParent]) {
    def rule: FinalRule = treeItem.rule

    def ancestors: Iterator[TreeItemWithParent] = parent match {
      case Some(x) => Iterator(x) ++ x.ancestors
      case None => Iterator.empty
    }
  }

  private def _dfs(children: ForEach[TreeItem], parent: Option[TreeItemWithParent]): ForEach[TreeItemWithParent] = children.flatMap {
    case treeItem@Node(_, children) =>
      val newParent = TreeItemWithParent(treeItem, parent)
      ForEach(newParent).concat(_dfs(children, Some(newParent)))
    case treeItem@Leaf(_) =>
      ForEach(TreeItemWithParent(treeItem, parent))
  }

  private implicit class PimpedTree(val tree: ForEach[TreeItem]) extends AnyVal {
    def dfs: ForEach[TreeItemWithParent] = _dfs(tree, None)
  }

}