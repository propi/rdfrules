package com.github.propi.rdfrules.http.task.data

import java.io.File

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.http.util.BasicExceptions.ValidationException
import com.github.propi.rdfrules.http.{InMemoryCache, Workspace}
import com.github.propi.rdfrules.index

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Cache(path: String, inMemory: Boolean, revalidate: Boolean) extends Task.CacheTask[Dataset] with Task.Prevalidate {
  val companion: TaskDefinition = Cache

  def validate(): Option[ValidationException] = if (!inMemory && !Workspace.filePathIsWritable(path)) {
    Some(ValidationException("DirectoryIsNotWritable", "The directory for placing the file is not writable."))
  } else {
    None
  }

  def useCache(lastIndexTask: Option[Task[Task.NoInput.type, index.Index]]): Option[Task[Task.NoInput.type, Dataset]] = if (revalidate) {
    None
  } else {
    val dataset = if (inMemory) {
      InMemoryCache.get[Dataset](path)
    } else {
      val cacheFile = new File(Workspace.path(path))
      if (cacheFile.exists()) {
        Some(Dataset.fromCache(cacheFile))
      } else {
        None
      }
    }
    dataset.map(new Task.CachedTask[Dataset](companion, _))
  }

  def execute(input: Dataset): Dataset = {
    if (inMemory) {
      val cachedDataset = input.cache
      InMemoryCache.put(path, cachedDataset)
      cachedDataset
    } else {
      input.cache(Workspace.path(path))
    }
  }
}

object Cache extends TaskDefinition {
  val name: String = "CacheDataset"
}