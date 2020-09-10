package com.github.propi.rdfrules.data.ops

import com.github.propi.rdfrules.data.Quad
import com.github.propi.rdfrules.data.Quad.QuadTraversableView

/**
  * Created by Vaclav Zeman on 14. 1. 2020.
  */
trait QuadsOps[Coll] {

  def quads: QuadTraversableView

  /**
    * For all quads apply string interning
    *
    * @return
    */
  def intern: Coll = transformQuads(quads.map(_.intern))

  protected def transformQuads(col: Traversable[Quad]): Coll

}