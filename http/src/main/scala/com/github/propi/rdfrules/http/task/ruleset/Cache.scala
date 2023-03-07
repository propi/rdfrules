package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.task.{CommonCache, TaskDefinition}
import com.github.propi.rdfrules.index.IndexPart
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.Debugger

import java.io.File

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Cache(path: String, inMemory: Boolean, revalidate: Boolean)(implicit debugger: Debugger) extends CommonCache[Ruleset](path, inMemory, revalidate) {
  val companion: TaskDefinition = Cache

  def cacheInMemory(x: Ruleset): Ruleset = x.cache

  def cacheOnDisk(x: Ruleset, path: String): Ruleset = x.cache(path)

  def loadCache(file: File): Option[Ruleset] = None

  override def loadCacheWithIndex(file: File, index: IndexPart): Ruleset = Ruleset.fromCache(index, file)

  override def mapLoadedCache(x: Ruleset): Ruleset = x.withIndex(x.index.withDebugger)
}

object Cache extends TaskDefinition {
  val name: String = "CacheRuleset"
}