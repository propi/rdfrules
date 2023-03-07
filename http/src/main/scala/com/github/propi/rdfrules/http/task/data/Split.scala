package com.github.propi.rdfrules.http.task.data

import com.github.propi.rdfrules.data.ops.Sampleable
import com.github.propi.rdfrules.data.{Dataset, Graph, TripleItem}
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Split(train: (TripleItem.Uri, Sampleable.Part), test: (TripleItem.Uri, Sampleable.Part)) extends Task[Dataset, Dataset] {
  val companion: TaskDefinition = Split

  def execute(input: Dataset): Dataset = {
    val (trainingSet, testSet) = input.shuffle(train._2, test._2)
    Graph(train._1, trainingSet.triples).toDataset + Graph(test._1, testSet.triples)
  }
}

object Split extends TaskDefinition {
  val name: String = "Split"
}
