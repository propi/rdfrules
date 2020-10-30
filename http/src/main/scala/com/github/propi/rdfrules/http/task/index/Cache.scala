package com.github.propi.rdfrules.http.task.index

import java.io.File

import com.github.propi.rdfrules.http.{InMemoryCache, Workspace}
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.http.util.BasicExceptions.ValidationException
import com.github.propi.rdfrules.index.Index

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Cache(path: String, inMemory: Boolean, revalidate: Boolean) extends Task.CacheTask[Index] with Task.Prevalidate {
  val companion: TaskDefinition = Cache

  def validate(): Option[ValidationException] = if (!inMemory && !Workspace.filePathIsWritable(path)) {
    Some(ValidationException("DirectoryIsNotWritable", "The directory for placing the file is not writable."))
  } else {
    None
  }

  def useCache(lastIndexTask: Option[Task[Task.NoInput.type, Index]]): Option[Task[Task.NoInput.type, Index]] = if (revalidate) {
    None
  } else {
    val index = if (inMemory) {
      InMemoryCache.get[Index](path).map(_.cache())
    } else {
      val cacheFile = new File(Workspace.path(path))
      if (cacheFile.exists()) {
        Some(Index.fromCache(cacheFile, false))
      } else {
        None
      }
    }
    index.map(new Task.CachedTask[Index](companion, _))
  }

  def execute(input: Index): Index = {
    if (inMemory) {
      InMemoryCache.put(path, input)
      input
    } else {
      input.cache(Workspace.path(path))
    }
  }
}

object Cache extends TaskDefinition {
  val name: String = "CacheIndex"
}