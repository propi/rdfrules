package com.github.propi.rdfrules.http.task

import com.github.propi.rdfrules.data.{Dataset, Graph}
import com.github.propi.rdfrules.http.util.BasicExceptions.ValidationException

import scala.reflect.ClassTag

/**
  * Created by Vaclav Zeman on 7. 8. 2018.
  */
class Pipeline[O] private(head: Task[Task.NoInput.type, O],
                          graphs: List[Task[Task.NoInput.type, Graph]] = Nil,
                          datasets: List[Task[Task.NoInput.type, Dataset]] = Nil) {

  private val `Task[Task.NoInput, Graph]` = implicitly[ClassTag[Task[Task.NoInput.type, Graph]]]
  private val `Task[Task.NoInput, Dataset]` = implicitly[ClassTag[Task[Task.NoInput.type, Dataset]]]

  def +[A, B](task: Task[A, B]): Pipeline[B] = {
    val `Task[Task.NoInput, B]` = implicitly[ClassTag[Task[Task.NoInput.type, B]]]
    val `Task[O, B]` = implicitly[ClassTag[Task[O, B]]]
    task match {
      case task: Task.MergeDatasets => head match {
        case `Task[Task.NoInput, Graph]`(oldTask) => new Pipeline(task.addGraphTasks(oldTask :: graphs).addDatasetTasks(datasets)).asInstanceOf[Pipeline[B]]
        case `Task[Task.NoInput, Dataset]`(oldTask) => new Pipeline(task.addGraphTasks(graphs).addDatasetTasks(oldTask :: datasets)).asInstanceOf[Pipeline[B]]
        case _ => new Pipeline(task.addGraphTasks(graphs).addDatasetTasks(datasets)).asInstanceOf[Pipeline[B]]
      }
      case `Task[Task.NoInput, B]`(task) => head match {
        case `Task[Task.NoInput, Graph]`(oldTask) => new Pipeline[B](task, oldTask :: graphs, datasets)
        case `Task[Task.NoInput, Dataset]`(oldTask) => new Pipeline[B](task, graphs, oldTask :: datasets)
        case _ => new Pipeline[B](task)
      }
      case `Task[O, B]`(task) => new Pipeline[B](head andThen task, graphs, datasets)
      case _ => throw ValidationException("NonBindingTask", s"Task '${task.name}' can not be bound to '${head.name}'.")
    }
  }

  def execute: O = head.execute(Task.NoInput)
}