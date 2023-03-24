# RDFRules: Experiments

This module has implemented some experiments with the RDFRules Scala Core API. Experiments are divided into four modules:
- Experiments module (this directory): Experiments with some RDFRules heuristics and operations (such as discretization, mining from multiple graphs, top-k strategy, patterns). All experiments of this module are described in this directory (see below).
- [Experiments AMIE2 module](../experiments_amie2): RDFRules vs AMIE+
- [Experiments AMIE3 module](../experiments_amie3): RDFRules vs AMIE3
- [Experiments KGC module](../experiments_kgc): RDFRules vs AnyBURL and experiments with various settings of RDFRules for knowledge graph completion.

Benchmarks were launched on the CESNET Metacentrum computing cluster - results are also available within this GitHub repository. 

## Getting Started

Clone the RDFRules repository and run following SBT commands:
```
> project experiments
> run
```

After execution of the *run* command we can choose from three basic (fast) examples and one complex benchmark:
- **YagoAndDbpediaSamples**: 5 samples with DBpedia and YAGO datasets. It operates with the Scala API.
- **CompleteWorkflowScala**: one example with the complete workflow of rule mining. It operates with the Scala API.
- **RdfRulesExperiments**: complex bechmark of some operations implemented in RDFRules.
- **AllTests**: run all experiments with default settings and with `-input experiments/data/yago2core_facts.clean.notypes.tsv.bz2`.

## Benchmarks

The main class *RdfRulesExperiments* requires some parameters to determine which kind of experiment should be launched:

```
> runMain com.github.propi.rdfrules.experiments.RdfRulesExperiments parameters...
```

| *Parameter name*  | *Description*                                                                                                                                                                                                                                                              | *Default*                                              |
|-------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------|
| cores             | Number of threads.                                                                                                                                                                                                                                                         | available cores                                        |
| minhcs            | List of values with min head coverage thresholds.                                                                                                                                                                                                                          | 0.005,0.01,0.02,0.05,0.1,0.2,0.3                       |
| times             | Number of repetition of each task                                                                                                                                                                                                                                          | 7                                                      |
| input             | Input TSV dataset. Dataset can be compressed by GZIP or BZIP2.                                                                                                                                                                                                             | experiments/data/yago2core_facts.clean.notypes.tsv.bz2 |
| output            | Output file with benchmark results.                                                                                                                                                                                                                                        | experiments/data/results.txt                           |
| topks             | TopK values for the pruning experiment.                                                                                                                                                                                                                                    | 500,1000,2000,4000,8000,16000,32000                    |
| minsims           | Minimum similarities to make a cluster by the clustering experiment.                                                                                                                                                                                                       | 0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8                        |
| runtopk           | Run rule mining in the top-k mode. It contains *runconstants* and *runlogical* tasks with topK = 100.                                                                                                                                                                      |                                                        |                                                    |
| runpatterns       | Run rule mining with patterns                                                                                                                                                                                                                                              |                                                        |
| runconfidence     | Run confidence computing separately. The default threshold is minPcaConfidence=0.1, input 10000 rules with/without constants.                                                                                                                                              |                                                        |
| rungraphs         | Run mining of rules from two different graphs and from merged graphs together by owl:sameAs links. YAGO and DBpedia samples are used.                                                                                                                                      |                                                        |
| rundiscretization | Run a heuristic for automatic discretization of numerical values in order to discover more interesting rules. There should by used an input dataset with numerical literals, e.g., experiments/data/mappingbased_literals_sample.ttl.bz2                                   |                                                        |
| runclusters       | Run DBScan clustering for mined rules by different settings with calculated inter/intra cluster similarities and final score. MinNeigbours is set to 1 and Eps (min similarity) is set by the *minsims* option. Rules are clusteted based on their body and head contents. |                                                        |
| runpruning        | Run CBA pruning strategy for reducing number of rules and compare results with the original ruleset. The original ruleset is mined by topK approach with different values set by the *topks* option and with MinHC=0.01 and MinConf=0.1                                    |                                                        |
| runanytime        | Run mining with a refiniment timeout and with sampling. The result is compared with the result of the exhaustive approach without any sampling or anytime stopping criteria.                                                                                               |                                                        |

### Performed experiments

We performed some experiments on the [CESNET Metacentrum](https://www.metacentrum.cz/en/index.html) computing cluster with RDFRules 1.2.1 and the [sample YAGO dataset](./data/yago2core_facts.clean.notypes.tsv.bz2) containing around 1M triples with following input parameters:

```
# task1 - topK, patterns and confidence computing experiments with 8 cores
> runMain com.github.propi.rdfrules.experiments.RdfRulesExperiments -cores 8 -runtopk -runpatterns
```

For RDFRules 1.4.0, the experiments were extended by discretization, clustering, pruning, and multiple-graphs experiments.

```
# task2 - auto discretization of numerical values
> runMain com.github.propi.rdfrules.experiments.RdfRulesExperiments -cores 8 -rundiscretization -minhcs 0.01,0.02,0.05,0.1,0.2,0.3 -input "experiments/data/mappingbased_literals_sample.ttl.bz2"
# task3 - clustering by different settings with calculated inter/intra cluster similarities and final score
> runMain com.github.propi.rdfrules.experiments.RdfRulesExperiments -cores 8 -runclusters
# task4 - CBA pruning strategy for reducing number of rules without loss of predicted triples 
> runMain com.github.propi.rdfrules.experiments.RdfRulesExperiments -cores 8 -runpruning
# task5 - comparison between mining of rules from two different graphs and from merged graphs together
> runMain com.github.propi.rdfrules.experiments.RdfRulesExperiments -cores 8 -rungraphs
```

For RDFRules 1.6.1, the experiments were extended by the anytime approach.

```
# task6 - anytime approach vs exhaustive approach
> runMain com.github.propi.rdfrules.experiments.RdfRulesExperiments -cores 8 -runanytime -minhcs 0.01
```

The results of individual tasks are placed in the [results](./results) folder. The parameters of the used machine for experiments are:
- CPU: 4x 14-core Intel Xeon E7-4830 v4 (2GHz),
- RAM: 512 GB,
- OS: Debian 9.

All experiments can be launched with default settings with the *yago2core_facts.clean.notypes.tsv.bz2* dataset and with the *mappingbased_literals_sample.ttl.bz2* dataset for discretization by the main class *AllTests*:

```
> runMain com.github.propi.rdfrules.experiments.AllTests
```
