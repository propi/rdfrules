package com.github.propi.rdfrules.http.task.ruleset

import java.io.File

import com.github.propi.rdfrules.http.{InMemoryCache, Workspace}
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.http.util.BasicExceptions.ValidationException
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Cache(path: String, inMemory: Boolean, revalidate: Boolean) extends Task.CacheTask[Ruleset] with Task.Prevalidate {

  self =>

  val companion: TaskDefinition = Cache

  def validate(): Option[ValidationException] = if (!inMemory && !Workspace.filePathIsWritable(path)) {
    Some(ValidationException("DirectoryIsNotWritable", "The directory for placing the file is not writable."))
  } else {
    None
  }

  def useCache(lastIndexTask: Option[Task[Task.NoInput.type, Index]]): Option[Task[Task.NoInput.type, Ruleset]] = if (revalidate) {
    None
  } else {
    if (inMemory) {
      InMemoryCache.get[Ruleset](path).map(new Task.CachedTask[Ruleset](companion, _))
    } else {
      val cacheFile = new File(Workspace.path(path))
      lastIndexTask match {
        case Some(lastIndexTask) if cacheFile.exists() =>
          Some(lastIndexTask.andThen[Ruleset](new Task[Index, Ruleset] {
            val companion: TaskDefinition = self.companion

            def execute(input: Index): Ruleset = {
              Ruleset.fromCache(input, cacheFile)
            }
          }))
        case _ => None
      }
    }
  }

  def execute(input: Ruleset): Ruleset = {
    if (inMemory) {
      val cachedRuleset = input.cache
      InMemoryCache.put(path, cachedRuleset)
      cachedRuleset
    } else {
      input.cache(Workspace.path(path))
    }
  }
}

object Cache extends TaskDefinition {
  val name: String = "CacheRuleset"
}