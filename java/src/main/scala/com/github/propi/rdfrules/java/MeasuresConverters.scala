package com.github.propi.rdfrules.java

import com.github.propi.rdfrules.rule.Measure
import com.github.propi.rdfrules.utils.TypedKeyMap.Key

/**
  * Created by Vaclav Zeman on 7. 8. 2018.
  */
object MeasuresConverters {

  def Support: Key[Measure] = Measure.Support

  def HeadCoverage: Key[Measure] = Measure.HeadCoverage

  def HeadSize: Key[Measure] = Measure.HeadSize

  def BodySize: Key[Measure] = Measure.BodySize

  def Confidence: Key[Measure] = Measure.Confidence

  def HeadConfidence: Key[Measure] = Measure.HeadConfidence

  def Lift: Key[Measure] = Measure.Lift

  def PcaBodySize: Key[Measure] = Measure.PcaBodySize

  def PcaConfidence: Key[Measure] = Measure.PcaConfidence

  def Cluster: Key[Measure] = Measure.Cluster


}
