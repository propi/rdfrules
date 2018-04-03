package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.index.{TripleHashIndex, TripleItemHashIndex}

/**
  * Created by Vaclav Zeman on 13. 3. 2018.
  */
trait Buildable {

  protected def buildTripleHashIndex: TripleHashIndex

  protected def buildTripleItemHashIndex: TripleItemHashIndex

}
