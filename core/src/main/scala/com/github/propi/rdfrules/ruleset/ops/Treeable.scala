package com.github.propi.rdfrules.ruleset.ops

import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.rule.{Measure, RuleContent}
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.ruleset.ops.Treeable._
import com.github.propi.rdfrules.utils.TypedKeyMap.Key
import com.github.propi.rdfrules.utils.{Debugger, ForEach}

/**
  * Created by Vaclav Zeman on 27. 4. 2020.
  */
trait Treeable extends Sortable[FinalRule, Ruleset] {

  private class Tree() {
    private val items = collection.mutable.HashMap.empty[RuleContent, HmapTreeItem]

    private class HmapTreeItem(private val ruleContent: RuleContent) extends TreeItem {
      private var _rule = Option.empty[FinalRule]
      private val _children = collection.mutable.HashSet.empty[RuleContent]

      def isDefined: Boolean = _rule.isDefined

      def setRule(rule: FinalRule): Unit = _rule = Some(rule)

      def addChild(ruleContent: RuleContent): Unit = _children.addOne(ruleContent)

      def rule: FinalRule = _rule.get

      private lazy val nonDistinctParents: Seq[HmapTreeItem] = ruleContent.parents.flatMap(items.get).flatMap { parent =>
        if (parent.isDefined) Iterator(parent) else parent.nonDistinctParents
      }.toSeq

      private lazy val nonDistinctChildren: Seq[HmapTreeItem] = _children.iterator.flatMap(items.get).flatMap { child =>
        if (child.isDefined) Iterator(child) else child.nonDistinctChildren
      }.toSeq

      def parents: ForEach[HmapTreeItem] = (f: HmapTreeItem => Unit) => {
        val containedRules = collection.mutable.HashMap.empty[RuleContent, HmapTreeItem]
        nonDistinctParents.foreach(x => containedRules.put(x.ruleContent, x))
        containedRules.filterInPlace((_, treeItem) => treeItem.nonDistinctDescendants.forall(x => !containedRules.contains(x.ruleContent)))
        containedRules.valuesIterator.foreach(f)
      }

      def children: ForEach[HmapTreeItem] = (f: HmapTreeItem => Unit) => {
        val containedRules = collection.mutable.HashMap.empty[RuleContent, HmapTreeItem]
        nonDistinctChildren.foreach(x => containedRules.put(x.ruleContent, x))
        containedRules.filterInPlace((_, treeItem) => treeItem.nonDistinctAncestors.forall(x => !containedRules.contains(x.ruleContent)))
        containedRules.valuesIterator.foreach(f)
      }

      private lazy val nonDistinctAncestors: Seq[HmapTreeItem] = ruleContent.parents.flatMap(items.get).flatMap(parent => Iterator(parent) ++ parent.nonDistinctAncestors.iterator).filter(_.isDefined).toSeq

      private lazy val nonDistinctDescendants: Seq[HmapTreeItem] = _children.iterator.flatMap(items.get).flatMap(child => Iterator(child) ++ child.nonDistinctDescendants.iterator).filter(_.isDefined).toSeq

      def ancestors: ForEach[TreeItem] = ForEach.from(nonDistinctAncestors).distinctBy(_.ruleContent)

      def descendants: ForEach[TreeItem] = ForEach.from(nonDistinctDescendants).distinctBy(_.ruleContent)

      def directParents: ForEach[HmapTreeItem] = ForEach.from(ruleContent.parents).flatMap(items.get).filter(_.isDefined)

      def directChildren: ForEach[HmapTreeItem] = ForEach.from(_children).flatMap(items.get).filter(_.isDefined)
    }

    private def addChildrenRecursively(ruleContent: RuleContent): Unit = {
      for (parent <- ruleContent.parents) {
        items.getOrElseUpdate(parent, new HmapTreeItem(parent)).addChild(ruleContent)
        addChildrenRecursively(parent)
      }
    }

    def addRule(rule: FinalRule): Unit = {
      val ruleContent = RuleContent(rule.body, rule.head)
      items.getOrElseUpdate(ruleContent, new HmapTreeItem(ruleContent)).setRule(rule)
      addChildrenRecursively(ruleContent)
    }

    def iterator: Iterator[TreeItem] = items.valuesIterator.filter(_.isDefined)
  }

  def tree(implicit debugger: Debugger): ForEach[TreeItem] = new ForEach[TreeItem] {
    def foreach(f: TreeItem => Unit): Unit = {
      val tree = new Tree()
      for (rule <- coll.withDebugger("Tree building", true)) {
        tree.addRule(rule)
      }
      tree.iterator.foreach(f)
    }

    override def knownSize: Int = coll.knownSize
  }.withDebugger("Tree traversing")

  /**
    * this avoids outputting rules that do not improve the measure w.r.t their parents.
    *
    * @param measure some measure by which to do this pruning strategy
    * @return
    */
  def onlyBetterDescendant(measure: Key[Measure])(implicit debugger: Debugger): Ruleset = {
    val ordering = implicitly[Ordering[Measure]]
    transform(tree.filter(x => x.parents.forall(y => ordering.gt(x.rule.measures(measure), y.rule.measures(measure)))).map(_.rule))
  }

  /**
    * Get most specific rules (only rules with no descendants)
    *
    * @return
    */
  def maximal(implicit debugger: Debugger): Ruleset = {
    transform(tree.filter(_.children.isEmpty).map(_.rule))
  }

  /**
    * Get closed rules. Rule is closed if its parent has different measure
    *
    * @param measure some measure by which to do this pruning strategy
    * @return
    */
  def closed(measure: Key[Measure])(implicit debugger: Debugger): Ruleset = {
    val ordering = implicitly[Ordering[Measure]]
    transform(tree.filter(x => x.directParents.forall(y => !ordering.equiv(x.rule.measures(measure), y.rule.measures(measure)))).map(_.rule))
  }

}

object Treeable {

  /**
    * Examples of rules:
    * 1: A & B & C => D,
    * 2: A => D,
    * 3: B & C => D
    * 4: B => D
    */
  trait TreeItem {
    def rule: FinalRule

    /**
      * For rule 1 ancestors are {2, 3, 4}
      *
      * @return
      */
    def ancestors: ForEach[TreeItem]

    /**
      * For rule 4 descendants are {3, 1}, for 2 they are {1}
      *
      * @return
      */
    def descendants: ForEach[TreeItem]

    /**
      * For rule 1 parents are {2, 3}
      *
      * @return
      */
    def parents: ForEach[TreeItem]

    /**
      * For rule 4 children are {3}, for 2 they are {1}
      *
      * @return
      */
    def children: ForEach[TreeItem]

    /**
      * For rule 1 direct parents are {3}
      *
      * @return
      */
    def directParents: ForEach[TreeItem]

    /**
      * For rule 4 children are {3}, for 2 they are {}
      *
      * @return
      */
    def directChildren: ForEach[TreeItem]
  }

  /*private def _dfs(nodes: ForEach[TreeItem]): ForEach[TreeItem] = nodes.flatMap { node =>
    node +: _dfs(node.children)
  }

  private implicit class PimpedTree(val tree: ForEach[TreeItem]) extends AnyVal {
    def dfs: ForEach[TreeItem] = _dfs(tree.filter(_.parents.isEmpty))
  }*/

}