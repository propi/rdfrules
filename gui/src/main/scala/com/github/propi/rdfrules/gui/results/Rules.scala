package com.github.propi.rdfrules.gui.results

import com.github.propi.rdfrules.gui.results.Rules._
import com.github.propi.rdfrules.gui.utils.StringConverters._
import com.github.propi.rdfrules.gui._
import com.thoughtworks.binding.Binding.{Constants, Var}
import com.thoughtworks.binding.Binding
import org.lrng.binding.html
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.{HTMLInputElement, HTMLSelectElement}
import org.scalajs.dom.{Event, window}

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.{JSON, Math}

/**
  * Created by Vaclav Zeman on 14. 9. 2018.
  */
class Rules(val title: String, val id: Future[String]) extends ActionProgress with Pagination[(Rule, Int)] {

  private val selectedCluster: Var[Option[Int]] = Var(None)
  private val fulltext: Var[String] = Var("")

  private var lastFulltext: String = ""
  private var lastCluster: Option[Int] = None
  private var lastResult: Option[IndexedSeq[js.Dynamic]] = None

  private def exportJson(rules: Seq[js.Dynamic]): Unit = {
    Downloader.download("rules.json", js.Array(rules: _*))
  }

  private def exportText(rules: Seq[js.Dynamic]): Unit = {
    val textRules = rules.view.map(_.asInstanceOf[Rule]).map { rule =>
      s"${rule.body.map(viewAtom).mkString(" ^ ")} => ${viewAtom(rule.head)} | ${rule.measures.view.map(x => s"${x.name}: ${x.value}").mkString(", ")}"
    }
    Downloader.download("rules.txt", textRules)
  }

  private def filterRulesByFulltext(rules: Iterable[js.Dynamic], text: String): Iterable[js.Dynamic] = if (text.isEmpty) {
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

  private def filteredRules(rules: Iterable[js.Dynamic], fulltext: String, cluster: Option[Int]): IndexedSeq[js.Dynamic] = {
    if (fulltext != lastFulltext || cluster != lastCluster) lastResult = None
    lastResult match {
      case Some(x) => x
      case None =>
        val _lastResult = filterRulesByFulltext(getRulesByCluster(rules, cluster), fulltext).toVector
        lastResult = Some(_lastResult)
        lastFulltext = fulltext
        lastCluster = cluster
        _lastResult
    }
  }

  private def getClusters(rules: Iterable[js.Dynamic]): Seq[(Int, Int)] = {
    rules.view
      .map(_.asInstanceOf[Rule])
      .flatMap(_.measures.find(_.name == "Cluster").map(_.value.toInt))
      .groupBy(x => x)
      .view
      .mapValues(_.size)
      .toList
      .sortBy(_._1)
  }

  private def getRulesByCluster(rules: Iterable[js.Dynamic], cluster: Option[Int]): Iterable[js.Dynamic] = cluster match {
    case Some(cluster) => rules.view.filter(_.asInstanceOf[Rule].measures.exists(x => x.name == "Cluster" && x.value == cluster))
    case None => rules
  }

  private def instantiate(event: Event, rule: Rule): Unit = {
    event.preventDefault()
    for (op <- Main.canvas.getOperations.reverseIterator.find(_.info.targetStructure == OperationStructure.Index)) {
      LocalStorage.put(Canvas.newWindowTaskKey, JSON.stringify(js.Array(op.toJson(Nil): _*)))
      LocalStorage.put(Canvas.loadRulesKey, JSON.stringify(js.Array(rule)))
      LocalStorage.put(Canvas.instantiationKey, JSON.stringify(rule))
      AutoCaching.saveCache()
      window.open(s"./?${Canvas.newWindowTaskKey}=1&${Canvas.loadRulesKey}=1&${Canvas.instantiationKey}=1")
    }
  }

  private def predict(event: Event, rule: Rule): Unit = {
    event.preventDefault()
    for (op <- Main.canvas.getOperations.reverseIterator.find(_.info.targetStructure == OperationStructure.Index)) {
      LocalStorage.put(Canvas.newWindowTaskKey, JSON.stringify(js.Array(op.toJson(Nil): _*)))
      LocalStorage.put(Canvas.loadRulesKey, JSON.stringify(js.Array(rule)))
      LocalStorage.put(Canvas.predictionKey, JSON.stringify(rule))
      AutoCaching.saveCache()
      window.open(s"./?${Canvas.newWindowTaskKey}=1&${Canvas.loadRulesKey}=1&${Canvas.predictionKey}=1")
    }
  }

  @html
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
      {record._1.measures.mapApproximativeMeasures.map(x => s"${x.name}: ${x.value}").mkString(", ")}
    </div>
    <div class="rule-tools">
      <a href="#" onclick={e: Event => instantiate(e, record._1)}>Instantiate</a>
      <a href="#" onclick={e: Event => predict(e, record._1)}>Predict</a>
    </div>
  </div>

  @html
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
      if (pu.prefix.isEmpty && pu.nameSpace.nonEmpty) {
        s"<${pu.nameSpace}${pu.localName}>"
      } else {
        s"${pu.prefix}:${pu.localName}"
      }
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

  implicit class PimpedMeasures(val measures: js.Array[Measure]) extends AnyVal {
    def isApproximative: Boolean = measures.exists(_.name == "SupportIncreaseRatio")

    def mapApproximativeMeasures: Iterator[Measure] = if (isApproximative) {
      val mappedMeasures = measures.iterator.filter(_.name != "SupportIncreaseRatio").map { measure =>
        if (measure.name == "Support" || measure.name == "HeadCoverage") {
          new Measure {
            val name: String = s"~${measure.name}"
            val value: Double = measure.value
          }
        } else {
          measure
        }
      }
      val samples = Some(measures.foldLeft(0.0 -> 0.0) { case (r@(hs, sir), measure) =>
        measure.name match {
          case "SupportIncreaseRatio" => hs -> measure.value
          case "HeadSupport" => measure.value -> sir
          case _ => r
        }
      }).filter(x => x._1 > 0.0 && x._2 > 0.0).map(x => new Measure {
        val name: String = "Samples"
        val value: Double = Math.round(x._1 / x._2)
      })
      mappedMeasures ++ samples
    } else {
      measures.iterator
    }
  }

  implicit class PimpedAtomItem(val atomItem: AtomItem) extends AnyVal {
    def isConstant: Boolean = atomItem.`type` == "constant"
  }

}