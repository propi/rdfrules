package com.github.propi.rdfrules.http.task

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.index.Index

import scala.reflect.runtime.universe._

/**
  * Created by Vaclav Zeman on 7. 8. 2018.
  */
class Pipeline[O] private(head: Task[Task.NoInput.type, O],
                          datasets: List[Task.NoInputDatasetTask],
                          lastIndexTask: Option[Task[Task.NoInput.type, Index]]) {

  def |~>[T](task: Task.NoInputDatasetTask): Pipeline[Dataset] = task match {
    case task: Task.MergeDatasets => head match {
      case oldTask: Task.NoInputDatasetTask => new Pipeline(task.addDatasetTasks(oldTask :: datasets), Nil, lastIndexTask)
      case _ => new Pipeline(task.addDatasetTasks(datasets), Nil, lastIndexTask)
    }
    case _ => head match {
      case oldTask: Task.NoInputDatasetTask => new Pipeline(task, oldTask :: datasets, lastIndexTask)
      case _ => new Pipeline(task, Nil, lastIndexTask)
    }
  }

  private def pipeWith[T](head: Task[Task.NoInput.type, T], datasets: List[Task.NoInputDatasetTask])(implicit tag: TypeTag[T]): Pipeline[T] = head match {
    case value if typeOf[Task[Task.NoInput.type, T]] =:= typeOf[Task[Task.NoInput.type, Index]] => new Pipeline[T](head, datasets, Some(value.asInstanceOf[Task[Task.NoInput.type, Index]]))
    case _ => new Pipeline[T](head, datasets, lastIndexTask)
  }

  def ~>[T](task: Task[O, T])(implicit tag: TypeTag[T]): Pipeline[T] = task match {
    case task: Task.CacheTask[_] =>
      task.useCache(lastIndexTask) match {
        case Some(cached) => pipeWith[T](cached.asInstanceOf[Task[Task.NoInput.type, T]], Nil)
        case None => pipeWith[T](head andThen task, datasets)
      }
    case _ => pipeWith[T](head andThen task, datasets)
  }

  def execute: O = head.execute(Task.NoInput)

}

object Pipeline {

  def apply[T](head: Task[Task.NoInput.type, T]): Pipeline[T] = new Pipeline(head, Nil, None)

}