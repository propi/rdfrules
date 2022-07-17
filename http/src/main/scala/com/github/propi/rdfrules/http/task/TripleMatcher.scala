package com.github.propi.rdfrules.http.task

import com.github.propi.rdfrules.data.Triple
import com.github.propi.rdfrules.http.task.TripleMatcher.CapturedTripleItems

/**
  * Created by Vaclav Zeman on 8. 8. 2018.
  */
class TripleMatcher private(quadMatcher: QuadMatcher) {
  def matchAll(triple: Triple): CapturedTripleItems = quadMatcher.matchAll(triple.toQuad)
}

object TripleMatcher {

  trait CapturedTripleItems {
    val s: IndexedSeq[String]
    val p: IndexedSeq[String]
    val o: IndexedSeq[String]

    def matched: Boolean
  }

  def apply(s: Option[String],
            p: Option[String],
            o: Option[String]): TripleMatcher = new TripleMatcher(QuadMatcher(s, p, o, None))

}