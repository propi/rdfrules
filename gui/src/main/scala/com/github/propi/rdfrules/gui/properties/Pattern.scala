package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Documentation.Context
import com.github.propi.rdfrules.gui.Property
import com.github.propi.rdfrules.gui.utils.Validate
import com.github.propi.rdfrules.gui.utils.Validate.ValidationException

import scala.annotation.tailrec
import scala.collection.mutable
import scala.scalajs.js
import scala.util.{Failure, Success, Try}

/**
  * Created by Vaclav Zeman on 17. 9. 2018.
  */
object Pattern {

  private sealed trait AtomItem {
    def toJson: js.Any
  }

  private object AtomItem {
    case object Any extends AtomItem {
      def toJson: js.Any = js.Dictionary("name" -> "Any")

      override def toString: String = "?"
    }

    case object AnyVariable extends AtomItem {
      def toJson: js.Any = js.Dictionary("name" -> "AnyVariable")

      override def toString: String = "?V"
    }

    case object AnyConstant extends AtomItem {
      def toJson: js.Any = js.Dictionary("name" -> "AnyConstant")

      override def toString: String = "?C"
    }

    case class Variable(c: Char) extends AtomItem {
      def toJson: js.Any = js.Dictionary(
        "name" -> "Variable",
        "value" -> c
      )

      override def toString: String = s"?$c"
    }

    case class Constant(x: String) extends AtomItem {
      def toJson: js.Any = js.Dictionary(
        "name" -> "Constant",
        "value" -> x
      )

      override def toString: String = x
    }

    case class OneOf(items: IndexedSeq[Constant]) extends AtomItem {
      def :+(x: Constant): OneOf = OneOf(items :+ x)

      def toNonOf: NoneOf = NoneOf(items)

      def toJson: js.Any = js.Dictionary(
        "name" -> "OneOf",
        "value" -> js.Array(items.map(_.toJson): _*)
      )

      override def toString: String = s"{${items.mkString(" | ")}}"
    }

    case class NoneOf(items: IndexedSeq[Constant]) extends AtomItem {
      def toJson: js.Any = js.Dictionary(
        "name" -> "NoneOf",
        "value" -> js.Array(items.map(_.toJson): _*)
      )

      override def toString: String = s"!{${items.mkString(" | ")}}"
    }

    def apply(data: js.Dynamic): AtomItem = data.name.toString match {
      case "AnyVariable" => AnyVariable
      case "AnyConstant" => AnyConstant
      case "Variable" => Variable(data.value.toString.head)
      case "Constant" => Constant(data.value.toString)
      case "OneOf" => OneOf(data.value.asInstanceOf[js.Array[js.Dynamic]].iterator.map(data => Constant(data.value.toString)).toIndexedSeq)
      case "NoneOf" => NoneOf(data.value.asInstanceOf[js.Array[js.Dynamic]].iterator.map(data => Constant(data.value.toString)).toIndexedSeq)
      case _ => Any
    }
  }

  private case class Atom(s: AtomItem, p: AtomItem, o: AtomItem, g: AtomItem) {
    def replace(x: AtomItem, i: Int): Atom = i match {
      case 0 => copy(s = x)
      case 1 => copy(p = x)
      case 2 => copy(o = x)
      case 3 => copy(g = x)
      case _ => this
    }

    def toJson: js.Any = js.Dictionary(
      "subject" -> s.toJson,
      "predicate" -> p.toJson,
      "object" -> o.toJson,
      "graph" -> g.toJson
    )

    override def toString: String = s"($s $p $o${if (g == AtomItem.Any) "" else s" $g"})"
  }

  private object Atom {
    def apply(data: js.Dynamic): Atom = Atom(
      AtomItem(data.subject),
      AtomItem(data.predicate),
      AtomItem(data.`object`),
      AtomItem(data.graph)
    )
  }

  private case class Rule(body: IndexedSeq[Atom], head: Atom, exact: Boolean) {
    def partial: Rule = copy(exact = false)

    def +>(x: Atom): Rule = copy(body = body :+ x)

    def >+(x: Atom): Rule = copy(head = x)

    def toJson: js.Any = js.Dictionary(
      "body" -> js.Array(body.map(_.toJson): _*),
      "head" -> head.toJson,
      "exact" -> exact,
      "orderless" -> true
    )

    override def toString: String = s"${(Iterator(if (exact) "" else "*") ++ body.iterator.map(_.toString)).mkString(" ^ ")} => ${head.toString}"
  }

  private object Rule {
    def apply(data: js.Dynamic): Rule = Rule(
      data.body.asInstanceOf[js.Array[js.Dynamic]].iterator.map(Atom(_)).toIndexedSeq,
      Atom(data.head),
      data.exact.asInstanceOf[Boolean]
    )
  }

  private def parseVariable(it: Iterator[Char]): Either[AtomItem, String] = if (it.hasNext) {
    it.next() match {
      case 'V' => Left(AtomItem.AnyVariable)
      case 'C' => Left(AtomItem.AnyConstant)
      case x if x.isSpaceChar => Left(AtomItem.Any)
      case x if x.isLetter && x.isLower => Left(AtomItem.Variable(x))
      case x => Right(s"Invalid character '$x' occured during variable parsing.")
    }
  } else {
    Right("Invalid rule atom.")
  }

  @tailrec
  private def parseText(it: Iterator[Char], buffer: mutable.StringBuilder): Either[AtomItem.Constant, String] = if (it.hasNext) {
    val x = it.next()
    x match {
      case '"' if buffer.last != '\\' => Left(AtomItem.Constant(buffer.addOne(x).toString()))
      case _ => parseText(it, buffer.addOne(x))
    }
  } else {
    Right("Invalid rule atom.")
  }

  @tailrec
  private def parseInterval(it: Iterator[Char], buffer: mutable.StringBuilder): Either[AtomItem.Constant, String] = if (it.hasNext) {
    val x = it.next()
    x match {
      case ']' | ')' =>
        val text = buffer.addOne(x).toString()
        if (text.matches("([\\[\\(])\\s*(.+?)\\s*;\\s*(.+?)\\s*([\\]\\)])")) {
          Left(AtomItem.Constant(text))
        } else {
          Right("Invalid interval syntax.")
        }
      case _ => parseInterval(it, buffer.addOne(x))
    }
  } else {
    Right("Invalid rule atom.")
  }

  @tailrec
  private def parseUri(it: Iterator[Char], buffer: mutable.StringBuilder): Either[AtomItem.Constant, String] = if (it.hasNext) {
    val x = it.next()
    x match {
      case '>' if buffer.last != '\\' => Left(AtomItem.Constant(buffer.addOne(x).toString()))
      case _ => parseUri(it, buffer.addOne(x))
    }
  } else {
    Right("Invalid rule atom.")
  }

  @tailrec
  private def parseOtherConstant(it: Iterator[Char], buffer: mutable.StringBuilder): Either[(AtomItem.Constant, Option[Char]), String] = if (it.hasNext) {
    val x = it.next()
    x match {
      case '}' | '|' | ')' | ' ' if buffer.last != '\\' =>
        val text = buffer.toString()
        if (text == "true" || text == "false" || text.matches("\\d+([.]\\d+)?") || text.matches("_:.+") || text.matches("\\w*:.*")) {
          Left(AtomItem.Constant(buffer.toString()) -> (if (x.isSpaceChar) None else Some(x)))
        } else {
          Right(s"Invalid constant '$text'.")
        }
      case _ => parseOtherConstant(it, buffer.addOne(x))
    }
  } else {
    Right("Invalid rule atom.")
  }

  @tailrec
  private def parseConstant(it: Iterator[Char]): Either[(AtomItem.Constant, Option[Char]), String] = if (it.hasNext) {
    val x = it.next()
    x match {
      case '"' => parseText(it, new mutable.StringBuilder(x)).left.map(_ -> None)
      case '[' | '(' => parseInterval(it, new mutable.StringBuilder(x)).left.map(_ -> None)
      case '<' => parseUri(it, new mutable.StringBuilder(x)).left.map(_ -> None)
      case x if x.isSpaceChar => parseConstant(it)
      case _ => parseOtherConstant(it, new mutable.StringBuilder(x))
    }
  } else {
    Right("Invalid rule atom.")
  }

  @tailrec
  private def parseOneOf(it: Iterator[Char], oneOf: AtomItem.OneOf): Either[AtomItem.OneOf, String] = if (it.hasNext) {
    it.next() match {
      case '}' => Left(oneOf)
      case '|' => parseOneOf(it, oneOf)
      case x if x.isSpaceChar => parseOneOf(it, oneOf)
      case x => parseConstant(Iterator(x) ++ it) match {
        case Left((x, lastChar)) => lastChar match {
          case Some(lastChar@('}' | '|')) => parseOneOf(Iterator(lastChar) ++ it, oneOf :+ x)
          case Some(x) => Right(s"Invalid character '$x' occured during OneOf parsing.")
          case None => parseOneOf(it, oneOf :+ x)
        }
        case Right(x) => Right(x)
      }
    }
  } else {
    Right("Invalid rule atom.")
  }

  @tailrec
  private def parseAtom(it: Iterator[Char], atom: Atom, pointer: Int): Either[Atom, String] = if (it.hasNext) {
    val x = it.next()
    x match {
      case ')' => Left(atom)
      case '?' => parseVariable(it) match {
        case Left(x) => parseAtom(it, atom.replace(x, pointer), pointer + 1)
        case Right(x) => Right(x)
      }
      case '!' | '{' =>
        val res = if (x == '!') {
          if (it.hasNext && it.next() == '{') {
            parseOneOf(it, AtomItem.OneOf(Vector.empty)).left.map(_.toNonOf)
          } else {
            Right(s"Invalid character '$x' occured during NonOf parsing.")
          }
        } else {
          parseOneOf(it, AtomItem.OneOf(Vector.empty))
        }
        res match {
          case Left(x) => parseAtom(it, atom.replace(x, pointer), pointer + 1)
          case Right(x) => Right(x)
        }
      case x if x.isSpaceChar => parseAtom(it, atom, pointer)
      case x => parseConstant(Iterator(x) ++ it) match {
        case Left((x, lastChar)) => lastChar match {
          case Some(')') => parseAtom(Iterator(')') ++ it, atom.replace(x, pointer), pointer + 1)
          case Some(x) => Right(s"Invalid character '$x' occured during atom parsing.")
          case None => parseAtom(it, atom.replace(x, pointer), pointer + 1)
        }
        case Right(x) => Right(x)
      }
    }
  } else {
    Right("Invalid rule atom.")
  }

  @tailrec
  private def parseHead(it: Iterator[Char], rule: Rule): Either[Rule, String] = if (it.hasNext) {
    it.next() match {
      case '(' => parseAtom(it, Atom(AtomItem.Any, AtomItem.Any, AtomItem.Any, AtomItem.Any), 0) match {
        case Left(atom) => parseHead(it, rule >+ atom)
        case Right(x) => Right(x)
      }
      case x if x.isSpaceChar => parseHead(it, rule)
      case x => Right(s"Invalid character '$x' occured during head parsing.")
    }
  } else {
    Left(rule)
  }

  @tailrec
  private def parseBody(it: Iterator[Char], rule: Rule, isStart: Boolean, nextAtom: Boolean): Either[Rule, String] = if (it.hasNext) {
    it.next() match {
      case '*' if isStart => parseBody(it, rule.partial, true, false)
      case '^' if !nextAtom => parseBody(it, rule, false, true)
      case '(' if nextAtom => parseAtom(it, Atom(AtomItem.Any, AtomItem.Any, AtomItem.Any, AtomItem.Any), 0) match {
        case Left(atom) => parseBody(it, rule +> atom, false, false)
        case Right(x) => Right(x)
      }
      case '=' if !nextAtom => if (it.hasNext && it.next() == '>') parseHead(it, rule) else Right("Invalid implication character.")
      case x if x.isSpaceChar => parseBody(it, rule, isStart, nextAtom)
      case x => Right(s"Invalid character '$x' occured during body parsing.")
    }
  } else {
    Right("Invalid rule pattern syntax.")
  }

  private def parseRule(x: String): Try[Rule] = parseBody(x.iterator, Rule(Vector.empty, Atom(AtomItem.Any, AtomItem.Any, AtomItem.Any, AtomItem.Any), true), true, true) match {
    case Left(rule) => Success(rule)
    case Right(x) => Failure(ValidationException(x))
  }

  private object RuleValidator extends Validate.Validator[String] {
    def validate(x: String): Try[String] = parseRule(x).map(_ => x)
  }

  private class PatternInput(implicit context: Context) extends Text("pattern", "Pattern", "", RuleValidator) {
    def toJson: js.Any = parseRule(getText).map(_.toJson).getOrElse(js.undefined)

    override def setValue(data: js.Dynamic): Unit = {
      if (js.isUndefined(data)) {
        super.setValue(js.Any.fromString("").asInstanceOf[js.Dynamic])
      } else {
        super.setValue(js.Any.fromString(Rule(data).toString).asInstanceOf[js.Dynamic])
      }
    }
  }

  def apply(name: String, title: String)(implicit context: Context): Property = ArrayElement(name, title) { implicit context =>
    new PatternInput
  }

  /*private def variableText: Property = new FixedText[String]("value", "Value", description = "In this position there must be a specified variable. The variable is just one character from a to z.", validator = RegExp("[a-z]"))

  private def constantText: Property = new FixedText[String]("value", "Value", description = "The constant has form as a triple item. It can be resource, text, number, boolean or interval. RESOURCE: <...> or prefix:localName, TEXT: \\\"...\\\", NUMBER: 0, BOOLEAN: true|false, INTERVAL: (x,y) or [x,y]", validator = NonEmpty)

  private def atomItemPatternProperties = {
    val value = new DynamicElement(Constants(
      variableText,
      constantText,
      new DynamicGroup("value", "Values", () => {
        val innerValue = new DynamicElement(Constants(
          variableText,
          constantText
        ))
        Constants(
          new Select("name", "Name", Constants("AnyConstant" -> "AnyConstant", "AnyVariable" -> "AnyVariable", "Constant" -> "Constant", "Variable" -> "Variable"), onSelect = {
            case "Variable" => innerValue.setElement(0)
            case "Constant" => innerValue.setElement(1)
            case _ => innerValue.setElement(-1)
          }),
          innerValue
        )
      })
    ))
    Constants(
      new Select("name", "Name", Constants("Any" -> "Any", "AnyConstant" -> "AnyConstant", "AnyVariable" -> "AnyVariable", "Constant" -> "Constant", "Variable" -> "Variable", "OneOf" -> "OneOf", "NoneOf" -> "NoneOf"), Some("Any"), {
        case "Variable" => value.setElement(0)
        case "Constant" => value.setElement(1)
        case "OneOf" | "NoneOf" => value.setElement(2)
        case _ => value.setElement(-1)
      }),
      value
    )
  }

  private def atomPatternProperties = Constants(
    new Group("subject", "Subject", atomItemPatternProperties),
    new Group("predicate", "Predicate", atomItemPatternProperties),
    new Group("object", "Object", atomItemPatternProperties),
    new Group("graph", "Graph", atomItemPatternProperties)
  )

  def apply(): Constants[Property] = Constants(
    new Group("head", "Head", atomPatternProperties, description = "Atom pattern for the head of a rule."),
    new DynamicGroup("body", "Body", () => atomPatternProperties, description = "Atom patterns for body of a rule."),
    new Checkbox("exact", "Exact", description = "If this field is checked then rules must match exactly this pattern. Otherwise, the partial matching is applied; that means rules must match the pattern but there may be additional atoms in the rule (the rule length may be greater than the pattern length)."),
    new Checkbox("orderless", "Orderless", description = "If this field is checked then it does not matter the order of atoms in the body of the rule and pattern.")
  )*/

}
