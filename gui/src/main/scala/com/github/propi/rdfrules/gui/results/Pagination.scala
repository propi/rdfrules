package com.github.propi.rdfrules.gui.results

import com.thoughtworks.binding.Binding.{Constants, Var}
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.Event
import org.scalajs.dom.html.Div

/**
  * Created by Vaclav Zeman on 18. 9. 2018.
  */
trait Pagination[T] {

  private val page: Var[Int] = Var(1)
  protected val maxPerPage: Int = 50

  def viewRecord(record: T): Binding[Div]

  def setPage(x: Int): Unit = page.value = x

  @dom
  def viewRecords(records: Seq[T]): Binding[Div] = {
    val p = page.bind
    <div class="records">
      {for (record <- Constants(records.slice((p - 1) * maxPerPage, p * maxPerPage): _*)) yield viewRecord(record).bind}
    </div>
  }

  @dom
  def viewPages(total: Int): Binding[Div] = {
    val p = page.bind
    val pages = math.ceil(total.toDouble / maxPerPage).toInt
    <div class="pages">
      {for (i <- Constants(0 until pages: _*)) yield
      <a class={"page" + (if (i + 1 == p) " active" else "")} onclick={_: Event => if (p != i + 1) page.value = i + 1}>
        {(i + 1).toString}
      </a>}
    </div>
  }

}