package com.github.propi.rdfrules.gui.components

import com.thoughtworks.binding.Binding
import com.thoughtworks.binding.Binding.{BindingSeq, Constants, Var, Vars}
import org.lrng.binding.html
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.{Event, HTMLInputElement}
import org.scalajs.dom.window

import scala.scalajs.js

class Multiselect(options: Constants[(String, String)], defaults: Set[String] = Set.empty, placeholder: String = "") {

  private val selected: Vars[String] = Vars(defaults.toSeq: _*)
  private val optionsShowed: Var[Boolean] = Var(false)

  private def change(value: String, event: Event): Unit = {
    event.stopPropagation()
    val checked = event.target.asInstanceOf[HTMLInputElement].checked
    if (checked) {
      selected.value += value
    } else {
      selected.value -= value
    }
  }

  private def hide[T <: Event]: js.Function1[T, Unit] = (_: T) => {
    optionsShowed.value = false
    window.removeEventListener("click", hide);
  }

  private def show(e: Event): Unit = {
    if (!optionsShowed.value) {
      e.stopPropagation()
      optionsShowed.value = true
      window.addEventListener("click", hide)
    }
  }

  def selectedValuesBinding: BindingSeq[String] = selected

  def selectedValues: Set[String] = selected.value.toSet

  private def placeholderText: String = if (placeholder.isEmpty) "" else s"$placeholder: "

  @html
  def view: Binding[Div] = <div class="multiselect">
    <div class="selectBox" onclick={e: Event => show(e)}>
      <select onmousedown={e: Event => e.preventDefault()}>
        <option>
          {selected.length.bind match {
          case 0 => s"${placeholderText}no selected items"
          case 1 => s"${placeholderText}1 item selected"
          case x => s"$placeholderText$x items selected"
        }}
        </option>
      </select>
    </div>
    <div class={s"options${if (optionsShowed.bind) "" else " hidden"}"} onclick={e: Event => e.stopPropagation()}>
      {for ((value, title) <- options) yield
      <label>
        <input type="checkbox" checked={defaults(value)} onchange={event: Event => change(value, event)}/>{title}
      </label>}
    </div>
  </div>

}