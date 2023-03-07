package com.github.propi.rdfrules.http.task.index

import com.github.propi.rdfrules.http.task.{CommonCache, TaskDefinition}
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.utils.Debugger

import java.io.File

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Cache(path: String, inMemory: Boolean, revalidate: Boolean)(implicit debugger: Debugger) extends CommonCache[Index](path, inMemory, revalidate) {
  val companion: TaskDefinition = Cache

  override def mapLoadedCache(x: Index): Index = x.withDebugger

  def cacheInMemory(x: Index): Index = x

  def cacheOnDisk(x: Index, path: String): Index = x.cache(path)

  def loadCache(file: File): Option[Index] = Some(Index.fromCache(file, false))
}

object Cache extends TaskDefinition {
  val name: String = "CacheIndex"
}