# RdfRules: Experiments

This module has implemented some experiments with the RDFRules Scala Core API and Java API. There are 3 basic examples (as 3 main classes) and one main class for complex benchmark and comparison with the original AMIE+ implementation. The complex benchmark was launched on the CESNET Metacentrum computing cluster - results are also available within this GitHub repository. 

## Getting Started

Clone the RDFRules repository and run following SBT commands:
```sbt
> project experiments
> run
```

After execution of the *run* command we can choose from three basic (fast) examples and one complex benchmark:
- **YagoAndDbpediaSamples**: 5 samples with DBpedia and YAGO datasets. It operates with the Scala API.
- **CompleteWorkflowScala**: one example with the complete workflow of rule mining. It operates with the Scala API.
- **CompleteWorkflowJava**: the same example as in the CompleteWorkflowScala experiments, but it operates with the Java API.
- **OriginalAmieComparison**: complex bechmark with comparison to original AMIE+.

## Benchmark

The main class *OriginalAmieComparison* requires some parameters to determine which kind of experiment should be launched:

```sbt
> run-main com.github.propi.rdfrules.experiments.OriginalAmieComparison parameters...
```

| *Parameter name* | *Description* | *Default* |
|------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------|
| coresc | List of numbers of threads to be used | Nil |
| cores | If *coresc* is not specified, it performs experiments for number of threads from 1 to the *cores* value. | available cores |
| minhcs | List of values with min head coverage thresholds. | 0.005,0.01,0.02,0.05,0.1,0.2,0.3 |
| times | Number of repetition of each task | 7 |
| input | Input TSV dataset. Dataset can be compressed by GZIP or BZIP2. | experiments/data/yago2core_facts.clean.notypes.tsv.bz2 |
| output | Output file with benchmark results. | experiments/data/results.txt |
| runtopk | Run rule mining in the top-k mode. It contains *runconstants* and *runlogical* tasks with topK = 100. |  |
| runconstants | Run rule mining with constants. Default confidence thresholds are: minConfidence = 0.1, minPcaConfidence = 0.1, numberOfThreads = $cores |  |
| runlogical | Run rule mining only with variables. Default confidence thresholds are: minConfidence = 0.1, minPcaConfidence = 0.1, numberOfThreads = $cores |  |
| runcores | Run the experiment of scalability. Default thresholds are: minHeadCoverage = ${minhcs.head}, minConfidence = 0.1, minPcaConfidence = 0.1 |  |
| runpatterns | Run rule mining with patterns |  |
| runconfidence | Run confidence computing separately. The default threshold is minPcaConfidence=0.1, input 10000 rules with/without constants. |  |
| rdfrulesonly | Run only RDFRules |  |
| amieonly | Run only AMIE+ |  |

### Performed experiments

We performed some experiments on the CESNET Metacentrum computing cluster with following input parameters a results:

