package com.github.propi.rdfrules.gui

import com.github.propi.rdfrules.gui.utils.Markdown
import com.thoughtworks.binding.Binding.Var
import org.scalajs.dom.experimental.Fetch

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object Documentation {

  sealed trait Context {
    def title: String

    def description: String

    def apply(x: String): Context
  }

  private object Empty extends Context {
    val title: String = "Unknown name"
    val description: String = "Missing description in the doc."

    def apply(x: String): Context = Empty
  }

  private class Property(item: Markdown.ListItem) extends Context {
    val title: String = item.title
    val description: String = item.content.toString
    val properties: Map[String, Property] = item.subList.iterator.flatMap(_.items).map(x => x.title -> new Property(x)).toMap

    def apply(x: String): Context = properties.getOrElse(x, Empty)
  }

  private class Task(h4: Markdown.Heading) extends Context {
    val title: String = h4.title
    val description: String = h4.content.mkString("<br /><br />")
    val properties: Map[String, Property] = h4.subHeadings.find(_.title == "Properties").iterator.flatMap(_.content.collectFirst {
      case x: Markdown.List => x
    }).flatMap(_.items).map(x => x.title -> new Property(x)).toMap

    def apply(x: String): Context = properties.getOrElse(x, Empty)
  }

  private abstract class InnerContext[T <: Context](h: Markdown.Heading) extends Context {
    final val title: String = h.title
    final val description: String = h.content.mkString("<br /><br />")
    private val subs: Map[String, T] = h.subHeadings.iterator.map(x => x.title -> buildChild(x)).toMap

    protected def buildChild(x: Markdown.Heading): T

    final def apply(x: String): Context = subs.getOrElse(x, Empty)
  }

  private class TaskType(h3: Markdown.Heading) extends InnerContext[Task](h3) {
    protected def buildChild(x: Markdown.Heading): Task = new Task(x)
  }

  private class Struct(h2: Markdown.Heading) extends InnerContext[TaskType](h2) {
    protected def buildChild(x: Markdown.Heading): TaskType = new TaskType(x)
  }

  private class Root(h1: Markdown.Heading) extends InnerContext[Struct](h1) {
    protected def buildChild(x: Markdown.Heading): Struct = new Struct(x)
  }

  val doc: Var[Option[Context]] = Var(None)

  Fetch.fetch("README.md").toFuture.flatMap { response =>
    if (response.ok) {
      response.text().toFuture
    } else {
      Future.successful("")
    }
  }.foreach { text =>
    doc.value = Markdown(text).map(new Root(_))
  }

}
