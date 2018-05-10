package com.github.propi.rdfrules.java

/**
  * Created by Vaclav Zeman on 10. 5. 2018.
  */
object NumberConverters {

  def apply(x: java.lang.Integer): com.github.propi.rdfrules.data.TripleItem.Number[Int] = com.github.propi.rdfrules.data.TripleItem.Number(x)

  def apply(x: java.lang.Double): com.github.propi.rdfrules.data.TripleItem.Number[Double] = com.github.propi.rdfrules.data.TripleItem.Number(x)

  def apply(x: java.lang.Float): com.github.propi.rdfrules.data.TripleItem.Number[Float] = com.github.propi.rdfrules.data.TripleItem.Number(x)

  def apply(x: java.lang.Byte): com.github.propi.rdfrules.data.TripleItem.Number[Byte] = com.github.propi.rdfrules.data.TripleItem.Number(x)

  def apply(x: java.lang.Short): com.github.propi.rdfrules.data.TripleItem.Number[Short] = com.github.propi.rdfrules.data.TripleItem.Number(x)

  def apply(x: java.lang.Long): com.github.propi.rdfrules.data.TripleItem.Number[Long] = com.github.propi.rdfrules.data.TripleItem.Number(x)

}
