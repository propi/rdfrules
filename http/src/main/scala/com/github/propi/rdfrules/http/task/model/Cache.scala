package com.github.propi.rdfrules.http.task.model

import java.io.File

import com.github.propi.rdfrules.http.{InMemoryCache, Workspace}
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.model.Model

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Cache(path: String, inMemory: Boolean, revalidate: Boolean) extends Task.CacheTask[Model] {
  val companion: TaskDefinition = Cache

  def useCache(lastIndexTask: Option[Task[Task.NoInput.type, Index]]): Option[Task[Task.NoInput.type, Model]] = if (revalidate) {
    None
  } else {
    val model = if (inMemory) {
      InMemoryCache.get[Model](path)
    } else {
      val cacheFile = new File(Workspace.path(path))
      if (cacheFile.exists()) {
        Some(Model.fromCache(cacheFile))
      } else {
        None
      }
    }
    model.map(new Task.CachedTask[Model](companion, _))
  }

  def execute(input: Model): Model = {
    if (inMemory) {
      val cachedModel = input.cache
      InMemoryCache.put(path, cachedModel)
      cachedModel
    } else {
      input.cache(Workspace.path(path))
    }
  }
}

object Cache extends TaskDefinition {
  val name: String = "CacheRuleset"
}