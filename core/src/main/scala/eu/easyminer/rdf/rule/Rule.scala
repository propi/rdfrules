package eu.easyminer.rdf.rule

import eu.easyminer.rdf.data.TripleItem
import eu.easyminer.rdf.printer.Printer
import eu.easyminer.rdf.stringifier.Stringifier

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
trait Rule {

  val body: IndexedSeq[Atom]
  val head: Atom
  val measures: Measure.Measures

  lazy val ruleLength: Int = body.size + 1

  override def hashCode(): Int = {
    val support = measures[Measure.Support].value
    val headSize = measures[Measure.HeadSize].value
    val bodyHashCode = body.iterator.map { atom =>
      atom.predicate +
        (atom.subject match {
          case constant: Atom.Constant => constant.value
          case _ => 0
        }) +
        (atom.`object` match {
          case constant: Atom.Constant => constant.value
          case _ => 0
        })
    }.foldLeft(0)(_ ^ _)
    bodyHashCode + body.size * headSize + support
  }

}

object Rule {

  case class Simple(head: Atom, body: IndexedSeq[Atom])(val measures: Measure.Measures) extends Rule

  implicit def ruleStringifier(implicit item: Int => TripleItem): Stringifier[Rule] = (v: Rule) => v.body.map(x => Stringifier(x)).mkString(" ^ ") +
    " -> " +
    Stringifier(v.head) + " | " +
    v.measures.m.toList.sortBy(_._1).iterator.map(_._2).map(x => Stringifier(x)).mkString(", ")

  implicit def rulePrinter(implicit str: Stringifier[Rule]): Printer[Rule] = (v: Rule) => println(Stringifier(v))

}