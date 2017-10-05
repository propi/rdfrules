package eu.easyminer.rdf.task.impl

import eu.easyminer.rdf.rule._
import eu.easyminer.rdf.rule.Threshold.Thresholds
import eu.easyminer.rdf.task.InputTaskParser
import eu.easyminer.rdf.task.InputTaskParser.InputTask
import eu.easyminer.rdf.utils.BasicExtractors.{AnyToDouble, AnyToInt}

/**
  * Created by Vaclav Zeman on 12. 7. 2017.
  */
trait LineInputTaskParser extends InputTaskParser[String] {

  case class Argument(name: String, value: String)

  private def parseThresholds(args: Array[Argument]) = {
    val thresholds: Thresholds = Thresholds()
    args.foreach {
      case Argument("s", AnyToInt(minSupport)) => thresholds += Threshold.MinSupport(minSupport)
      case Argument("hc", AnyToDouble(minHeadCoverage)) => thresholds += Threshold.MinHeadCoverage(minHeadCoverage)
      case Argument("c", AnyToDouble(minConfidence)) => thresholds += Threshold.MinConfidence(minConfidence)
      case Argument("l", AnyToInt(maxRuleLength)) => thresholds += Threshold.MaxRuleLength(maxRuleLength)
      case _ =>
    }
    thresholds
  }

  private def parseConstraints(args: Array[Argument]) = {
    val constraints = collection.mutable.ListBuffer.empty[RuleConstraint]
    args.foreach {
      case Argument("i", _) => constraints += RuleConstraint.WithInstances
      case Argument("wd", _) => constraints += RuleConstraint.WithoutDuplicitPredicates
      case _ =>
    }
    constraints.toList
  }

  //rp=(?2,123,547)^(?2,158,?1)->(?0,156,?1)
  private def parseRulePattern(args: Array[Argument]) = {
    val AtomRegExp = """\((.+?),(.*?),(.+?)\)""".r
    val AtomVariable = """\?(\d+)""".r
    val AtomConstant = """(\d+)""".r
    def parseAtomItem(atomItem: String): Option[Atom.Item] = atomItem match {
      case AtomVariable(AnyToInt(x)) => Some(Atom.Variable(x))
      case AtomConstant(AnyToInt(x)) => Some(Atom.Constant(x))
      case _ => None
    }
    def parseAtom(atom: String) = atom match {
      case AtomRegExp(s, p, o) => Some(parseAtomItem(s), parseAtomItem(p).collect { case Atom.Constant(x) => x }, parseAtomItem(o)).collect {
        case (Some(s: Atom.Item), p, Some(o: Atom.Item)) => AtomPattern(s, p, o)
      }
      case _ => None
    }
    args.collectFirst {
      case Argument("rp", pattern) => pattern.split("->") match {
        case Array(bodyPattern, headPattern) => parseAtom(headPattern).map(RulePattern.apply).map { rp =>
          bodyPattern.split('^').reverseIterator.flatMap(parseAtom).foldLeft(rp)(_ + _)
        }
        case _ => None
      }
    }.flatten
  }

  def parse(inputTask: String): InputTask = {
    val args = inputTask.split(' ').map(_.split('=')).collect {
      case Array(arg1, arg2) => Argument(arg1, arg2)
    }
    new InputTask("InputTask: " + inputTask, parseThresholds(args), parseRulePattern(args), parseConstraints(args))
  }

}
