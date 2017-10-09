package eu.easyminer.rdf.cli.impl

import eu.easyminer.rdf.cli.State
import eu.easyminer.rdf.data.Dataset

/**
  * Created by Vaclav Zeman on 7. 10. 2017.
  */
class ComplexState(val dataset: Dataset, val isTerminated: Boolean) extends State {

  def withDataset(dataset: Dataset) = new ComplexState(dataset, isTerminated)

}
