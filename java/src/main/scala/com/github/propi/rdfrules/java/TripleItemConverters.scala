package com.github.propi.rdfrules.java

import com.github.propi.rdfrules.java.data.TripleItem

/**
  * Created by Vaclav Zeman on 10. 5. 2018.
  */
object TripleItemConverters {

  def toJavaTripleItem(x: com.github.propi.rdfrules.data.TripleItem): TripleItem = x match {
    case x: com.github.propi.rdfrules.data.TripleItem.LongUri => new TripleItem.LongUri(x)
    case x: com.github.propi.rdfrules.data.TripleItem.PrefixedUri => new TripleItem.PrefixedUri(x)
    case x: com.github.propi.rdfrules.data.TripleItem.BlankNode => new TripleItem.BlankNode(x)
    case x: com.github.propi.rdfrules.data.TripleItem.BooleanValue => new TripleItem.BooleanValue(x)
    case x: com.github.propi.rdfrules.data.TripleItem.Text => new TripleItem.Text(x)
    case x: com.github.propi.rdfrules.data.TripleItem.Interval => new TripleItem.Interval(x)
    case n@com.github.propi.rdfrules.data.TripleItem.Number(_) => TripleItem.Number.fromNumber(n)
    /*case com.github.propi.rdfrules.data.TripleItem.Number(x: Int) => new TripleItem.Number(new java.lang.Integer(x))
    case com.github.propi.rdfrules.data.TripleItem.Number(x: Double) => new TripleItem.Number(new java.lang.Double(x))
    case com.github.propi.rdfrules.data.TripleItem.Number(x: Float) => new TripleItem.Number(new java.lang.Float(x))
    case com.github.propi.rdfrules.data.TripleItem.Number(x: Short) => new TripleItem.Number(new java.lang.Short(x))
    case com.github.propi.rdfrules.data.TripleItem.Number(x: Byte) => new TripleItem.Number(new java.lang.Byte(x))
    case com.github.propi.rdfrules.data.TripleItem.Number(x: Long) => new TripleItem.Number(new java.lang.Long(x))
    case n@com.github.propi.rdfrules.data.TripleItem.Number(x) => new TripleItem.Number(new java.lang.Double(n.n.toDouble(x)))*/
  }

  def toJavaUri(x: com.github.propi.rdfrules.data.TripleItem.Uri): TripleItem.Uri = x match {
    case x: com.github.propi.rdfrules.data.TripleItem.LongUri => new TripleItem.LongUri(x)
    case x: com.github.propi.rdfrules.data.TripleItem.PrefixedUri => new TripleItem.PrefixedUri(x)
    case x: com.github.propi.rdfrules.data.TripleItem.BlankNode => new TripleItem.BlankNode(x)
  }

}