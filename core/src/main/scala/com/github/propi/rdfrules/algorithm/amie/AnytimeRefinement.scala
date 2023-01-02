package com.github.propi.rdfrules.algorithm.amie

import com.github.propi.rdfrules.algorithm.amie.AnytimeRefinement.Checker
import com.github.propi.rdfrules.rule.Threshold

trait AnytimeRefinement {
  def anytimeRefine[T](stop: () => Unit)(f: Checker => T): T
}

object AnytimeRefinement {

  trait Checker {
    def checkTime(currentTime: Long): Unit

    def checkSupport(support: Int): Unit
  }

  private object EmptyChecker extends Checker {
    def checkTime(currentTime: Long): Unit = ()

    def checkSupport(support: Int): Unit = ()
  }

  object Empty extends AnytimeRefinement {
    def anytimeRefine[T](stop: () => Unit)(f: Checker => T): T = f(EmptyChecker)
  }

  class Comb private[AnytimeRefinement](seq: List[AnytimeRefinement]) extends AnytimeRefinement {
    private class CombChecker(seq: Vector[Checker]) extends Checker {
      def checkTime(currentTime: Long): Unit = seq.foreach(_.checkTime(currentTime))

      def checkSupport(support: Int): Unit = seq.foreach(_.checkSupport(support))
    }

    def ::(that: AnytimeRefinement): Comb = new Comb(that :: seq)

    def anytimeRefine[T](stop: () => Unit)(f: Checker => T): T = {
      def buildCheckers(seq: List[AnytimeRefinement], res: Vector[Checker]): T = seq match {
        case head :: tail => head.anytimeRefine(stop)(checker => buildCheckers(tail, res :+ checker))
        case Nil => f(new CombChecker(res))
      }

      buildCheckers(seq, Vector.empty)
    }
  }

  class GlobalTimeout(end: Long) extends AnytimeRefinement {
    private class CheckerImpl(stop: () => Unit) extends Checker {
      def checkTime(currentTime: Long): Unit = if (currentTime > end) stop()

      def checkSupport(support: Int): Unit = ()
    }

    def anytimeRefine[T](stop: () => Unit)(f: Checker => T): T = f(new CheckerImpl(stop))
  }

  class LocalTimeout(localTimeout: Threshold.LocalTimeout) extends AnytimeRefinement {
    private def calcMer(me: Double): Double = 1 / math.pow(me / 1.96, 2)

    private val mer: Double => Double = if (localTimeout.dme) {
      hc => calcMer(math.min(-localTimeout.me / math.log10(hc), localTimeout.me))
    } else {
      lazy val mer = calcMer(localTimeout.me)
      _ => mer
    }

    private class TimeChecker(stop: () => Unit) extends Checker {
      private val end = System.currentTimeMillis() + localTimeout.value.toMillis

      def checkTime(currentTime: Long): Unit = if (currentTime > end) stop()

      def checkSupport(support: Int): Unit = ()
    }

    private class SamplesChecker(stop: () => Unit, checkingActivated: Boolean) extends Checker {
      //we start at 2 because we do not want hc=0 or hc=1 which causes minSampleSize=0
      private var i = 2.0
      private var nearestToHalfHc = 0.5
      private var nearestToHalfHcThreshold = 1.0
      private var _checkingActivated = checkingActivated

      /**
        * Nearest to 0.5 head coverage searching for a time window
        * Closer to 0.5 means higher sample size threshold
        *
        * @param support support
        */
      def checkSupport(support: Int): Unit = {
        //zero support is not allowed because we do not want hc=0 which causes minSampleSize=0
        val hc = if (support == 0) 1 else support / i
        val hcThreshold = math.abs(hc - 0.5)
        if (hcThreshold < nearestToHalfHcThreshold) {
          nearestToHalfHc = hc
          nearestToHalfHcThreshold = hcThreshold
        }
      }

      def activateChecking(): Unit = {
        _checkingActivated = true
      }

      def checkTime(currentTime: Long): Unit = {
        if (_checkingActivated) {
          val minSampleSize = nearestToHalfHc * (1.0 - nearestToHalfHc) * mer(nearestToHalfHc)
          if (i >= minSampleSize) {
            //println(s"samples reached: $nearestToHalfHc, ss: $minSampleSize, bs: $i")
            stop()
          }
        }
        //end of the current time window, reset nearestToHalfHc because supports and hcs may change within next time window
        nearestToHalfHcThreshold = 1.0
        i += 1
      }
    }

    private class CombChecker(stop: () => Unit) extends Checker {
      private val samplesChecker = new SamplesChecker(stop, false)
      private val timeChecker = new TimeChecker(() => samplesChecker.activateChecking())

      def checkTime(currentTime: Long): Unit = {
        timeChecker.checkTime(currentTime)
        samplesChecker.checkTime(currentTime)
      }

      def checkSupport(support: Int): Unit = samplesChecker.checkSupport(support)
    }

    def anytimeRefine[T](stop: () => Unit)(f: Checker => T): T = {
      localTimeout.hasDuration -> localTimeout.hasMarginError match {
        case (true, true) => f(new CombChecker(stop))
        case (true, false) => f(new TimeChecker(stop))
        case (false, true) => f(new SamplesChecker(stop, true))
        case (false, false) => f(EmptyChecker)
      }
    }
  }

  implicit class PimpedAnytimeRefinement(val anytimeRefinement: AnytimeRefinement) extends AnyVal {
    def ::(that: AnytimeRefinement): AnytimeRefinement = anytimeRefinement match {
      case comb: Comb => that :: comb
      case _ => new Comb(List(that, anytimeRefinement))
    }
  }

}