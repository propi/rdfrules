package com.github.propi.rdfrules.experiments

/**
  * Created by Vaclav Zeman on 8. 9. 2020.
  */
object AllTests {

  def main(args: Array[String]): Unit = {
    CompleteWorkflowScala.main(Array.empty)
    YagoAndDbpediaSamples.main(Array.empty)
    val input = args.headOption match {
      case Some(input) => s"-input $input"
      case None => ""
    }
    RdfRulesExperiments.main(s"-rungraphs $input -times 1 -output experiments/data/result_graphs.txt".split(' '))
    RdfRulesExperiments.main(s"-rundiscretization -input experiments/data/mappingbased_literals_sample.ttl.bz2 -times 1 -output experiments/data/result_discretization.txt".split(' '))
    RdfRulesExperiments.main(s"-runtopk $input -times 1 -output experiments/data/result_topk.txt".split(' '))
    RdfRulesExperiments.main(s"-runpatterns $input -times 1 -output experiments/data/result_patterns.txt".split(' '))
    RdfRulesExperiments.main(s"-runconfidence $input -times 1 -output experiments/data/result_confidence.txt".split(' '))
    RdfRulesExperiments.main(s"-runclusters $input -times 1 -output experiments/data/result_clusters.txt".split(' '))
    RdfRulesExperiments.main(s"-runpruning $input -times 1 -output experiments/data/result_pruning.txt".split(' '))
    RdfRulesExperiments.main(s"-runanytime $input -times 1 -output experiments/data/result_anytime.txt".split(' '))
    println("Successfully finished.")
  }

}