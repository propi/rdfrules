package com.github.propi.rdfrules.java

import java.util
import java.util.function.Function

import scala.collection.JavaConverters._

/**
  * Created by Vaclav Zeman on 3. 5. 2018.
  */
class SeqWrapper[T](_col: Seq[T]) {

  def map[T1](f: Function[T, T1]): SeqWrapper[T1] = new SeqWrapper[T1](_col.view.map(f.apply))

  def asJava: util.List[T] = _col.asJava

}