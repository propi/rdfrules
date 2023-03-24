# RDFRules vs AnyBURL

This module contains experiments with RDFRules and [AnyBURL](https://web.informatik.uni-mannheim.de/AnyBURL/).

## Getting Started

Clone the RDFRules repository and run following SBT commands:
```
> project experimentsKgc
```

## Benchmarks

The main class *RdfRulesKgc* requires some parameters to determine which kind of experiment should be launched:

```
> runMain com.github.propi.rdfrules.experiments.RdfRulesKgc parameters...
```

| *Parameter name* | *Description*                                                                                             | *Default*                    |
|------------------|-----------------------------------------------------------------------------------------------------------|------------------------------|
| cores            | Number of threads.                                                                                        | available cores              |
| dataset          | Input dataset (wn18rr, fb15k-237, yago3-10)                                                               | wn18rr                       |
| output           | Output file with benchmark results.                                                                       | experiments/data/results.txt |
| rlen             | Max rule length                                                                                           | 3                            |
| revalidate       | Revalidate all intermediate files                                                                         | false                        |
| runconfidences   | Run KGC task with different confidence measures (CWA, PCA, QPCA)                                          |                              |
| runmodes         | Run KGC with modes (most frequent items added into each prediction task)                                  |                              |
| runconstants     | Run KGC without constants                                                                                 |                              |
| runanytime       | Run KGC with anytime approach for rule refinement                                                         |                              |
| runscorers       | Run KGC with various scorers (NoisyOr, NonRedundantNoisyOr, NonRedundantMaximum)                          |                              |
| runpruning       | Run KGC with data coverage pruning                                                                        |                              |
| runanyburl       | Run AnyBURL algorithm for KGC. Input thresholds are same as for RDFRules. Max mining time is set to 1000s |                              |

### Performed experiments

Default mining parameters are: minSupport = 5, minHeadSize = 1, minConfidence = 0.1

We performed some experiments in order to solve a knowledge graph completion (KGC) problem on the [CESNET Metacentrum](https://www.metacentrum.cz/en/index.html) computing cluster with RDFRules 1.7.2 and [AnyBURL 22](https://web.informatik.uni-mannheim.de/AnyBURL/) with following input parameters:

```
# task1 - run and evaluate the KGC task for wn18rr dataset computed by RDFRules with different confidence types.
> runMain com.github.propi.rdfrules.experiments.RdfRulesKgc -cores 8 -runconfidences
# task2 - run and evaluate the KGC task for wn18rr dataset computed by RDFRules with the data coverage pruning strategy.
> runMain com.github.propi.rdfrules.experiments.RdfRulesKgc -cores 8 -runpruning
# task3 - run and evaluate the KGC task for wn18rr dataset computed by AnyBURL.
> runMain com.github.propi.rdfrules.experiments.RdfRulesKgc -cores 8 -runanyburl
```

The parameters of the used machine for experiments are:
- CPU: 4x 14-core Intel Xeon E7-4830 v4 (2GHz),
- RAM: 512 GB,
- OS: Debian 9.
