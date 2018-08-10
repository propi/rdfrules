package com.github.propi.rdfrules.http.util

import com.github.propi.rdfrules.utils.BasicExtractors.AnyToDouble

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
object ArithmeticEval {

  private sealed trait Exp

  private object Exp {

    object InBracket extends Exp

    object OutBracket extends Exp

    case class Number(x: Double) extends Exp

    object Times extends Exp

    object Add extends Exp

    object Minus extends Exp

    object Divide extends Exp

  }

  private def nextExp(x: String, prev: Option[Exp]): Option[(Exp, String)] = {
    val stripped = x.trim

    def withoutHead = stripped.substring(1)

    if (stripped.isEmpty) {
      None
    } else {
      stripped.head match {
        case '(' => Some(Exp.InBracket -> withoutHead)
        case ')' => Some(Exp.OutBracket -> withoutHead)
        case '+' if prev.exists(_.isInstanceOf[Exp.Number]) => Some(Exp.Add -> withoutHead)
        case '-' if prev.exists(_.isInstanceOf[Exp.Number]) => Some(Exp.Minus -> withoutHead)
        case '/' if prev.exists(_.isInstanceOf[Exp.Number]) => Some(Exp.Divide -> withoutHead)
        case '*' if prev.exists(_.isInstanceOf[Exp.Number]) => Some(Exp.Times -> withoutHead)
        case _ => "^[-]?[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?".r
          .findFirstIn(stripped)
          .flatMap(x => AnyToDouble.unapply(x).map(Exp.Number).map(_ -> stripped.stripPrefix(x)))
      }
    }
  }

  private def evalExps(exps: IndexedSeq[Exp]): Exp.Number = {
    val (res, rel) = exps.foldLeft(Vector.empty[Exp], Vector.empty[Exp]) { case ((res, rel), exp) =>
      rel :+ exp match {
        case Seq(Exp.Number(x1), Exp.Times, Exp.Number(x2)) => res -> Vector(Exp.Number(x1 * x2))
        case Seq(Exp.Number(x1), Exp.Divide, Exp.Number(x2)) => res -> Vector(Exp.Number(x1 / x2))
        case Seq(x, y, z) => (res :+ x :+ y) -> Vector(z)
        case x => res -> x
      }
    }
    val x = (Exp.Add +: (res ++ rel)).grouped(2).foldLeft(0.0) { (res, rel) =>
      rel match {
        case Seq(Exp.Add, Exp.Number(x)) => res + x
        case Seq(Exp.Minus, Exp.Number(x)) => res - x
        case _ => res
      }
    }
    Exp.Number(x)
  }

  @scala.annotation.tailrec
  private def innerEval(expsQueue: List[IndexedSeq[Exp]], rest: String): Option[Double] = if (rest.isEmpty) {
    expsQueue.headOption.map(evalExps).map(_.x)
  } else {
    nextExp(rest, expsQueue.headOption.flatMap(_.lastOption)) match {
      case Some((exp, rest)) => exp match {
        case Exp.InBracket => innerEval(Vector.empty :: expsQueue, rest)
        case Exp.OutBracket => expsQueue match {
          case head1 :: head2 :: tail => innerEval((head2 :+ evalExps(head1)) :: tail, rest)
          case head :: _ => innerEval(List(Vector(evalExps(head))), rest)
          case _ => None
        }
        case x => expsQueue match {
          case head :: tail => innerEval((head :+ x) :: tail, rest)
          case _ => innerEval(List(Vector(x)), rest)
        }
      }
      case None => None
    }
  }

  def apply(x: String): Option[Double] = innerEval(Nil, x)

}
