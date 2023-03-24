# RDFRules vs AMIE3

This module contains experiments with RDFRules and [AMIE3](https://github.com/dig-team/amie).

## Getting Started

Clone the RDFRules repository and run following SBT commands:
```
> project experimentsAmie3
```

## Benchmarks

The main class *Amie3Comparison* requires some parameters to determine which kind of experiment should be launched:

```
> runMain com.github.propi.rdfrules.experiments.Amie3Comparison parameters...
```

| *Parameter name* | *Description*                                                                                                                                 | *Default*                                              |
|------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------|
| cores            | Number of threads.                                                                                                                            | available cores                                        |
| minhcs           | List of values with min head coverage thresholds.                                                                                             | 0.005,0.01,0.02,0.05,0.1,0.2,0.3                       |
| times            | Number of repetition of each task                                                                                                             | 7                                                      |
| input            | Input TSV dataset. Dataset can be compressed by GZIP or BZIP2.                                                                                | experiments/data/yago2core_facts.clean.notypes.tsv.bz2 |
| output           | Output file with benchmark results.                                                                                                           | experiments/data/results.txt                           |
| hcs              | Mine constants at the lower cadinality side                                                                                                   |                                                        |
| runconstants     | Run rule mining with constants. Default confidence thresholds are: minConfidence = 0.1, minPcaConfidence = 0.1, numberOfThreads = $cores      |                                                        |
| runlogical       | Run rule mining only with variables. Default confidence thresholds are: minConfidence = 0.1, minPcaConfidence = 0.1, numberOfThreads = $cores |                                                        |
| rdfrulesonly     | Run only RDFRules                                                                                                                             |                                                        |
| amieonly         | Run only AMIE+                                                                                                                                |                                                        |

### Performed experiments

We performed some experiments on the [CESNET Metacentrum](https://www.metacentrum.cz/en/index.html) computing cluster with RDFRules 1.6.1 and the [sample YAGO dataset](./data/yago2core_facts.clean.notypes.tsv.bz2) containing around 1M triples with following input parameters:

```
# task1 - mining only with variables
> runMain com.github.propi.rdfrules.experiments.Amie3Comparison -cores 8 -runlogical -minhcs 0.2,0.1,0.05,0.02,0.01,0.005,0.003,0.001
# task2 - mining with constants
> runMain com.github.propi.rdfrules.experiments.Amie3Comparison -cores 8 -runconstants
```

Experiments with the dbpedia 3.8 dataset.

```
# task3 - mining rules without constants by AMIE3 for DBpedia 3.8
> runMain com.github.propi.rdfrules.experiments.Amie3Comparison -cores 8 -times 1 -input "experiments/data/dbpedia.3.8.tsv.bz2" -minhcs 0.01 -runlogical -amieonly
# task4 - mining rules without constants by RDFRules for DBpedia 3.8
> runMain com.github.propi.rdfrules.experiments.Amie3Comparison -cores 8 -times 1 -input "experiments/data/dbpedia.3.8.tsv.bz2" -minhcs 0.01 -runlogical -rdfrulesonly
# task4 - mining rules with constants at the lower cardinality side by RDFRules for DBpedia 3.8
> runMain com.github.propi.rdfrules.experiments.Amie3Comparison -cores 8 -times 1 -input "experiments/data/dbpedia.3.8.tsv.bz2" -minhcs 0.15 -runconstants -hcs
```

The parameters of the used machine for experiments are:
- CPU: 4x 14-core Intel Xeon E7-4830 v4 (2GHz),
- RAM: 512 GB,
- OS: Debian 9.
