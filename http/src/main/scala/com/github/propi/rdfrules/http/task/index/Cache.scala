package com.github.propi.rdfrules.http.task.index

import com.github.propi.rdfrules.http.task.{CommonCache, TaskDefinition}
import com.github.propi.rdfrules.index.{IndexPart, Index}
import com.github.propi.rdfrules.utils.Debugger

import java.io.File

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Cache(path: String, inMemory: Boolean, revalidate: Boolean)(implicit debugger: Debugger) extends CommonCache[Index](path, inMemory, revalidate) {
  val companion: TaskDefinition = Cache

  def cacheInMemory(x: IndexPart): IndexPart = x

  def cacheOnDisk(x: IndexPart, path: String): IndexPart = x.cache(path)



  /*override def mapLoadedCache(x: Index): Index = x.withDebugger

  def loadCache(file: File): Option[Index] = Some(Index.fromCache(file, false))*/

  def cacheInMemory(x: Index): Index = ???

  def cacheOnDisk(x: Index, path: String): Index = ???

  def loadCache(file: File): Option[Index] = ???
}

object Cache extends TaskDefinition {
  val name: String = "CacheIndex"
}