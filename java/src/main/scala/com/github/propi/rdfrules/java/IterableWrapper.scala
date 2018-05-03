package com.github.propi.rdfrules.java

import java.lang
import java.util.function.Function

import scala.collection.JavaConverters._

/**
  * Created by Vaclav Zeman on 3. 5. 2018.
  */
class IterableWrapper[T](_col: Iterable[T]) {

  def map[T1](f: Function[T, T1]): IterableWrapper[T1] = new IterableWrapper[T1](_col.view.map(f.apply))

  def asJava: lang.Iterable[T] = _col.asJava

}