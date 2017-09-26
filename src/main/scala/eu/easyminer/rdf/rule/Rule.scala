package eu.easyminer.rdf.rule

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

}