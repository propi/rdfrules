# RDFRules: Experiments

This module has implemented some experiments with the RDFRules Scala Core API. There are 3 basic examples (as 3 main classes) and one main class for complex benchmark and comparison with the original AMIE+ implementation. The complex benchmark was launched on the CESNET Metacentrum computing cluster - results are also available within this GitHub repository. 

## Getting Started

Clone the RDFRules repository and run following SBT commands:
```
> project experiments
> run
```

After execution of the *run* command we can choose from three basic (fast) examples and one complex benchmark:
- **YagoAndDbpediaSamples**: 5 samples with DBpedia and YAGO datasets. It operates with the Scala API.
- **CompleteWorkflowScala**: one example with the complete workflow of rule mining. It operates with the Scala API.
- **OriginalAmieComparison**: complex bechmark with comparison to original AMIE+.
- **AllTests**: run all experiments with default settings and with `-input experiments/data/yago2core_facts.clean.notypes.tsv.bz2`.

## Benchmark

The main class *OriginalAmieComparison* requires some parameters to determine which kind of experiment should be launched:

```
> runMain com.github.propi.rdfrules.experiments.OriginalAmieComparison parameters...
```

| *Parameter name*  | *Description*                                                                                                                                                                                                                                                              | *Default*                                              |
|-------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------|
| coresc            | List of numbers of threads to be used                                                                                                                                                                                                                                      | Nil                                                    |
| cores             | If *coresc* is not specified, it performs experiments for number of threads from 1 to the *cores* value.                                                                                                                                                                   | available cores                                        |
| minhcs            | List of values with min head coverage thresholds.                                                                                                                                                                                                                          | 0.005,0.01,0.02,0.05,0.1,0.2,0.3                       |
| times             | Number of repetition of each task                                                                                                                                                                                                                                          | 7                                                      |
| input             | Input TSV dataset. Dataset can be compressed by GZIP or BZIP2.                                                                                                                                                                                                             | experiments/data/yago2core_facts.clean.notypes.tsv.bz2 |
| output            | Output file with benchmark results.                                                                                                                                                                                                                                        | experiments/data/results.txt                           |
| runtopk           | Run rule mining in the top-k mode. It contains *runconstants* and *runlogical* tasks with topK = 100.                                                                                                                                                                      |                                                        |
| runconstants      | Run rule mining with constants. Default confidence thresholds are: minConfidence = 0.1, minPcaConfidence = 0.1, numberOfThreads = $cores                                                                                                                                   |                                                        |
| runlogical        | Run rule mining only with variables. Default confidence thresholds are: minConfidence = 0.1, minPcaConfidence = 0.1, numberOfThreads = $cores                                                                                                                              |                                                        |
| runcores          | Run the experiment of scalability. Default thresholds are: minHeadCoverage = ${minhcs.head}, minConfidence = 0.1, minPcaConfidence = 0.1                                                                                                                                   |                                                        |
| runpatterns       | Run rule mining with patterns                                                                                                                                                                                                                                              |                                                        |
| runconfidence     | Run confidence computing separately. The default threshold is minPcaConfidence=0.1, input 10000 rules with/without constants.                                                                                                                                              |                                                        |
| rungraphs         | Run mining of rules from two different graphs and from merged graphs together by owl:sameAs links. YAGO and DBpedia samples are used.                                                                                                                                      |                                                        |
| rundiscretization | Run a heuristic for automatic discretization of numerical values in order to discover more interesting rules. There should by used an input dataset with numerical literals, e.g., experiments/data/mappingbased_literals_sample.ttl.bz2                                   |                                                        |
| runclusters       | Run DBScan clustering for mined rules by different settings with calculated inter/intra cluster similarities and final score. MinNeigbours is set to 1 and Eps (min similarity) is set by the *minsims* option. Rules are clusteted based on their body and head contents. |                                                        |
| minsims           | Minimum similarities to make a cluster by the clustering experiment.                                                                                                                                                                                                       | 0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8                        |
| runpruning        | Run CBA pruning strategy for reducing number of rules and compare results with the original ruleset. The original ruleset is mined by topK approach with different values set by the *topks* option and with MinHC=0.01 and MinConf=0.1                                    |                                                        |
| topks             | TopK values for the pruning experiment.                                                                                                                                                                                                                                    | 500,1000,2000,4000,8000,16000,32000                    |
| rdfrulesonly      | Run only RDFRules                                                                                                                                                                                                                                                          |                                                        |
| amieonly          | Run only AMIE+                                                                                                                                                                                                                                                             |                                                        |

### Performed experiments

We performed some experiments on the [CESNET Metacentrum](https://www.metacentrum.cz/en/index.html) computing cluster with RDFRules 1.2.1 and the [sample YAGO dataset](./data/yago2core_facts.clean.notypes.tsv.bz2) containing around 1M triples with following input parameters:

```
# task1 - mining only with variables
> runMain com.github.propi.rdfrules.experiments.OriginalAmieComparison -cores 8 -runlogical -minhcs 0.2,0.1,0.05,0.02,0.01,0.005,0.003,0.001
# task2 - mining with constants
> runMain com.github.propi.rdfrules.experiments.OriginalAmieComparison -cores 8 -runconstants
# task3 - scalability
> runMain com.github.propi.rdfrules.experiments.OriginalAmieComparison -cores 12 -minhcs 0.01 -runcores
# task4 - scalability
> runMain com.github.propi.rdfrules.experiments.OriginalAmieComparison -cores 24 -coresc 12,16,20,24 -minhcs 0.01,0.005 -runcores
# task5 - mining with constants with one thread
> runMain com.github.propi.rdfrules.experiments.OriginalAmieComparison -cores 1 -minhcs 0.01 -runconstants
# task6 - topK, patterns and confidence computing experiments with 8 cores
> runMain com.github.propi.rdfrules.experiments.OriginalAmieComparison -cores 8 -runtopk -runpatterns -runconfidence
# task7 - rule mining only with variables with a low head coverage
> runMain com.github.propi.rdfrules.experiments.OriginalAmieComparison -cores 8 -runlogical -minhcs 0.2,0.1,0.05,0.02,0.01,0.005,0.003,0.001
```

For RDFRules 1.4.0, the experiments were extended by dbpedia, discretization, clustering, pruning, and multiple-graphs experiments.

```
# task8 - mining rules without constants by AMIE+ for DBpedia 3.8
> runMain com.github.propi.rdfrules.experiments.OriginalAmieComparison -cores 8 -times 1 -input "experiments/data/dbpedia.3.8.tsv.bz2" -minhcs 0.01 -runlogical -amieonly
# task9 - mining rules without constants by RDFRules for DBpedia 3.8
> runMain com.github.propi.rdfrules.experiments.OriginalAmieComparison -cores 8 -times 1 -input "experiments/data/dbpedia.3.8.tsv.bz2" -minhcs 0.01 -runlogical -rdfrulesonly
# task10 - auto discretization of numerical values
> runMain com.github.propi.rdfrules.experiments.OriginalAmieComparison -cores 8 -rundiscretization -minhcs 0.01,0.02,0.05,0.1,0.2,0.3 -input "experiments/data/mappingbased_literals_sample.ttl.bz2"
# task11 - clustering by different settings with calculated inter/intra cluster similarities and final score
> runMain com.github.propi.rdfrules.experiments.OriginalAmieComparison -cores 8 -runclusters
# task12 - CBA pruning strategy for reducing number of rules without loss of predicted triples 
> runMain com.github.propi.rdfrules.experiments.OriginalAmieComparison -cores 8 -runpruning
# task13 - comparison between mining of rules from two different graphs and from merged graphs together
> runMain com.github.propi.rdfrules.experiments.OriginalAmieComparison -cores 8 -rungraphs
```

The results of individual tasks are placed in the [results](./results) folder. The parameters of the used machine for experiments are:
- CPU: 4x 14-core Intel Xeon E7-4830 v4 (2GHz),
- RAM: 512 GB,
- OS: Debian 9.

All experiments can be launched with default settings with the *yago2core_facts.clean.notypes.tsv.bz2* dataset and with the *mappingbased_literals_sample.ttl.bz2* dataset for discretization by the main class *AllTests*:

```
> runMain com.github.propi.rdfrules.experiments.AllTests
```
