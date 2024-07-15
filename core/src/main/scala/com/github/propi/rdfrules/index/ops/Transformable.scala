package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.index.{Index, IndexPart}

trait Transformable extends Index {

  def withMainPart(indexPart: IndexPart): Index

}
