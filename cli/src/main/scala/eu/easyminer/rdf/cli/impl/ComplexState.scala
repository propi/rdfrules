package eu.easyminer.rdf.cli.impl

import eu.easyminer.rdf.cli.State
import eu.easyminer.rdf.data.Dataset

/**
  * Created by Vaclav Zeman on 7. 10. 2017.
  */
class ComplexState private(val dataset: Option[Dataset], val isTerminated: Boolean) extends State {

  def withDataset(dataset: Dataset) = new ComplexState(Some(dataset), isTerminated)

  def terminate = new ComplexState(dataset, true)

  def clear = new ComplexState(None, isTerminated)

}

object ComplexState {

  def apply(): ComplexState = new ComplexState(None, false)

}