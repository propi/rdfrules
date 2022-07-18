package com.github.propi.rdfrules.gui.utils

import com.github.propi.rdfrules.gui.Globals

import scala.annotation.tailrec
import scala.scalajs.js

object Markdown {

  sealed trait Token

  sealed trait Content

  case class Text(text: String) extends Content {
    def :+(text: Text): Text = Text(s"${this.text}${text.text}")

    def map(f: String => String): Text = Text(f(text))

    override def toString: String = text
  }

  case class ListItem(title: String, content: Text, subList: Option[List])

  case class List(items: Seq[ListItem]) extends Content

  case class Heading(title: String, content: Seq[Content], subHeadings: Seq[Heading]) extends Token {
    def :+(heading: Heading): Heading = copy(subHeadings = subHeadings :+ heading)

    def :+(content: Content): Heading = copy(content = this.content :+ content)
  }

  private def parseListItem(token: js.Array[SimpleMarkdown.Token]): Option[ListItem] = {
    token.toList match {
      case head :: tail if head.`type` == "strong" && tail.nonEmpty =>
        val title = parseText(head.asInstanceOf[SimpleMarkdown.Strong].content).toString.trim
        val (contents, lists) = tail.span(_.`type` != "list")
        val content = parseText(js.Array(contents: _*)).map(_.stripPrefix(":").trim)
        val subList = lists.headOption.map(_.asInstanceOf[SimpleMarkdown.List]).map(parseContent)
        Some(ListItem(title, content, subList))
      case _ => None
    }
  }

  private def parseContent(token: SimpleMarkdown.List): List = List(token.items.iterator.flatMap(parseListItem).toSeq)

  private def parseContent(token: SimpleMarkdown.Token): Content = token.`type` match {
    case "list" => parseContent(token.asInstanceOf[SimpleMarkdown.List])
    case _ => parseText(token)
  }

  private def parseText(token: SimpleMarkdown.List): Text = {
    val tagName = if (token.ordered) "ul" else "ol"
    Text(s"<$tagName>${token.items.iterator.map(parseText).map(x => s"<li>$x</li>").mkString}</$tagName>")
  }

  private def parseText(token: SimpleMarkdown.Unknown): Text = token.content.map(x => (x: Any) match {
    case x: String => Text(x)
    case x: js.Array[_] => parseText(x.asInstanceOf[js.Array[SimpleMarkdown.Token]])
  }).getOrElse(Text(""))

  private def parseText(token: SimpleMarkdown.Text): Text = Text(Globals.escapeHTML(token.content))

  private def parseText(token: SimpleMarkdown.Em): Text = Text(s"<em>${parseText(token.content)}</em>")

  private def parseText(token: SimpleMarkdown.Paragraph): Text = Text(s"<p>${parseText(token.content)}</p>")

  private def parseText(token: SimpleMarkdown.Strong): Text = Text(s"<strong>${parseText(token.content)}</strong>")

  private def parseText(token: SimpleMarkdown.InlineCode): Text = Text(s"<code>${Globals.escapeHTML(token.content)}</code>")

  private def parseText(token: SimpleMarkdown.Link): Text = Text(s"""<a href="${token.target}" target="_blank">${parseText(token.content)}</a>""")

  private def parseText(token: SimpleMarkdown.Token): Text = token.`type` match {
    case "text" => parseText(token.asInstanceOf[SimpleMarkdown.Text])
    case "list" => parseText(token.asInstanceOf[SimpleMarkdown.List])
    case "link" => parseText(token.asInstanceOf[SimpleMarkdown.Link])
    case "strong" => parseText(token.asInstanceOf[SimpleMarkdown.Strong])
    case "em" => parseText(token.asInstanceOf[SimpleMarkdown.Em])
    case "paragraph" => parseText(token.asInstanceOf[SimpleMarkdown.Paragraph])
    case "inlineCode" => parseText(token.asInstanceOf[SimpleMarkdown.InlineCode])
    case _ => parseText(token.asInstanceOf[SimpleMarkdown.Unknown])
  }

  private def parseText(tokens: js.Array[SimpleMarkdown.Token]): Text = {
    tokens.iterator.map(parseText).reduceLeftOption(_ :+ _).getOrElse(Text(""))
  }

  private def parseHeading(heading: SimpleMarkdown.Heading): Heading = Heading(parseText(heading.content).toString, Nil, Nil)

  @tailrec
  private def parseHeadings(headings: collection.immutable.List[Heading], tokens: Iterator[SimpleMarkdown.Token], level: Int): Option[Heading] = {
    if (tokens.hasNext) {
      val token = tokens.next()
      if (token.`type` == "heading") {
        val heading = token.asInstanceOf[SimpleMarkdown.Heading]
        val parsedHeading = parseHeading(heading)
        if (heading.level == level + 1) {
          parseHeadings(parsedHeading :: headings, tokens, heading.level)
        } else if (heading.level <= level) {
          val stepsBack = level - heading.level + 2
          val (merging, remaining) = headings.splitAt(stepsBack)
          val parent = merging.reduceLeftOption((child, parent) => parent :+ child)
          parseHeadings(parsedHeading :: parent.map(_ :: remaining).getOrElse(remaining), tokens, heading.level)
        } else {
          parseHeadings(headings, tokens, level)
        }
      } else {
        headings match {
          case head :: tail => parseHeadings((head :+ parseContent(token)) :: tail, tokens, level)
          case _ => parseHeadings(headings, tokens, level)
        }
      }
    } else {
      headings.reduceLeftOption((child, parent) => parent :+ child)
    }
  }

  def apply(text: String): Option[Heading] = {
    val tree = SimpleMarkdown.defaultBlockParse(text).asInstanceOf[js.Array[SimpleMarkdown.Token]]
    parseHeadings(Nil, tree.iterator, 0)
  }

}
