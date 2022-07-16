package com.github.propi.rdfrules.http.task.model

import com.github.propi.rdfrules.http.task.{CommonCache, TaskDefinition}
import com.github.propi.rdfrules.model.Model

import java.io.File

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Cache(path: String, inMemory: Boolean, revalidate: Boolean) extends CommonCache[Model](path, inMemory, revalidate) {
  val companion: TaskDefinition = Cache

  def cacheInMemory(x: Model): Model = x.cache

  def cacheOnDisk(x: Model, path: String): Model = x.cache(path)

  def loadCache(file: File): Option[Model] = Some(Model.fromCache(file))
}

object Cache extends TaskDefinition {
  val name: String = "CacheModel"
}