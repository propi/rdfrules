package com.github.propi.rdfrules.gui.results

import com.thoughtworks.binding.Binding.{Constants, Var}
import com.thoughtworks.binding.Binding
import org.lrng.binding.html
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

  @html
  def viewRecords(records: Iterable[T]): Binding[Div] = <div class="records">
    {val p = page.bind
    for (record <- Constants(records.slice((p - 1) * maxPerPage, p * maxPerPage).toList: _*)) yield viewRecord(record).bind}
  </div>

  @html
  def viewPages(total: Int): Binding[Div] = <div class="pages">
    {val p = page.bind
    val pages = math.ceil(total.toDouble / maxPerPage).toInt
    for (i <- Constants(0 until pages: _*)) yield
      <a class={s"page${if (i + 1 == p) " active" else ""}"} onclick={_: Event => if (p != i + 1) page.value = i + 1}>
        {(i + 1).toString}
      </a>}
  </div>

}