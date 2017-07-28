package eu.easyminer.rdf.rule

/**
  * Created by Vaclav Zeman on 12. 7. 2017.
  */
trait RuleStringifier {

  protected val mapper: Map[Int, String]

  def stringifyAtomItem(atomItem: Atom.Item) = atomItem match {
    case x: Atom.Variable => x.value
    case Atom.Constant(x) => mapper(x)
  }

  def stringifyAtom(atom: Atom) = s"(${stringifyAtomItem(atom.subject)} ${mapper(atom.predicate)} ${stringifyAtomItem(atom.`object`)})"

  def stringifyRule(rule: Rule) = rule.body.map(stringifyAtom).mkString(" ^ ") + " -> " + stringifyAtom(rule.head) + " | " +
    "headSize: " + rule.measures(Measure.HeadSize).asInstanceOf[Measure.HeadSize].value + "," +
    "support: " + rule.measures(Measure.Support).asInstanceOf[Measure.Support].value + "," +
    "headCoverage: " + rule.measures(Measure.HeadCoverage).asInstanceOf[Measure.HeadCoverage].value + "," +
    "confidence: " + rule.measures(Measure.Confidence).asInstanceOf[Measure.Confidence].value

}