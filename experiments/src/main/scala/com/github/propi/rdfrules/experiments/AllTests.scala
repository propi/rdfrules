package com.github.propi.rdfrules.experiments

/**
  * Created by Vaclav Zeman on 8. 9. 2020.
  */
object AllTests {

  def main(args: Array[String]): Unit = {
    /*CompleteWorkflowScala.main(Array.empty)
    YagoAndDbpediaSamples.main(Array.empty)*/
    val input = args.headOption match {
      case Some(input) => s"-input $input"
      case None => ""
    }
    //OriginalAmieComparison.main(s"-rungraphs $input -times 1 -output experiments/data/result_graphs.txt".split(' '))
    OriginalAmieComparison.main(s"-rundiscretization -input experiments/data/mappingbased_literals_sample.ttl.bz2 -times 1 -output experiments/data/result_discretization.txt".split(' '))
    OriginalAmieComparison.main(s"-runlogical $input -times 1 -output experiments/data/result_logical.txt".split(' '))
    OriginalAmieComparison.main(s"-runconstants $input -times 1 -output experiments/data/result_constants.txt".split(' '))
    OriginalAmieComparison.main(s"-runcores $input -times 1 -output experiments/data/result_cores.txt".split(' '))
    OriginalAmieComparison.main(s"-runtopk $input -times 1 -output experiments/data/result_topk.txt".split(' '))
    OriginalAmieComparison.main(s"-runpatterns $input -times 1 -output experiments/data/result_patterns.txt".split(' '))
    OriginalAmieComparison.main(s"-runconfidence $input -times 1 -output experiments/data/result_confidence.txt".split(' '))
    OriginalAmieComparison.main(s"-runclusters $input -times 1 -output experiments/data/result_clusters.txt".split(' '))
    OriginalAmieComparison.main(s"-runpruning $input -times 1 -output experiments/data/result_pruning.txt".split(' '))
    println("Successfully finished.")
  }

}