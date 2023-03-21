package com.github.propi.rdfrules.gui.results

import com.github.propi.rdfrules.gui._
import com.github.propi.rdfrules.gui.results.PredictedTriples.{PredictedTriple, viewPredictedResultMark, viewPredictedTripleScore, viewTriple}
import com.github.propi.rdfrules.gui.results.PredictionTasks.PredictionTaskResult
import com.thoughtworks.binding.Binding
import com.thoughtworks.binding.Binding.{Constants, Var}
import org.lrng.binding.html
import org.scalajs.dom.Event
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.HTMLInputElement

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 14. 9. 2018.
  */
class PredictionTasks(val title: String, val id: Future[String]) extends ActionProgress with Pagination[(PredictionTaskResult, Int)] {

  private val fulltext: Var[String] = Var("")
  private var lastFulltext: String = ""
  private var lastResult: Option[IndexedSeq[js.Dynamic]] = None

  private def exportJson(triples: Seq[js.Dynamic]): Unit = {
    Downloader.download("predictionTasks.json", js.Array(triples: _*))
  }

  private def filterPredictionTasksByFulltext(tasks: Iterable[js.Dynamic], text: String): Iterable[js.Dynamic] = if (text.isEmpty) {
    tasks
  } else {
    val normFulltext = Globals.stripText(text).toLowerCase
    tasks.view.filter { x =>
      val task = x.asInstanceOf[PredictionTaskResult]
      Iterator(Rules.viewAtomItem(task.predictionTask.c.s.getOrElse("?".asInstanceOf[js.Dynamic])), Rules.viewAtomItem(task.predictionTask.p), Rules.viewAtomItem(task.predictionTask.c.o.getOrElse("?".asInstanceOf[js.Dynamic])))
        .map(Globals.stripText(_).toLowerCase)
        .mkString(" ")
        .contains(normFulltext)
    }
  }

  private def filteredTasks(tasks: Iterable[js.Dynamic], fulltext: String): IndexedSeq[js.Dynamic] = {
    if (fulltext != lastFulltext) lastResult = None
    lastResult match {
      case Some(x) => x
      case None =>
        val _lastResult = filterPredictionTasksByFulltext(tasks, fulltext).toVector
        lastResult = Some(_lastResult)
        lastFulltext = fulltext
        _lastResult
    }
  }

  @html
  def viewRecord(record: (PredictionTaskResult, Int)): Binding[Div] = <div class="predicted-triple">
    <div class="record">
      <div class="num">
        {(record._2 + 1).toString + ":"}
      </div>{viewTriple(record._1.predictionTask.c.s.getOrElse("?".asInstanceOf[js.Dynamic]), record._1.predictionTask.p, record._1.predictionTask.c.o.getOrElse("?".asInstanceOf[js.Dynamic])).bind}
    </div>{val n = Var(3)
    <ol class="candidates">
      {for (candidate <- Constants(record._1.candidates.iterator.take(n.bind).toSeq: _*)) yield {
      <li class="candidate">
        {viewPredictedResultMark(candidate.predictedResult).bind}{viewTriple(candidate.triple.subject, candidate.triple.predicate, candidate.triple.`object`).bind}{viewPredictedTripleScore(candidate.score).bind}
      </li>
    }}{if (record._1.candidates.length > n.bind) {
      <li class="more">
        <a href="#" onclick={e: Event =>
          e.preventDefault()
          n.value = n.value + 5}>show more</a>
      </li>
    } else {
      <!-- empty content -->
    }}
    </ol>}
  </div>

  @html
  def viewResult(result: Constants[js.Dynamic]): Binding[Div] = <div class="prediction-tasks-result">
    <div class="rules-amount">
      <span class="text">Number of prediction tasks:</span>
      <span class="number">
        {filteredTasks(result.value, fulltext.bind).size.toString}
      </span>
    </div>
    <div class="rules-tools">
      <a href="#" onclick={e: Event =>
        e.preventDefault()
        exportJson(filteredTasks(result.value, fulltext.value))}>Export as JSON</a>
      <input type="text" class="fulltext" value={fulltext.bind} onkeyup={e: Event =>
        fulltext.value = e.target.asInstanceOf[HTMLInputElement].value
        setPage(1)}></input>
    </div>
    <div class="rules-body">
      {viewRecords(filteredTasks(result.value, fulltext.bind).view.map(_.asInstanceOf[PredictionTaskResult]).zipWithIndex).bind}
    </div>
    <div class="rules-pages">
      {viewPages(filteredTasks(result.value, fulltext.bind).size).bind}
    </div>
  </div>

}

object PredictionTasks {

  trait TripleItemPosition extends js.Object {
    val s: js.UndefOr[js.Dynamic]
    val o: js.UndefOr[js.Dynamic]
  }

  trait PredictionTask extends js.Object {
    val p: js.Dynamic
    val c: TripleItemPosition
  }

  trait PredictionTaskResult extends js.Object {
    val predictionTask: PredictionTask
    val candidates: js.Array[PredictedTriple]
  }

}