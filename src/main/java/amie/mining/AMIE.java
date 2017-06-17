/**
 * @author lgalarra
 * @date Aug 8, 2012 AMIE Version 0.1
 */
package amie.mining;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javatools.administrative.Announce;
import javatools.datatypes.ByteString;
import javatools.datatypes.MultiMap;
import javatools.parsers.NumberFormatter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import amie.data.KB;
import amie.mining.assistant.DefaultMiningAssistant;
import amie.mining.assistant.MiningAssistant;
import amie.mining.assistant.RelationSignatureDefaultMiningAssistant;
import amie.rules.QueryEquivalenceChecker;
import amie.rules.Rule;

/**
 * Main class that implements the AMIE algorithm for rule mining 
 * on ontologies. The ontology must be provided as a list of TSV files
 * where each line has the format SUBJECT&lt;TAB&gt;RELATION&lt;TAB&gt;OBJECT.
 * 
 * @author lgalarra
 *
 */
public class AMIE {

    /**
     * Default PCA confidence threshold
     */
    private static final double DEFAULT_PCA_CONFIDENCE = 0.0;

    /**
     * Default Head coverage threshold
     *
     */
    private static final double DEFAULT_HEAD_COVERAGE = 0.01;

    /**
     * The default minimum size for a relation to be used as a head relation
     */
    private static final int DEFAULT_INITIAL_SUPPORT = 100;

    /**
     * The default support threshold
     */
    private static final int DEFAULT_SUPPORT = 100;

    /**
     * It implements all the operators defined for the mining process: ADD-EDGE,
     * INSTANTIATION, SPECIALIZATION and CLOSE-CIRCLE
     */
    private MiningAssistant assistant;

    /**
     * Support threshold for relations.
     */
    private double minInitialSupport;

    /**
     * Head coverage threshold for refinements
     */
    private double minHeadCoverage;

    /**
     * Metric used to prune the mining tree
     */
    private Metric pruningMetric;

    /**
     * Preferred number of threads
     */
    private int nThreads;
    
    /**
     * If true, print the rules as they are discovered.
     */
    private boolean realTime;

    /** Fields required to measure the system performance **/
    
    /**
     * Time spent in applying the operators.
     *
     */
    private long _specializationTime;

    /**
     * Time spent in calculating confidence scores.
     *
     */
    private long _scoringTime;
    
    /**
     * List of target head relations.
     */
    private Collection<ByteString> seeds;

    /**
     * Time spent in duplication elimination
     *
     */
    private long _queueingAndDuplicateElimination;

    /**
     * Time spent in confidence approximations
     */
    private long _approximationTime;
    
    /**
     * Time spent loading the KB into memory.
     */
    private long _sourcesLoadingTime;

    /**
     * Rules of the form A ^ B ^ C => D are output if their immediate parents,
     * e.g., A ^ B => D and A ^ C => D and A ^ C => D have higher confidence. If
     * this option is enabled the check is extended also to parents of degree 2,
     * e.g., A => D B => D, C => D.
     *
     */
    private boolean _checkParentsOfDegree2;

    /**
     *
     * @param assistant An object that implements the logic of the mining operators.
     * @param minInitialSupport If head coverage is defined as pruning metric,
     * it is the minimum size for a relation to be considered in the mining.
     * @param threshold The minimum support threshold: it can be either a head
     * coverage ratio threshold or an absolute number.
     * @param metric Head coverage or support.
     */
    public AMIE(MiningAssistant assistant, int minInitialSupport, double threshold, Metric metric, int nThreads) {
        this.assistant = assistant;
        this.minInitialSupport = minInitialSupport;
        this.minHeadCoverage = threshold;
        this.pruningMetric = metric;
        this.nThreads = nThreads;
        this.realTime = true;
        this.seeds = null;
        /** System performance **/
        this._specializationTime = 0l;
        this._scoringTime = 0l;
        this._queueingAndDuplicateElimination = 0l;
        this._approximationTime = 0l;
    }

    public MiningAssistant getAssistant() {
        return assistant;
    }
    
	public boolean isVerbose() {
		// TODO Auto-generated method stub
		return assistant.isVerbose();
	}
    
    public boolean isRealTime() {
    	return realTime;
    }
    
    public void setRealTime(boolean realTime) {
    	this.realTime = realTime;
    }
    
    public Collection<ByteString> getSeeds() {
    	return seeds;
    }
    
    public void setSeeds(Collection<ByteString> seeds) {
    	this.seeds = seeds;
    }

    public Metric getPruningMetric() {
        return pruningMetric;
    }

    public void setPruningMetric(Metric pruningMetric) {
        this.pruningMetric = pruningMetric;
    }

    private long _getSpecializationTime() {
        return _specializationTime;
    }

    private long _getScoringTime() {
        return _scoringTime;
    }

    private long _getQueueingAndDuplicateElimination() {
        return _queueingAndDuplicateElimination;
    }

    private long _getApproximationTime() {
        return _approximationTime;
    }
    
    
    private void _setSourcesLoadingTime(long sourcesLoadingTime) {
    	this._sourcesLoadingTime = sourcesLoadingTime;
	}
    
    
    private long _getSourcesLoadingTime() {
    	return this._sourcesLoadingTime;
	}
    

    /**
     * The key method which returns a set of rules mined from the KB based on 
     * the AMIE object's configuration.
     *
     * @return
     * @throws Exception
     */
    public List<Rule> mine() throws Exception {
        List<Rule> result = new ArrayList<>();
        MultiMap<Integer, Rule> indexedResult = new MultiMap<>();
        RuleConsumer consumerObj = null;
        Thread consumerThread = null;
        Lock resultsLock = new ReentrantLock();
        Condition resultsCondVar = resultsLock.newCondition();
        AtomicInteger sharedCounter = new AtomicInteger(nThreads);

        Collection<Rule> seedsPool = new LinkedHashSet<>();
        // Queue initialization
        if (seeds == null || seeds.isEmpty()) {
            assistant.getInitialAtoms(minInitialSupport, seedsPool);
        } else {
            assistant.getInitialAtomsFromSeeds(seeds, minInitialSupport, seedsPool);
        }

        if (realTime) {
            consumerObj = new RuleConsumer(result, resultsLock, resultsCondVar);
            consumerThread = new Thread(consumerObj);
            consumerThread.start();
        }

        System.out.println("Using " + nThreads + " threads");
        //Create as many threads as available cores
        ArrayList<Thread> currentJobs = new ArrayList<>();
        ArrayList<RDFMinerJob> jobObjects = new ArrayList<>();
        for (int i = 0; i < nThreads; ++i) {
            RDFMinerJob jobObject = new RDFMinerJob(seedsPool, result, resultsLock, resultsCondVar, sharedCounter, indexedResult);
            Thread job = new Thread(jobObject);
            currentJobs.add(job);
            jobObjects.add(jobObject);

        }

        for (Thread job : currentJobs) {
            job.start();
        }

        for (Thread job : currentJobs) {
            job.join();
        }

        // Required by the reviewers of the AMIE+ article. Timing collection   	
        for (RDFMinerJob jobObject : jobObjects) {
            this._specializationTime += jobObject._getSpecializationTime();
            this._scoringTime += jobObject._getScoringTime();
            this._queueingAndDuplicateElimination += jobObject._getQueueingAndDuplicateElimination();
            this._approximationTime += jobObject._getApproximationTime();
        }

        if (realTime) {
            consumerObj.finish();
            consumerThread.interrupt();
        }

        return result;
    }

    /**
     * It removes and prints rules from a shared list (a list accessed by
     * multiple threads).
     *
     * @author galarrag
     *
     */
    private class RuleConsumer implements Runnable {

        private List<Rule> consumeList;

        private volatile boolean finished;

        private int lastConsumedIndex;

        private Lock consumeLock;

        private Condition conditionVariable;

        public RuleConsumer(List<Rule> consumeList, Lock consumeLock, Condition conditionVariable) {
            this.consumeList = consumeList;
            this.lastConsumedIndex = -1;
            this.consumeLock = consumeLock;
            this.conditionVariable = conditionVariable;
            finished = false;
        }

        @Override
        public void run() {
            Rule.printRuleHeaders();
            while (!finished) {
                consumeLock.lock();
                try {
                    while (lastConsumedIndex == consumeList.size() - 1) {
                        conditionVariable.await();
                        for (int i = lastConsumedIndex + 1; i < consumeList.size(); ++i) {
                            System.out.println(consumeList.get(i).getFullRuleString());
                        }

                        lastConsumedIndex = consumeList.size() - 1;
                    }
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                } finally {
                    consumeLock.unlock();
                }
            }
        }

        public void finish() {
            finished = true;
        }
    }

    /**
     * This class implements the AMIE algorithm in a single thread.
     *
     * @author lgalarra
     */
    private class RDFMinerJob implements Runnable {

        private List<Rule> outputSet;

        // A version of the output set thought for search.
        private MultiMap<Integer, Rule> indexedOutputSet;

        private Collection<Rule> queryPool;

        private Lock resultsLock;

        private Condition resultsCondition;

        private AtomicInteger sharedCounter;

        private boolean idle;

        private Boolean done;

        /** System performance evaluation **/
        
        // Time spent in applying the operators.
        private long _specializationTime;

        // Time spent in calculating confidence scores.
        private long _scoringTime;

        // Time spent in duplication elimination
        private long _queueingAndDuplicateElimination;

        // Time to calculate confidence approximations
        private long _approximationTime;

        /**
         * 
         * @param seedsPool
         * @param outputSet
         * @param resultsLock Lock associated to the output buffer were mined rules are added
         * @param resultsCondition Condition variable associated to the results lock
         * @param sharedCounter Reference to a shared counter that keeps track of the number of threads that are running
         * in the system.
         * @param indexedOutputSet
         */
        public RDFMinerJob(Collection<Rule> seedsPool,
                List<Rule> outputSet, Lock resultsLock,
                Condition resultsCondition,
                AtomicInteger sharedCounter,
                MultiMap<Integer, Rule> indexedOutputSet) {
            this.queryPool = seedsPool;
            this.outputSet = outputSet;
            this.resultsLock = resultsLock;
            this.resultsCondition = resultsCondition;
            this.sharedCounter = sharedCounter;
            this.indexedOutputSet = indexedOutputSet;
            this.idle = false;
            this.done = false;
            this._specializationTime = 0l;
            this._scoringTime = 0l;
            this._queueingAndDuplicateElimination = 0l;
            this._approximationTime = 0l;
        }

        /**
         * Removes the front of the queue of rules.
         * @return
         */
        private Rule pollQuery() {
            long timeStamp1 = System.currentTimeMillis();
            Rule nextQuery = null;
            if (!queryPool.isEmpty()) {
                Iterator<Rule> iterator = queryPool.iterator();
                nextQuery = iterator.next();
                iterator.remove();
            }
            long timeStamp2 = System.currentTimeMillis();
            this._queueingAndDuplicateElimination += (timeStamp2 - timeStamp1);
            return nextQuery;
        }

        @Override
        public void run() {
            synchronized (this.done) {
                this.done = false;
            }
            while (true) {
                Rule currentRule = null;

                synchronized (queryPool) {
                    currentRule = pollQuery();
                }

                if (currentRule != null) {
                  //  System.out.println("Dequeued:"+currentRule);
                    if (this.idle) {
                        this.idle = false;
                        this.sharedCounter.decrementAndGet();
                    }

                    // Check if the rule meets the language bias and confidence thresholds and
                    // decide whether to output it.
                    boolean outputRule = false;
                    if (currentRule.isClosed()) {
                        long timeStamp1 = System.currentTimeMillis();
                        boolean ruleSatisfiesConfidenceBounds
                                = assistant.calculateConfidenceBoundsAndApproximations(currentRule);
                        this._approximationTime += (System.currentTimeMillis() - timeStamp1);
                        if (ruleSatisfiesConfidenceBounds) {
                            this.resultsLock.lock();
                            setAdditionalParents2(currentRule);
                            this.resultsLock.unlock();
                            // Calculate the metrics
                            assistant.calculateConfidenceMetrics(currentRule);
                            // Check the confidence threshold and skyline technique.
                            outputRule = assistant.testConfidenceThresholds(currentRule);
                        } else {
                            outputRule = false;
                        }
                        long timeStamp2 = System.currentTimeMillis();
                        this._scoringTime += (timeStamp2 - timeStamp1);
                    }

                    // Check if we should further refine the rule
                    boolean furtherRefined = true;
                    if (assistant.isEnablePerfectRules()) {
                        furtherRefined = !currentRule.isPerfect();
                    }

                    // If so specialize it
                    if (furtherRefined) {
                        long timeStamp1 = System.currentTimeMillis();
                        double threshold = getCountThreshold(currentRule);
                        List<Rule> temporalOutput = new ArrayList<Rule>();
                        List<Rule> temporalOutputDanglingEdges = new ArrayList<Rule>();

                        // Application of the mining operators
                        assistant.getClosingAtoms(currentRule, threshold, temporalOutput);
                        assistant.getDanglingAtoms(currentRule, threshold, temporalOutputDanglingEdges);
                        assistant.getInstantiatedAtoms(currentRule, threshold, temporalOutputDanglingEdges, temporalOutput);
                        
                        long timeStamp2 = System.currentTimeMillis();
                        this._specializationTime += (timeStamp2 - timeStamp1);
                        // Addition of the specializations to the queue
                        synchronized (queryPool) {
                            timeStamp1 = System.currentTimeMillis();
                            queryPool.addAll(temporalOutput);
                            // This part of the code, check please!
                            if (currentRule.getRealLength() < assistant.getMaxDepth() - 1) {
                                queryPool.addAll(temporalOutputDanglingEdges);
                            }

                            timeStamp2 = System.currentTimeMillis();
                            this._queueingAndDuplicateElimination += (timeStamp2 - timeStamp1);
                        }
                    }

                    // Output the rule
                    if (outputRule) {
                        this.resultsLock.lock();
                        long timeStamp1 = System.currentTimeMillis();
                        Set<Rule> outputQueries = indexedOutputSet.get(currentRule.alternativeParentHashCode());
                        if (outputQueries != null) {
                            if (!outputQueries.contains(currentRule)) {
                                this.outputSet.add(currentRule);
                                outputQueries.add(currentRule);
                            }
                        } else {
                            this.outputSet.add(currentRule);
                            this.indexedOutputSet.put(currentRule.alternativeParentHashCode(), currentRule);
                        }
                        long timeStamp2 = System.currentTimeMillis();
                        this._queueingAndDuplicateElimination += (timeStamp2 - timeStamp1);
                        this.resultsCondition.signal();
                        this.resultsLock.unlock();
                    }
                } else {
                    if (!this.idle) {
                        this.idle = true;
                        boolean leave;
                        synchronized (this.sharedCounter) {
                            leave = this.sharedCounter.decrementAndGet() <= 0;
                        }
                        if (leave) {
                            break;
                        }
                    } else {
                        if (this.sharedCounter.get() <= 0) {
                            break;
                        }
                    }
                }
            }

            synchronized (this.done) {
                this.done = true;
            }
        }

        /**
         * It finds all potential parents of a rule in the output set of indexed
         * rules. A rule X => Y is a parent of rule X'=> Y' if Y = Y' and
         * X is a subset of X' (in other words X' => Y' is a more specific version
         * of X => Y)
         *
         * @param currentQuery
         */
        private void setAdditionalParents2(Rule currentQuery) {
            int parentHashCode = currentQuery.alternativeParentHashCode();
            Set<Rule> candidateParents = indexedOutputSet.get(parentHashCode);
            if (candidateParents != null) {
                List<ByteString[]> queryPattern = currentQuery.getRealTriples();
                // No need to look for parents of rules of size 2
                if (queryPattern.size() <= 2) {
                    return;
                }
                List<List<ByteString[]>> parentsOfSizeI = new ArrayList<>();
                Rule.getParentsOfSize(queryPattern.subList(1, queryPattern.size()), 
                		queryPattern.get(0), queryPattern.size() - 2, parentsOfSizeI);
                if (_checkParentsOfDegree2) {
                    if (queryPattern.size() > 3) {
                        Rule.getParentsOfSize(queryPattern.subList(1, queryPattern.size()), 
                        		queryPattern.get(0), queryPattern.size() - 3, parentsOfSizeI);
                    }
                }
                for (List<ByteString[]> parent : parentsOfSizeI) {
                    for (Rule candidate : candidateParents) {
                        List<ByteString[]> candidateParentPattern = candidate.getRealTriples();
                        if (QueryEquivalenceChecker.areEquivalent(parent, candidateParentPattern)) {
                            currentQuery.setParent(candidate);
                        }
                    }
                }
            }
        }

        /**
         * Based on AMIE's configuration, it returns the absolute support threshold
         * that should be applied to the rule.
         * @param query
         * @return
         */
        private double getCountThreshold(Rule query) {
            switch (pruningMetric) {
                case Support:
                    return minHeadCoverage;
                case HeadCoverage:
                    return Math.ceil((minHeadCoverage * 
                    		(double) assistant.getHeadCardinality(query)));
                default:
                    return 0;
            }
        }

        public long _getSpecializationTime() {
            return _specializationTime;
        }

        public long _getScoringTime() {
            return _scoringTime;
        }

        public long _getQueueingAndDuplicateElimination() {
            return _queueingAndDuplicateElimination;
        }

        public long _getApproximationTime() {
            return _approximationTime;
        }

    }

    public void setCheckParentsOfDegree2(boolean booleanVal) {
        this._checkParentsOfDegree2 = booleanVal;
    }

    /**
     * Returns an instance of AMIE that mines rules on the given KB using
     * the vanilla setting of head coverage 1% and no confidence threshold.
     * @param db
     * @return
     */
    public static AMIE getVanillaSettingInstance(KB db) {
        return new AMIE(new DefaultMiningAssistant(db),
                100, // Do not look at relations smaller than 100 facts 
                0.01, // Head coverage 1%
                Metric.HeadCoverage,
                Runtime.getRuntime().availableProcessors());
    }
    
    /** Factory methods. They return canned instances of AMIE. **/ 

    /**
     * Returns an instance of AMIE that mines rules on the given KB using
     * the vanilla setting of head coverage 1% and a given PCA confidence threshold
     * @param db
     * @return
     */
    public static AMIE getVanillaSettingInstance(KB db, double minPCAConfidence) {
        DefaultMiningAssistant miningAssistant = new DefaultMiningAssistant(db);
        miningAssistant.setPcaConfidenceThreshold(minPCAConfidence);
        return new AMIE(miningAssistant,
                100, // Do not look at relations smaller than 100 facts 
                0.01, // Head coverage 1%
                Metric.HeadCoverage,
                Runtime.getRuntime().availableProcessors());
    }

    /**
     * Returns an (vanilla setting) instance of AMIE that enables the lossy optimizations, i.e., optimizations that
     * optimize for runtime but that could in principle omit some rules that should be mined.
     * @param db
     * @param minPCAConfidence
     * @param startSupport
     * @return
     */
    public static AMIE getLossyVanillaSettingInstance(KB db, double minPCAConfidence, int startSupport) {
        DefaultMiningAssistant miningAssistant = new DefaultMiningAssistant(db);
        miningAssistant.setPcaConfidenceThreshold(minPCAConfidence);
        miningAssistant.setEnabledConfidenceUpperBounds(true);
        miningAssistant.setEnabledFunctionalityHeuristic(true);
        return new AMIE(miningAssistant,
                startSupport, // Do not look at relations smaller than 100 facts 
                DEFAULT_HEAD_COVERAGE, // Head coverage 1%
                Metric.HeadCoverage,
                Runtime.getRuntime().availableProcessors());
    }

    /**
     * Returns an instance of AMIE that enables the lossy optimizations, i.e., optimizations that
     * optimize for runtime but that could in principle omit some rules that should be mined.
     * @param db
     * @param minPCAConfidence
     * @param minSupport
     * @return
     */
    public static AMIE getLossyInstance(KB db, double minPCAConfidence, int minSupport) {
        DefaultMiningAssistant miningAssistant = new DefaultMiningAssistant(db);
        miningAssistant.setPcaConfidenceThreshold(minPCAConfidence);
        miningAssistant.setEnabledConfidenceUpperBounds(true);
        miningAssistant.setEnabledFunctionalityHeuristic(true);
        return new AMIE(miningAssistant,
                minSupport, // Do not look at relations smaller than the support threshold 
                minSupport, // Head coverage 1%
                Metric.Support,
                Runtime.getRuntime().availableProcessors());
    }
    
    /**
     * Gets an instance of AMIE configured according to the command line arguments.
     * @param args
     * @return
     * @throws IOException
     * @throws InvocationTargetException 
     * @throws IllegalArgumentException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    public static AMIE getInstance(String[] args) 
    		throws IOException, InstantiationException, 
    		IllegalAccessException, IllegalArgumentException, 
    		InvocationTargetException {
        List<File> dataFiles = new ArrayList<File>();
        List<File> targetFiles = new ArrayList<File>();
        List<File> schemaFiles = new ArrayList<File>();

        CommandLine cli = null;
        double minStdConf = 0.0;
        double minPCAConf = DEFAULT_PCA_CONFIDENCE;
        int minSup = DEFAULT_SUPPORT;
        int minInitialSup = DEFAULT_INITIAL_SUPPORT;
        double minHeadCover = DEFAULT_HEAD_COVERAGE;
        int maxDepth = 3;
        int recursivityLimit = 3;
        boolean realTime = true;
        boolean countAlwaysOnSubject = false;
        double minMetricValue = 0.0;
        boolean allowConstants = false;
        boolean enableConfidenceUpperBounds = true;
        boolean enableFunctionalityHeuristic = true;
        boolean verbose = false;
        boolean enforceConstants = false;
        boolean avoidUnboundTypeAtoms = true;
        /** System performance measure **/
        boolean exploitMaxLengthForRuntime = true;
        boolean enableQueryRewriting = true;
        boolean enablePerfectRulesPruning = true;
        long sourcesLoadingTime = 0l;
        /*********************************/
        int nProcessors = Runtime.getRuntime().availableProcessors();
        String bias = "default"; // Counting support on the two head variables.
        Metric metric = Metric.HeadCoverage; // Metric used to prune the search space.
        MiningAssistant mineAssistant = null;
        Collection<ByteString> bodyExcludedRelations = null;
        Collection<ByteString> headExcludedRelations = null;
        Collection<ByteString> headTargetRelations = null;
        Collection<ByteString> bodyTargetRelations = null;
        KB targetSource = null;
        KB schemaSource = null;
        String miningTechniqueStr = "standard";
        int nThreads = nProcessors; // By default use as many threads as processors.
        HelpFormatter formatter = new HelpFormatter();

        // create the command line parser
        CommandLineParser parser = new PosixParser();
        // create the Options
        Options options = new Options();

        Option supportOpt = OptionBuilder.withArgName("min-support")
                .hasArg()
                .withDescription("Minimum absolute support. Default: 100 positive examples")
                .create("mins");

        Option initialSupportOpt = OptionBuilder.withArgName("min-initial-support")
                .hasArg()
                .withDescription("Minimum size of the relations to be considered as head relations. "
                		+ "Default: 100 (facts or entities depending on the bias)")
                .create("minis");

        Option headCoverageOpt = OptionBuilder.withArgName("min-head-coverage")
                .hasArg()
                .withDescription("Minimum head coverage. Default: 0.01")
                .create("minhc");

        Option pruningMetricOpt = OptionBuilder.withArgName("pruning-metric")
                .hasArg()
                .withDescription("Metric used for pruning of intermediate queries: "
                		+ "support|headcoverage. Default: headcoverage")
                .create("pm");

        Option realTimeOpt = OptionBuilder.withArgName("output-at-end")
                .withDescription("Print the rules at the end and not while they are discovered. "
                		+ "Default: false")
                .create("oute");

        Option bodyExcludedOpt = OptionBuilder.withArgName("body-excluded-relations")
                .hasArg()
                .withDescription("Do not use these relations as atoms in the body of rules."
                		+ " Example: <livesIn>,<bornIn>")
                .create("bexr");

        Option headExcludedOpt = OptionBuilder.withArgName("head-excluded-relations")
                .hasArg()
                .withDescription("Do not use these relations as atoms in the head of rules "
                		+ "(incompatible with head-target-relations). Example: <livesIn>,<bornIn>")
                .create("hexr");

        Option headTargetRelationsOpt = OptionBuilder.withArgName("head-target-relations")
                .hasArg()
                .withDescription("Mine only rules with these relations in the head. "
                		+ "Provide a list of relation names separated by commas "
                		+ "(incompatible with head-excluded-relations). "
                		+ "Example: <livesIn>,<bornIn>")
                .create("htr");

        Option bodyTargetRelationsOpt = OptionBuilder.withArgName("body-target-relations")
                .hasArg()
                .withDescription("Allow only these relations in the body. Provide a list of relation "
                		+ "names separated by commas (incompatible with body-excluded-relations). "
                		+ "Example: <livesIn>,<bornIn>")
                .create("btr");

        Option maxDepthOpt = OptionBuilder.withArgName("max-depth")
                .hasArg()
                .withDescription("Maximum number of atoms in the antecedent and succedent of rules. "
                		+ "Default: 3")
                .create("maxad");

        Option pcaConfThresholdOpt = OptionBuilder.withArgName("min-pca-confidence")
                .hasArg()
                .withDescription("Minimum PCA confidence threshold. "
                		+ "This value is not used for pruning, only for filtering of the results. "
                		+ "Default: 0.0")
                .create("minpca");

        Option allowConstantsOpt = OptionBuilder.withArgName("allow-constants")
                .withDescription("Enable rules with constants. Default: false")
                .create("const");

        Option enforceConstantsOpt = OptionBuilder.withArgName("only-constants")
                .withDescription("Enforce constants in all atoms. Default: false")
                .create("fconst");

        Option assistantOp = OptionBuilder.withArgName("e-name")
                .hasArg()
                .withDescription("Syntatic/semantic bias: oneVar|default|[Path to a subclass of amie.mining.assistant.MiningAssistant]"
                		+ "Default: default (defines support and confidence in terms of 2 head variables)")
                .create("bias");

        Option countOnSubjectOpt = OptionBuilder.withArgName("count-always-on-subject")
                .withDescription("If a single variable bias is used (oneVar), "
                		+ "force to count support always on the subject position.")
                .create("caos");

        Option coresOp = OptionBuilder.withArgName("n-threads")
                .hasArg()
                .withDescription("Preferred number of cores. Round down to the actual number of cores "
                		+ "in the system if a higher value is provided.")
                .create("nc");

        Option stdConfThresholdOpt = OptionBuilder.withArgName("min-std-confidence")
                .hasArg()
                .withDescription("Minimum standard confidence threshold. "
                		+ "This value is not used for pruning, only for filtering of the results. Default: 0.0")
                .create("minc");

        Option confidenceBoundsOp = OptionBuilder.withArgName("optim-confidence-bounds")
                .withDescription("Enable the calculation of confidence upper bounds to prune rules.")
                .create("optimcb");

        Option funcHeuristicOp = OptionBuilder.withArgName("optim-func-heuristic")
                .withDescription("Enable functionality heuristic to identify potential low confident rules for pruning.")
                .create("optimfh");

        Option verboseOp = OptionBuilder.withArgName("verbose")
                .withDescription("Maximal verbosity")
                .create("verbose");

        Option recursivityLimitOpt = OptionBuilder.withArgName("recursivity-limit")
                .withDescription("Recursivity limit")
                .hasArg()
                .create("rl");

        Option avoidUnboundTypeAtomsOpt = OptionBuilder.withArgName("avoid-unbound-type-atoms")
                .withDescription("Avoid unbound type atoms, e.g., type(x, y), i.e., bind always 'y' to a type")
                .create("auta");

        Option doNotExploitMaxLengthOp = OptionBuilder.withArgName("do-not-exploit-max-length")
                .withDescription("Do not exploit max length for speedup "
                		+ "(requested by the reviewers of AMIE+). False by default.")
                .create("deml");

        Option disableQueryRewriteOp = OptionBuilder.withArgName("disable-query-rewriting")
                .withDescription("Disable query rewriting and caching.")
                .create("dqrw");

        Option disablePerfectRulesOp = OptionBuilder.withArgName("disable-perfect-rules")
                .withDescription("Disable perfect rules.")
                .create("dpr");

        Option onlyOutputEnhancementOp = OptionBuilder.withArgName("only-output")
                .withDescription("If enabled, it activates only the output enhacements, that is, "
                		+ "the confidence approximation and upper bounds. "
                        + " It overrides any other configuration that is incompatible.")
                .create("oout");

        Option fullOp = OptionBuilder.withArgName("full")
                .withDescription("It enables all enhancements: "
                		+ "lossless heuristics and confidence approximation and upper bounds"
                        + " It overrides any other configuration that is incompatible.")
                .create("full");

        Option extraFileOp = OptionBuilder.withArgName("extraFile")
                .withDescription("An additional text file whose interpretation depends "
                		+ "on the selected mining assistant (bias)")
                .hasArg()
                .create("ef");

        Option miningTechniqueOp = OptionBuilder.withArgName("mining-technique")
                .withDescription("AMIE offers 2 multi-threading strategies: "
                		+ "standard (traditional) and solidary (experimental)")
                .hasArg()
                .create("mt");

        options.addOption(stdConfThresholdOpt);
        options.addOption(supportOpt);
        options.addOption(initialSupportOpt);
        options.addOption(headCoverageOpt);
        options.addOption(pruningMetricOpt);
        options.addOption(realTimeOpt);
        options.addOption(bodyExcludedOpt);
        options.addOption(headExcludedOpt);
        options.addOption(maxDepthOpt);
        options.addOption(pcaConfThresholdOpt);
        options.addOption(headTargetRelationsOpt);
        options.addOption(bodyTargetRelationsOpt);
        options.addOption(allowConstantsOpt);
        options.addOption(enforceConstantsOpt);
        options.addOption(countOnSubjectOpt);
        options.addOption(assistantOp);
        options.addOption(coresOp);
        options.addOption(confidenceBoundsOp);
        options.addOption(verboseOp);
        options.addOption(funcHeuristicOp);
        options.addOption(recursivityLimitOpt);
        options.addOption(avoidUnboundTypeAtomsOpt);
        options.addOption(doNotExploitMaxLengthOp);
        options.addOption(disableQueryRewriteOp);
        options.addOption(disablePerfectRulesOp);
        options.addOption(onlyOutputEnhancementOp);
        options.addOption(fullOp);
        options.addOption(extraFileOp);
        options.addOption(miningTechniqueOp);

        try {
            cli = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println("Unexpected exception: " + e.getMessage());
            formatter.printHelp("AMIE [OPTIONS] <TSV FILES>", options);
            System.exit(1);
        }

        // These configurations override any other option
        boolean onlyOutput = cli.hasOption("oout");
        boolean full = cli.hasOption("full");
        if (onlyOutput && full) {
            System.err.println("The options only-output and full are incompatible. Pick either one.");
            formatter.printHelp("AMIE [OPTIONS] <TSV FILES>", options);
            System.exit(1);
        }

        if (cli.hasOption("htr") && cli.hasOption("hexr")) {
            System.err.println("The options head-target-relations and head-excluded-relations cannot appear at the same time");
            System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
            System.exit(1);
        }

        if (cli.hasOption("btr") && cli.hasOption("bexr")) {
            System.err.println("The options body-target-relations and body-excluded-relations cannot appear at the same time");
            formatter.printHelp("AMIE+", options);
            System.exit(1);
        }

        if (cli.hasOption("mins")) {
            String minSupportStr = cli.getOptionValue("mins");
            try {
                minSup = Integer.parseInt(minSupportStr);
            } catch (NumberFormatException e) {
                System.err.println("The option -mins (support threshold) requires an integer as argument");
                System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
                formatter.printHelp("AMIE+", options);
                System.exit(1);
            }
        }

        if (cli.hasOption("minis")) {
            String minInitialSupportStr = cli.getOptionValue("minis");
            try {
                minInitialSup = Integer.parseInt(minInitialSupportStr);
            } catch (NumberFormatException e) {
                System.err.println("The option -minis (initial support threshold) requires an integer as argument");
                System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
                formatter.printHelp("AMIE+", options);
                System.exit(1);
            }
        }

        if (cli.hasOption("minhc")) {
            String minHeadCoverage = cli.getOptionValue("minhc");
            try {
                minHeadCover = Double.parseDouble(minHeadCoverage);
            } catch (NumberFormatException e) {
                System.err.println("The option -minhc (head coverage threshold) requires a real number as argument");
                System.err.println("AMIE [OPTIONS] <.tsv INPUT FILES>");
                System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
                System.exit(1);
            }
        }

        if (cli.hasOption("minc")) {
            String minConfidenceStr = cli.getOptionValue("minc");
            try {
                minStdConf = Double.parseDouble(minConfidenceStr);
            } catch (NumberFormatException e) {
                System.err.println("The option -minc (confidence threshold) requires a real number as argument");
                System.err.println("AMIE [OPTIONS] <.tsv INPUT FILES>");
                System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
                System.exit(1);
            }
        }

        if (cli.hasOption("minpca")) {
            String minicStr = cli.getOptionValue("minpca");
            try {
                minPCAConf = Double.parseDouble(minicStr);
            } catch (NumberFormatException e) {
                System.err.println("The argument for option -minpca (PCA confidence threshold) must be an integer greater than 2");
                System.err.println("AMIE [OPTIONS] <.tsv INPUT FILES>");
                System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
                System.exit(1);
            }
        }

        if (cli.hasOption("bexr")) {
            bodyExcludedRelations = new ArrayList<>();
            String excludedValuesStr = cli.getOptionValue("bexr");
            String[] excludedValueArr = excludedValuesStr.split(",");
            for (String excludedValue : excludedValueArr) {
                bodyExcludedRelations.add(ByteString.of(excludedValue.trim()));
            }
        }

        if (cli.hasOption("btr")) {
            bodyTargetRelations = new ArrayList<>();
            String targetBodyValuesStr = cli.getOptionValue("btr");
            String[] bodyTargetRelationsArr = targetBodyValuesStr.split(",");
            for (String targetString : bodyTargetRelationsArr) {
                bodyTargetRelations.add(ByteString.of(targetString.trim()));
            }
        }

        if (cli.hasOption("htr")) {
            headTargetRelations = new ArrayList<>();
            String targetValuesStr = cli.getOptionValue("htr");
            String[] targetValueArr = targetValuesStr.split(",");
            for (String targetValue : targetValueArr) {
                headTargetRelations.add(ByteString.of(targetValue.trim()));
            }
        }

        if (cli.hasOption("hexr")) {
            headExcludedRelations = new ArrayList<>();
            String excludedValuesStr = cli.getOptionValue("hexr");
            String[] excludedValueArr = excludedValuesStr.split(",");
            for (String excludedValue : excludedValueArr) {
                headExcludedRelations.add(ByteString.of(excludedValue.trim()));
            }
        }

        if (cli.hasOption("maxad")) {
            String maxDepthStr = cli.getOptionValue("maxad");
            try {
                maxDepth = Integer.parseInt(maxDepthStr);
            } catch (NumberFormatException e) {
                System.err.println("The argument for option -maxad (maximum depth) must be an integer greater than 2");
                System.err.println("AMIE [OPTIONS] <.tsv INPUT FILES>");
                formatter.printHelp("AMIE+", options);
                System.exit(1);
            }

            if (maxDepth < 2) {
                System.err.println("The argument for option -maxad (maximum depth) must be greater or equal than 2");
                System.err.println("AMIE [OPTIONS] <.tsv INPUT FILES>");
                formatter.printHelp("AMIE+", options);
                System.exit(1);
            }
        }

        if (cli.hasOption("nc")) {
            String nCoresStr = cli.getOptionValue("nc");
            try {
                nThreads = Integer.parseInt(nCoresStr);
            } catch (NumberFormatException e) {
                System.err.println("The argument for option -nc (number of threads) must be an integer");
                System.err.println("AMIE [OPTIONS] <.tsv INPUT FILES>");
                System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
                System.exit(1);
            }

            if (nThreads > nProcessors) {
                nThreads = nProcessors;
            }
        }

        if (cli.hasOption("mt")) {
            miningTechniqueStr = cli.getOptionValue("mt").toLowerCase();
            if (!miningTechniqueStr.equals("solidary")
                    && !miningTechniqueStr.equals("standard")) {
                miningTechniqueStr = "standard";
            }
        }

        avoidUnboundTypeAtoms = cli.hasOption("auta");
        exploitMaxLengthForRuntime = !cli.hasOption("deml");
        enableQueryRewriting = !cli.hasOption("dqrw");
        enablePerfectRulesPruning = !cli.hasOption("dpr");
        String[] leftOverArgs = cli.getArgs();

        if (leftOverArgs.length < 1) {
            System.err.println("No input file has been provided");
            System.err.println("AMIE [OPTIONS] <.tsv INPUT FILES>");
            System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
            System.exit(1);
        }

        //Load database
        for (int i = 0; i < leftOverArgs.length; ++i) {
            if (leftOverArgs[i].startsWith(":t")) {
                targetFiles.add(new File(leftOverArgs[i].substring(2)));
            } else if (leftOverArgs[i].startsWith(":s")) {
                schemaFiles.add(new File(leftOverArgs[i].substring(2)));
            } else {
                dataFiles.add(new File(leftOverArgs[i]));
            }
        }
        KB dataSource = new KB();
        long timeStamp1 = System.currentTimeMillis();
        dataSource.load(dataFiles);
        long timeStamp2 = System.currentTimeMillis();
        if (cli.hasOption("optimfh")) {
            Announce.message("Building overlap tables for confidence approximation.");
            dataSource.buildOverlapTables();
            Announce.done("Overlap tables were built.");
        }
        sourcesLoadingTime = timeStamp2 - timeStamp1;

        if (!targetFiles.isEmpty()) {
            targetSource = new KB();
            targetSource.load(targetFiles);
        }

        if (!schemaFiles.isEmpty()) {
            schemaSource = new KB();
            schemaSource.load(schemaFiles);
        }

        if (cli.hasOption("pm")) {
            switch (cli.getOptionValue("pm")) {
                case "support":
                    metric = Metric.Support;
                    System.err.println("Using " + metric + " as pruning metric with threshold " + minSup);
                    minMetricValue = minSup;
                    minInitialSup = minSup;
                    break;
                default:
                    metric = Metric.HeadCoverage;
                    System.err.println("Using " + metric + " as pruning metric with threshold " + minHeadCover);
                    break;
            }
        } else {
            System.out.println("Using " + metric + " as pruning metric with minimum threshold " + minHeadCover);
            minMetricValue = minHeadCover;
        }

        if (cli.hasOption("bias")) {
            bias = cli.getOptionValue("bias");
        }

        verbose = cli.hasOption("verbose");

        if (cli.hasOption("rl")) {
            try {
                recursivityLimit = Integer.parseInt(cli.getOptionValue("rl"));
            } catch (NumberFormatException e) {
                System.err.println("The argument for option -rl (recursivity limit) must be an integer");
                System.err.println("AMIE [OPTIONS] <.tsv INPUT FILES>");
                System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
                System.exit(1);
            }
        }
        System.out.println("Using recursivity limit " + recursivityLimit);

        enableConfidenceUpperBounds = cli.hasOption("optimcb");
        if (enableConfidenceUpperBounds) {
            System.out.println("Enabling standard and PCA confidences upper "
            		+ "bounds for pruning [EXPERIMENTAL]");
        }

        enableFunctionalityHeuristic = cli.hasOption("optimfh");
        if (enableFunctionalityHeuristic) {
            System.out.println("Enabling functionality heuristic with ratio "
            		+ "for pruning of low confident rules [EXPERIMENTAL]");
        }

        switch (bias) {    	
        	case "oneVar" :
        		mineAssistant = new MiningAssistant(dataSource);
        		break;
            case "default" :
                mineAssistant = new DefaultMiningAssistant(dataSource);
                break;
            case "signatured" :
            	mineAssistant = new RelationSignatureDefaultMiningAssistant(dataSource);
            	break;
            default: 
            	// To support customized assistant classes
            	// The assistant classes must inherit from amie.mining.assistant.MiningAssistant
            	// and implement a constructor with the any of the following signatures.
            	// ClassName(amie.data.KB), ClassName(amie.data.KB, String), ClassName(amie.data.KB, amie.data.KB) 
            	Class<?> assistantClass = null;
            	try {
	            	assistantClass = Class.forName(bias);	
            	} catch (Exception e) {
            		System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
            		e.printStackTrace();
            		System.exit(1);
            	}
            	
            	Constructor<?> constructor = null;
            	try {
            		// Standard constructor
            		constructor = assistantClass.getConstructor(new Class[]{KB.class});
            		mineAssistant = (MiningAssistant) constructor.newInstance(dataSource);
            	} catch (NoSuchMethodException e) {
            		try {
            			// Constructor with additional input            			
            			constructor = assistantClass.getConstructor(new Class[]{KB.class, String.class});
            			System.out.println(cli.getOptionValue("ef"));
            			mineAssistant = (MiningAssistant) constructor.newInstance(dataSource, cli.getOptionValue("ef"));
            		} catch (NoSuchMethodException ep) {
            			// Constructor with schema KB       
            			try {
            				constructor = assistantClass.getConstructor(new Class[]{KB.class, KB.class});
            				mineAssistant = (MiningAssistant) constructor.newInstance(dataSource, schemaSource);
            			} catch (Exception e2p) {
            				e.printStackTrace();
            				System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
                    		e.printStackTrace();
            			}
            		}
            	} catch (SecurityException e) {
            		System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
            		e.printStackTrace();
            		System.exit(1);
            	}
            	break;
        }
        
        allowConstants = cli.hasOption("const");
        countAlwaysOnSubject = cli.hasOption("caos");
        realTime = !cli.hasOption("oute");
        enforceConstants = cli.hasOption("fconst");

        // These configurations override others
        if (onlyOutput) {
            System.out.println("Using the only output enhacements configuration.");
            enablePerfectRulesPruning = false;
            enableQueryRewriting = false;
            exploitMaxLengthForRuntime = false;
            enableConfidenceUpperBounds = true;
            enableFunctionalityHeuristic = true;
            minPCAConf = DEFAULT_PCA_CONFIDENCE;
        }

        if (full) {
            System.out.println("Using the FULL configuration.");
            enablePerfectRulesPruning = true;
            enableQueryRewriting = true;
            exploitMaxLengthForRuntime = true;
            enableConfidenceUpperBounds = true;
            enableFunctionalityHeuristic = true;
            minPCAConf = DEFAULT_PCA_CONFIDENCE;
        }

        mineAssistant.setKbSchema(schemaSource);
        mineAssistant.setEnabledConfidenceUpperBounds(enableConfidenceUpperBounds);
        mineAssistant.setEnabledFunctionalityHeuristic(enableFunctionalityHeuristic);
        mineAssistant.setMaxDepth(maxDepth);
        mineAssistant.setStdConfidenceThreshold(minStdConf);
        mineAssistant.setPcaConfidenceThreshold(minPCAConf);
        mineAssistant.setAllowConstants(allowConstants);
        mineAssistant.setEnforceConstants(enforceConstants);
        mineAssistant.setBodyExcludedRelations(bodyExcludedRelations);
        mineAssistant.setHeadExcludedRelations(headExcludedRelations);
        mineAssistant.setTargetBodyRelations(bodyTargetRelations);
        mineAssistant.setCountAlwaysOnSubject(countAlwaysOnSubject);
        mineAssistant.setRecursivityLimit(recursivityLimit);
        mineAssistant.setAvoidUnboundTypeAtoms(avoidUnboundTypeAtoms);
        mineAssistant.setExploitMaxLengthOption(exploitMaxLengthForRuntime);
        mineAssistant.setEnableQueryRewriting(enableQueryRewriting);
        mineAssistant.setEnablePerfectRules(enablePerfectRulesPruning);
        mineAssistant.setVerbose(verbose);

        System.out.println(mineAssistant.getDescription());
        
        AMIE miner = new AMIE(mineAssistant, minInitialSup, minMetricValue, metric, nThreads);
        miner.setRealTime(realTime);
        miner.setSeeds(headTargetRelations);
        miner._setSourcesLoadingTime(sourcesLoadingTime);

        if (minStdConf > 0.0) {
            System.out.println("Filtering on standard confidence with minimum threshold " + minStdConf);
        } else {
            System.out.println("No minimum threshold on standard confidence");
        }

        if (minPCAConf > 0.0) {
            System.out.println("Filtering on PCA confidence with minimum threshold " + minPCAConf);
        } else {
            System.out.println("No minimum threshold on PCA confidence");
        }

        if (enforceConstants) {
            System.out.println("Constants in the arguments of relations are enforced");
        } else if (allowConstants) {
            System.out.println("Constants in the arguments of relations are enabled");
        } else {
            System.out.println("Constants in the arguments of relations are disabled");
        }

        if (exploitMaxLengthForRuntime && enableQueryRewriting && enablePerfectRulesPruning) {
            System.out.println("Lossless (query refinement) heuristics enabled");
        } else {
            if (!exploitMaxLengthForRuntime) {
                System.out.println("Pruning by maximum rule length disabled");
            }

            if (!enableQueryRewriting) {
                System.out.println("Query rewriting and caching disabled");
            }

            if (!enablePerfectRulesPruning) {
                System.out.println("Perfect rules pruning disabled");
            }
        }
       
        return miner;
    }

	/**
	 * AMIE's main program
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
    	  AMIE miner = AMIE.getInstance(args);

          Announce.doing("Starting the mining phase");

          long time = System.currentTimeMillis();
          List<Rule> rules = null;

          rules = miner.mine();

          if (!miner.isRealTime()) {
              Rule.printRuleHeaders();
              for (Rule rule : rules) {
                  System.out.println(rule.getFullRuleString());
              }
          }

          if (miner.isVerbose()) {
              System.out.println("Specialization time: " + (miner._getSpecializationTime() / 1000.0) + "s");
              System.out.println("Scoring time: " + (miner._getScoringTime() / 1000.0) + "s");
              System.out.println("Queueing and duplicate elimination: " + (miner._getQueueingAndDuplicateElimination() / 1000.0) + "s");
              System.out.println("Approximation time: " + (miner._getApproximationTime() / 1000.0) + "s");
          }

          long miningTime = System.currentTimeMillis() - time;
          System.out.println("Mining done in " + NumberFormatter.formatMS(miningTime));
          Announce.done("Total time " + NumberFormatter.formatMS(miningTime + miner._getSourcesLoadingTime()));
          System.out.println(rules.size() + " rules mined.");
    }
}
