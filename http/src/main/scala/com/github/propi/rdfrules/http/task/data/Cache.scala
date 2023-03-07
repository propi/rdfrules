package com.github.propi.rdfrules.http.task.data

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.http.task.{CommonCache, TaskDefinition}
import com.github.propi.rdfrules.utils.Debugger

import java.io.File

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Cache(path: String, inMemory: Boolean, revalidate: Boolean)(implicit debugger: Debugger) extends CommonCache[Dataset](path, inMemory, revalidate)  {
  val companion: TaskDefinition = Cache

  def cacheInMemory(x: Dataset): Dataset = x.withPrefixedUris.intern.cache

  def cacheOnDisk(x: Dataset, path: String): Dataset = x.cache(path)

  def loadCache(file: File): Option[Dataset] = Some(Dataset.fromCache(file))

  override def mapLoadedCache(x: Dataset): Dataset = x.withDebugger()
}

object Cache extends TaskDefinition {
  val name: String = "CacheDataset"
}