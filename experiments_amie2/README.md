# RDFRules vs AMIE+

This module contains experiments with RDFRules and [AMIE+](https://www.mpi-inf.mpg.de/departments/databases-and-information-systems/research/yago-naga/amie).

## Getting Started

Clone the RDFRules repository and run following SBT commands:
```
> project experimentsAmie2
```

## Benchmarks

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
| runconstants      | Run rule mining with constants. Default confidence thresholds are: minConfidence = 0.1, minPcaConfidence = 0.1, numberOfThreads = $cores                                                                                                                                   |                                                        |
| runlogical        | Run rule mining only with variables. Default confidence thresholds are: minConfidence = 0.1, minPcaConfidence = 0.1, numberOfThreads = $cores                                                                                                                              |                                                        |
| runcores          | Run the experiment of scalability. Default thresholds are: minHeadCoverage = ${minhcs.head}, minConfidence = 0.1, minPcaConfidence = 0.1                                                                                                                                   |                                                        |
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
# task6 - rule mining only with variables with a low head coverage
> runMain com.github.propi.rdfrules.experiments.OriginalAmieComparison -cores 8 -runlogical -minhcs 0.2,0.1,0.05,0.02,0.01,0.005,0.003,0.001
```

For RDFRules 1.4.0, the experiments were extended by dbpedia (~11M triples), discretization, clustering, pruning, and multiple-graphs experiments.

```
# task7 - mining rules without constants by AMIE+ for DBpedia 3.8
> runMain com.github.propi.rdfrules.experiments.OriginalAmieComparison -cores 8 -times 1 -input "experiments/data/dbpedia.3.8.tsv.bz2" -minhcs 0.01 -runlogical -amieonly
# task8 - mining rules without constants by RDFRules for DBpedia 3.8
> runMain com.github.propi.rdfrules.experiments.OriginalAmieComparison -cores 8 -times 1 -input "experiments/data/dbpedia.3.8.tsv.bz2" -minhcs 0.01 -runlogical -rdfrulesonly
```

The results of individual tasks are placed in the [results](./results) folder. The parameters of the used machine for experiments are:
- CPU: 4x 14-core Intel Xeon E7-4830 v4 (2GHz),
- RAM: 512 GB,
- OS: Debian 9.
