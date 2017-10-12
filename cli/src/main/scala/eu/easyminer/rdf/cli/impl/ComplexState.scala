package eu.easyminer.rdf.cli.impl

import eu.easyminer.rdf.cli.State
import eu.easyminer.rdf.data.Dataset

/**
  * Created by Vaclav Zeman on 7. 10. 2017.
  */
case class ComplexState private(dataset: Option[Dataset],
                                index: Option[ComplexIndex],
                                isTerminated: Boolean) extends State {

  def withDataset(dataset: Dataset): ComplexState = copy(dataset = Some(dataset))

  def withIndex(index: ComplexIndex): ComplexState = copy(index = Some(index))

  def terminate: ComplexState = copy(isTerminated = true)

  def clear: ComplexState = copy(dataset = None)

}

object ComplexState {

  def apply(): ComplexState = new ComplexState(None, None, false)

}