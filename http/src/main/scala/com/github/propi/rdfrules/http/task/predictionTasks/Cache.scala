package com.github.propi.rdfrules.http.task.predictionTasks

import com.github.propi.rdfrules.http.task.{CommonCache, TaskDefinition}
import com.github.propi.rdfrules.prediction.PredictionTasksResults
import com.github.propi.rdfrules.utils.Debugger

import java.io.File

class Cache(revalidate: Boolean)(implicit debugger: Debugger) extends CommonCache[PredictionTasksResults]("", true, revalidate) {
  val companion: TaskDefinition = Cache

  def cacheInMemory(x: PredictionTasksResults): PredictionTasksResults = x.cache

  def cacheOnDisk(x: PredictionTasksResults, path: String): PredictionTasksResults = x.cache(path)

  def loadCache(file: File): Option[PredictionTasksResults] = None
}

object Cache extends TaskDefinition {
  val name: String = "CachePredictionTasks"
}