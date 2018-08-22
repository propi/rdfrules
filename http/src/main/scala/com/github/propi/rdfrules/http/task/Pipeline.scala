package com.github.propi.rdfrules.http.task

import com.github.propi.rdfrules.data.Dataset

/**
  * Created by Vaclav Zeman on 7. 8. 2018.
  */
class Pipeline[O] private(head: Task[Task.NoInput.type, O],
                          datasets: List[Task.NoInputDatasetTask] = Nil) {

  def |~>[T](task: Task.NoInputDatasetTask): Pipeline[Dataset] = task match {
    case task: Task.MergeDatasets => head match {
      case oldTask: Task.NoInputDatasetTask => new Pipeline(task.addDatasetTasks(oldTask :: datasets))
      case _ => new Pipeline(task.addDatasetTasks(datasets))
    }
    case _ => head match {
      case oldTask: Task.NoInputDatasetTask => new Pipeline(task, oldTask :: datasets)
      case _ => new Pipeline(task)
    }
  }

  def ~>[T](task: Task[O, T]): Pipeline[T] = new Pipeline[T](head andThen task, datasets)

  def execute: O = head.execute(Task.NoInput)

}

object Pipeline {

  def apply[T](head: Task[Task.NoInput.type, T]): Pipeline[T] = new Pipeline(head)

}