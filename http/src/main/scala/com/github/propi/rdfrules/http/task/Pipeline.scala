package com.github.propi.rdfrules.http.task

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.http.util.BasicExceptions.ValidationException

import scala.reflect.ClassTag

/**
  * Created by Vaclav Zeman on 7. 8. 2018.
  */
class Pipeline[O] private(head: Task[Task.NoInput.type, O],
                          datasets: List[Task[Task.NoInput.type, Dataset]] = Nil) {

  private val `Task[Task.NoInput, Dataset]` = implicitly[ClassTag[Task[Task.NoInput.type, Dataset]]]

  def +[A, B](task: Task[A, B]): Pipeline[B] = {
    val `Task[Task.NoInput, B]` = implicitly[ClassTag[Task[Task.NoInput.type, B]]]
    val `Task[O, B]` = implicitly[ClassTag[Task[O, B]]]
    val `Task[Any, B]` = implicitly[ClassTag[Task[Any, B]]]
    task match {
      case task: Task.MergeDatasets => head match {
        case `Task[Task.NoInput, Dataset]`(oldTask) => new Pipeline(task.addDatasetTasks(oldTask :: datasets)).asInstanceOf[Pipeline[B]]
        case _ => new Pipeline(task.addDatasetTasks(datasets)).asInstanceOf[Pipeline[B]]
      }
      case `Task[Task.NoInput, B]`(task) => head match {
        case `Task[Task.NoInput, Dataset]`(oldTask) => new Pipeline[B](task, oldTask :: datasets)
        case _ => new Pipeline[B](task)
      }
      case `Task[O, B]`(task) => new Pipeline[B](head andThen task, datasets)
      case `Task[Any, B]`(task) => new Pipeline[B](head andThenAny task, datasets)
      case _ => throw ValidationException("NonBindingTask", s"Task '${task.name}' can not be bound to '${head.name}'.")
    }
  }

  def execute: O = head.execute(Task.NoInput)
}

object Pipeline {

  def apply[T](head: Task[Task.NoInput.type, T]): Pipeline[T] = new Pipeline(head)

}