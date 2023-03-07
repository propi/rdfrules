package com.github.propi.rdfrules.http.task

import com.github.propi.rdfrules.http.util.BasicExceptions.ValidationException
import com.github.propi.rdfrules.http.{InMemoryCache, Workspace}
import com.github.propi.rdfrules.index.Index

import java.io.File
import scala.reflect.ClassTag

abstract class CommonCache[T](path: String, inMemory: Boolean, revalidate: Boolean)(implicit tag: ClassTag[T]) extends Task.CacheTask[T] with Task.Prevalidate {
  self =>

  def cacheInMemory(x: T): T

  def cacheOnDisk(x: T, path: String): T

  def loadCache(file: File): Option[T]

  def loadCacheWithIndex(file: File, index: Index): T = loadCache(file).get

  def mapLoadedCache(x: T): T = x

  def validate(): Option[ValidationException] = if (!inMemory && !Workspace.filePathIsWritable(path)) {
    Some(ValidationException("DirectoryIsNotWritable", "The directory for placing the file is not writable."))
  } else {
    None
  }

  def useCache(lastIndexTask: Option[Task[Task.NoInput.type, Index]]): Option[Task[Task.NoInput.type, T]] = if (revalidate) {
    None
  } else {
    if (inMemory) {
      InMemoryCache.get[T](path).map(mapLoadedCache).map(new Task.CachedTask[T](companion, _))
    } else {
      val cacheFile = new File(Workspace.path(path))
      if (cacheFile.exists()) {
        lastIndexTask match {
          case Some(lastIndexTask) =>
            Some(lastIndexTask.andThen[T](new Task[Index, T] {
              val companion: TaskDefinition = self.companion

              def execute(input: Index): T = {
                mapLoadedCache(loadCacheWithIndex(cacheFile, input))
              }
            }))
          case None => loadCache(cacheFile).map(mapLoadedCache).map(new Task.CachedTask[T](companion, _))
        }
      } else {
        None
      }
    }
  }

  def execute(input: T): T = {
    if (inMemory) {
      val cachedModel = cacheInMemory(input)
      InMemoryCache.put(path, cachedModel)
      cachedModel
    } else {
      cacheOnDisk(input, Workspace.path(path))
    }
  }

}
