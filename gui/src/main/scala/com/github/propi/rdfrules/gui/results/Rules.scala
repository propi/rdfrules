package com.github.propi.rdfrules.gui.results

import com.github.propi.rdfrules.gui.operations.Instantiate
import com.github.propi.rdfrules.gui.results.Rules._
import com.github.propi.rdfrules.gui.utils.StringConverters._
import com.github.propi.rdfrules.gui.{ActionProgress, Downloader, Globals, Main, OperationInfo}
import com.thoughtworks.binding.Binding.{Constants, Var}
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.Event
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.{HTMLInputElement, HTMLSelectElement}

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 14. 9. 2018.
  */
class Rules(val title: String, val id: Future[String]) extends ActionProgress with Pagination[(Rule, Int)] {

  private val selectedCluster: Var[Option[Int]] = Var(None)
  private val fulltext: Var[String] = Var("")

  private var lastFulltext: String = ""
  private var lastCluster: Option[Int] = None
  private var lastResult: Option[IndexedSeq[js.Dynamic]] = None

  /*@dom
  private def viewAtom(atom: Atom): Binding[Div] = <div class="atom">
    <span class="symbol">(</span>
    <span class={"subject " + atom.subject.`type`}>
      {atom.subject.value.toString}
    </span>
    <span class="predicate">
      {atom.predicate}
    </span>
    <span class={"object " + atom.`object`.`type`}>
      {atom.`object`.value.toString}
    </span>{atom.graphs.toOption match {
      case Some(graphs) if graphs.length > 0 =>
        if (graphs.length == 1) {
          <span class="graph">
            {graphs.head}
          </span>
        } else {
          <span class="graph">
            [
            {graphs.mkString(", ")}
            ]
          </span>
        }
      case None => <span class="graph"></span>
    }}<span class="symbol">)</span>
  </div>*/

  /*
        {val span = <span></span>.asInstanceOf[Span]
    span.innerHTML = record.body.map(viewAtom).mkString(" ^ ") + " &rArr; " + viewAtom(record.head)
    span}
   */

  private def exportJson(rules: Seq[js.Dynamic]): Unit = {
    Downloader.download("rules.json", js.Array(rules: _*))
  }

  private def exportText(rules: Seq[js.Dynamic]): Unit = {
    val textRules = rules.view.map(_.asInstanceOf[Rule]).map { rule =>
      rule.body.map(viewAtom).mkString(" ^ ") + " => " + viewAtom(rule.head) + " | " + rule.measures.view.map(x => s"${x.name}: ${x.value}").mkString(", ")
    }
    Downloader.download("rules.txt", textRules)
  }

  private def filteredRules(rules: Seq[js.Dynamic], fulltext: String, cluster: Option[Int]): IndexedSeq[js.Dynamic] = {
    if (fulltext != lastFulltext || cluster != lastCluster) lastResult = None
    lastResult match {
      case Some(x) => x
      case None =>
        val _lastResult = getRulesByFulltext(getRulesByCluster(rules, cluster), fulltext).toVector
        lastResult = Some(_lastResult)
        lastFulltext = fulltext
        lastCluster = cluster
        _lastResult
    }
  }

  private def getClusters(rules: Seq[js.Dynamic]): Seq[(Int, Int)] = {
    rules.view
      .map(_.asInstanceOf[Rule])
      .flatMap(_.measures.find(_.name == "Cluster").map(_.value.toInt))
      .groupBy(x => x)
      .mapValues(_.size)
      .toList
      .sortBy(_._1)
  }

  private def getRulesByCluster(rules: Seq[js.Dynamic], cluster: Option[Int]): Seq[js.Dynamic] = cluster match {
    case Some(cluster) => rules.view.filter(_.asInstanceOf[Rule].measures.exists(x => x.name == "Cluster" && x.value == cluster))
    case None => rules
  }

  private def getRulesByFulltext(rules: Seq[js.Dynamic], text: String): Seq[js.Dynamic] = if (text.isEmpty) {
    rules
  } else {
    val normFulltext = Globals.stripText(text).toLowerCase
    rules.view.filter { x =>
      val rule = x.asInstanceOf[Rule]
      (rule.body.iterator ++ Iterator(rule.head))
        .flatMap(x => Iterator(viewAtomItem(x.subject), viewAtomItem(x.predicate), viewAtomItem(x.`object`)) ++ x.graphs.toOption.iterator.flatten.map(viewAtomItem))
        .map(Globals.stripText(_).toLowerCase)
        .exists(_.contains(normFulltext))
    }
  }

  @dom
  def viewRecord(record: (Rule, Int)): Binding[Div] = <div class="rule">
    <div class="text">
      <span>
        {(record._2 + 1).toString + ":"}
      </span>
      <span>
        {record._1.body.map(viewAtom).mkString(" ^ ")}
      </span>
      <span>
        &rArr;
      </span>
      <span>
        {viewAtom(record._1.head)}
      </span>
    </div>
    <div class="measures">
      {record._1.measures.map(x => s"${x.name}: ${x.value}").mkString(", ")}
    </div>
    <div class={"rule-tools" + (if (Main.canvas.getOperations.last.previousOperation.value.exists(_.info.isInstanceOf[OperationInfo.RulesetTransformation]) && !record._1.isInstantiated) "" else " hidden")}>
      <a href="#" onclick={e: Event =>
        e.preventDefault()
        Main.canvas.getOperations.last.delete()
        val op0 = Main.canvas.getOperations.last
        val op1 = op0.appendOperation(OperationInfo.RulesetTransformation.Instantiate)
        op1.asInstanceOf[Instantiate].setRule(record._1)
        Main.canvas.addOperation(op1)
        Main.canvas.addOperation(op1.appendOperation(OperationInfo.GetRules))
        op1.openModal()}>Instantiate</a>
    </div>
  </div>

  @dom
  def viewResult(result: Constants[js.Dynamic]): Binding[Div] = <div class="rules">
    <div class="rules-amount">
      <span class="text">Number of rules:</span>
      <span class="number">
        {filteredRules(result.value, fulltext.bind, selectedCluster.bind).size.toString}
      </span>
    </div>
    <div class="rules-tools">
      <a href="#" onclick={e: Event =>
        e.preventDefault()
        exportJson(filteredRules(result.value, fulltext.value, selectedCluster.value))}>Export as JSON</a>
      <a href="#" onclick={e: Event =>
        e.preventDefault()
        exportText(filteredRules(result.value, fulltext.value, selectedCluster.value))}>Export as TEXT</a>{val clusters = getClusters(result.value)
    <select class={"clusters" + (if (clusters.isEmpty) " hidden" else "")} onchange={e: Event =>
      selectedCluster.value = stringToTryInt(e.target.asInstanceOf[HTMLSelectElement].value).toOption
      setPage(1)}>
      <option value="">All clusters</option>{for (cluster <- Constants(clusters: _*)) yield <option value={"" + cluster._1} selected={selectedCluster.bind.contains(cluster._1)}>
      {"Cluster " + cluster._1 + " (" + cluster._2 + ")"}
    </option>}
    </select>}<input type="text" class="fulltext" value={fulltext.bind} onkeyup={e: Event =>
      fulltext.value = e.target.asInstanceOf[HTMLInputElement].value
      setPage(1)}></input>
    </div>
    <div class="rules-body">
      {viewRecords(filteredRules(result.value, fulltext.bind, selectedCluster.bind).view.map(_.asInstanceOf[Rule]).zipWithIndex).bind}
    </div>
    <div class="rules-pages">
      {viewPages(filteredRules(result.value, fulltext.bind, selectedCluster.bind).size).bind}
    </div>
  </div>

}

object Rules {

  def viewAtom(atom: Atom): String = s"( ${viewAtomItem(atom.subject)} ${viewAtomItem(atom.predicate)} ${viewAtomItem(atom.`object`)}${atom.graphs.toOption.map(x => if (x.length == 1) viewAtomItem(x.head) else x.map(viewAtomItem).mkString(", ")).map(" " + _).getOrElse("")} )"

  def viewAtomItem(atomItem: AtomItem): String = viewAtomItem(atomItem.value)

  def viewAtomItem(atomItem: js.Dynamic): String = {
    if (js.typeOf(atomItem) == "object" && !js.isUndefined(atomItem.prefix)) {
      val pu = atomItem.asInstanceOf[PrefixedUri]
      s"${pu.prefix}:${pu.localName}"
    } else {
      atomItem.toString
    }
  }

  trait AtomItem extends js.Object {
    val `type`: String
    val value: js.Dynamic
  }

  trait PrefixedUri extends js.Object {
    val prefix: String
    val nameSpace: String
    val localName: String
  }

  trait Atom extends js.Object {
    val subject: AtomItem
    val predicate: js.Dynamic
    val `object`: AtomItem
    val graphs: js.UndefOr[js.Array[js.Dynamic]]
  }

  trait Measure extends js.Object {
    val name: String
    val value: Double
  }

  trait Rule extends js.Object {
    val head: Atom
    val body: js.Array[Atom]
    val measures: js.Array[Measure]
  }

  implicit class PimpedAtomItem(val atomItem: AtomItem) extends AnyVal {
    def isConstant: Boolean = atomItem.`type` == "constant"
  }

  implicit class PimpedRule(val rule: Rule) extends AnyVal {
    def isInstantiated: Boolean = (rule.head :: rule.body.toList).forall(x => x.subject.isConstant && x.`object`.isConstant)
  }

}