package com.github.propi.rdfrules.gui.results

import com.github.propi.rdfrules.gui._
import com.github.propi.rdfrules.gui.components.Multiselect
import com.github.propi.rdfrules.gui.results.InstantiatedRules.{InstantiatedRule, viewInstantiatedAtom}
import com.github.propi.rdfrules.gui.results.Rules._
import com.thoughtworks.binding.Binding
import com.thoughtworks.binding.Binding.{Constants, Var}
import org.lrng.binding.html
import org.scalajs.dom.Event
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.HTMLInputElement
import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 14. 9. 2018.
  */
class InstantiatedRules(val title: String, val id: Future[String]) extends ActionProgress with Pagination[(InstantiatedRule, Int)] {

  private val fulltext: Var[String] = Var("")
  private var lastFulltext: String = ""
  private val predictedResultsSelection = new Multiselect(Constants(
    PredictedResult.Positive.toString -> PredictedResult.Positive.label,
    PredictedResult.Negative.toString -> PredictedResult.Negative.label,
    PredictedResult.PcaPositive.toString -> PredictedResult.PcaPositive.label
  ), placeholder = "Predicted head")

  private val predictedResults: Binding[Set[PredictedResult]] = predictedResultsSelection.selectedValuesBinding.all.map(_.iterator.flatMap(PredictedResult(_)).toSet)
  private var lastPredictedResults: Set[PredictedResult] = Set.empty
  private var lastResult: Option[IndexedSeq[js.Dynamic]] = None

  private def exportJson(rules: Seq[js.Dynamic]): Unit = {
    Downloader.download("instantiatedRules.json", js.Array(rules: _*))
  }

  private def exportText(rules: Seq[js.Dynamic]): Unit = {
    val textRules = rules.view.map(_.asInstanceOf[InstantiatedRule]).map { rule =>
      s"${rule.body.map(viewInstantiatedAtom).mkString(" ^ ")} => ${viewInstantiatedAtom(rule.head)}"
    }
    Downloader.download("instantiatedRules.txt", textRules)
  }

  private def filterRulesByPredictedResults(rules: Iterable[js.Dynamic], predictedResults: Set[PredictedResult]): Iterable[js.Dynamic] = {
    if (predictedResults.isEmpty) {
      rules
    } else {
      rules.view.filter(x => PredictedResult(x.asInstanceOf[InstantiatedRule].predictedResult).forall(predictedResults))
    }
  }

  private def filterRulesByFulltext(rules: Iterable[js.Dynamic], text: String): Iterable[js.Dynamic] = if (text.isEmpty) {
    rules
  } else {
    val normFulltext = Globals.stripText(text).toLowerCase
    rules.view.filter { x =>
      val rule = x.asInstanceOf[InstantiatedRule]
      (rule.body.iterator ++ Iterator(rule.head))
        .flatMap(x => Iterator(viewAtomItem(x.subject), viewAtomItem(x.predicate), viewAtomItem(x.`object`)))
        .map(Globals.stripText(_).toLowerCase)
        .exists(_.contains(normFulltext))
    }
  }

  private def filteredRules(rules: Iterable[js.Dynamic], fulltext: String, predictedResults: Set[PredictedResult]): IndexedSeq[js.Dynamic] = {
    if (fulltext != lastFulltext || predictedResults != lastPredictedResults) lastResult = None
    lastResult match {
      case Some(x) => x
      case None =>
        val _lastResult = filterRulesByFulltext(filterRulesByPredictedResults(rules, predictedResults), fulltext).toVector
        lastResult = Some(_lastResult)
        lastFulltext = fulltext
        lastPredictedResults = predictedResults
        _lastResult
    }
  }

  @html
  def viewRecord(record: (InstantiatedRule, Int)): Binding[Div] = <div class="rule">
    <div class="text">
      <span>
        {s"${(record._2 + 1).toString}:"}
      </span>
      <span class={s"predicted-result ${record._1.predictedResult.toLowerCase}"} title={PredictedResult(record._1.predictedResult).map(_.label).getOrElse("")}>
        {PredictedResult(record._1.predictedResult).map(_.symbol).getOrElse("!")}
      </span>
      <span>
        {record._1.body.map(viewInstantiatedAtom).mkString(" ^ ")}
      </span>
      <span>
        &rArr;
      </span>
      <span>
        {viewInstantiatedAtom(record._1.head)}
      </span>
    </div>
  </div>

  @html
  def viewResult(result: Constants[js.Dynamic]): Binding[Div] = <div class="rules">
    <div class="rules-amount">
      <span class="text">Number of instantiated rules:</span>
      <span class="number">
        {filteredRules(result.value, fulltext.bind, predictedResults.bind).size.toString}
      </span>
    </div>
    <div class="rules-tools">
      <a href="#" onclick={e: Event =>
        e.preventDefault()
        exportJson(filteredRules(result.value, fulltext.value, predictedResultsSelection.selectedValues.flatMap(PredictedResult(_))))}>Export as JSON</a>
      <a href="#" onclick={e: Event =>
        e.preventDefault()
        exportText(filteredRules(result.value, fulltext.value, predictedResultsSelection.selectedValues.flatMap(PredictedResult(_))))}>Export as TEXT</a>
      <input type="text" class="fulltext" value={fulltext.bind} onkeyup={e: Event =>
        fulltext.value = e.target.asInstanceOf[HTMLInputElement].value
        setPage(1)}></input>{predictedResultsSelection.view.bind}
    </div>
    <div class="rules-body">
      {viewRecords(filteredRules(result.value, fulltext.bind, predictedResults.bind).view.map(_.asInstanceOf[InstantiatedRule]).zipWithIndex).bind}
    </div>
    <div class="rules-pages">
      {viewPages(filteredRules(result.value, fulltext.bind, predictedResults.bind).size).bind}
    </div>
  </div>

}

object InstantiatedRules {

  trait InstantiatedAtom extends js.Object {
    val subject: js.Dynamic
    val predicate: js.Dynamic
    val `object`: js.Dynamic
  }

  trait InstantiatedRule extends js.Object {
    val head: InstantiatedAtom
    val body: js.Array[InstantiatedAtom]
    val predictedResult: String
  }

  def viewInstantiatedAtom(atom: InstantiatedAtom): String = s"( ${viewAtomItem(atom.subject)} ${viewAtomItem(atom.predicate)} ${viewAtomItem(atom.`object`)} )"

}