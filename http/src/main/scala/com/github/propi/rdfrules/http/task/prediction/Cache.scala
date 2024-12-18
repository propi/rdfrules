package com.github.propi.rdfrules.http.task.prediction

import com.github.propi.rdfrules.http.task.{CommonCache, TaskDefinition}
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.index.ops.TrainTestIndex
import com.github.propi.rdfrules.prediction.PredictedTriples
import com.github.propi.rdfrules.utils.Debugger

import java.io.File

class Cache(path: String, inMemory: Boolean, revalidate: Boolean)(implicit debugger: Debugger) extends CommonCache[PredictedTriples](path, inMemory, revalidate) {
  val companion: TaskDefinition = Cache

  def cacheInMemory(x: PredictedTriples): PredictedTriples = x.cache

  def cacheOnDisk(x: PredictedTriples, path: String): PredictedTriples = x.cache(path)

  def loadCache(file: File): Option[PredictedTriples] = None

  override def loadCacheWithIndex(file: File, index: Index): PredictedTriples = PredictedTriples.fromCache(TrainTestIndex(index), file)

  override def mapLoadedCache(x: PredictedTriples): PredictedTriples = x.withDebugger().withIndex(x.index.withDebugger)
}

object Cache extends TaskDefinition {
  val name: String = "CachePrediction"
}
