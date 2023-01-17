package anyburl;

import java.io.PrintWriter;
import java.util.HashMap;

public class Settings {
	
	
	/**
	 * If set to true, which is the default value, the OI constraints are activated in cyclic rules.
	 * This value is changed only for experimental reasons. Its default should not be changed.
	 * 
	 */
	public static final boolean OI_CONSTRAINTS_ACTIVE = true;

	public static HashMap<String, Integer> KEYWORD;
	
	/**
	 * Do not change this. For APPLY its required that this is set to false, for LEARNING both is okay
	 * Currently in Apply its set to false hard-coded. It means this parameter has no impact on Apply.
	 */
	public static boolean REWRITE_REFLEXIV = true;
	
	public static final String REWRITE_REFLEXIV_TOKEN = "me_myself_i";
	
	public static boolean BEAM_NOT_DFS = true; 
	public static boolean DFS_SAMPLING_ON = true; 
	
	
	
	/**
	 * If set to true, in computeScores the starting point is a randomly chosen entity. 
	 * This entity is randomly chosen from the set of all entities that are a possible starting point.
	 * If set to false its a randomly chosen triples that instantiates the first body atom.
	 * This means that in this setting a starting entity that appears in many such triples, will be more frequently a starting
	 * entity.
	 * 
	 * 
	 */
	public static final boolean BEAM_TYPE_EDIS = true;


	
	/**
	 * If set to true, it adds a prefix in front of each entity and relation id, when reading triplesets from files
	 * to avoid problem related to input that uses numbers as ids only. 
	 */
	public static boolean SAFE_PREFIX_MODE = false;
	
	public static final String PREFIX_ENTITY = "e";
	public static final String PREFIX_RELATION = "r";
	
	public static String[] SINGLE_RELATIONS = null;
	
	/**
	* Suppresses any rules with constants.
	*/
	public static boolean CONSTANTS_OFF = false;
	
	public static double EPSILON = 0.1;
	
	/**
	* In the first batch the decisions are completely randomized. This random influence becomes less
	* at will be stable at  RANDOMIZED_DECISIONS after this number of batches have been carried out.
	*/
	public static double RANDOMIZED_DECISIONS_ANNEALING = 5;
	

	/**
	* REACTIVATED!
	*  
	* This number defines if a rule to be redundant if the number of groundings for its last atom is less than this parameter.
	* It avoid that rules with constants are too specific and thus redundant compared to shorter rules 
	*    head(X,c) <= hasGender(X, female)
	*    head(X,c) <= hasGender(X, A), hasGender(berta, A)
	* The second rule will be filtered out, because berta has only 1 gender, which is female.
	* 
	*/
	public static int AC_MIN_NUM_OF_LAST_ATOM_GROUNDINGS = 5;
	
	
	/**
	* PROBABLY OUT 
	* 
	* The specialization confidence interval determines that a rule shall only be accepted as a specialization of a more general rule, if
	* it has a higher confidence and if the probability that its confidence is really higher is at least that chosen value.
	* Possible values are 0.9, 0.99 and 0.99.
	* 
	* -1 = turned-off
	*/
	public static double SPECIALIZATION_CI = -1;
	
	/**
	 * Relevant for reinforced learning, how to compute the scores created by a thread.
	 * 
	 * 1 = correct predictions
	 * 2 = correct predictions weighted by confidence
	 * 3 = correct predictions weighted by applied confidence
	 * 4 = correct predictions weighted by applied confidence^2
	 * 5 = correct predictions weighted by applied confidence divided by (rule length-1)^2
	 */
	public static int REWARD = 5;
	
	
	/**
	 * Relevant for reinforced learning, how to use the scores created by a thread within the decision.
	 * 
	 * 1 = GREEDY = Epsilon greedy: Focus only on the best.
	 * 2 = WEIGHTED = Weighted policy: focus as much as much a a path type, as much as it gave you.
	 * 
	 */
	public static int POLICY = 2;
	
	
	
	/**
	 * Defines the prediction type which also influences the usage of the other parameters. 
	 * Possible values are currently aRx and xRy.
	 */
	public static String PREDICTION_TYPE = "aRx";
	
	
	/**
	 * Path to the file that contains the triple set used for learning the rules.
	 */
	public static String PATH_TRAINING = "";
	
	
	
	/**
	 * Path to the file that contains the triple set used for to test the rules.
	 */
	public static String PATH_TEST = "";
	
	/**
	 * Path to the file that contains the triple set used validation purpose (e.g. learning hyper parameter).
	 */
	public static String PATH_VALID = "";
	
	
	/**
	 * Path to the file that contains the rules that will be refined or will be sued for prediction.
	 */
	public static String PATH_RULES = "";
	
	/**
	 * Path to the file that contains the rules that will be used as base,
	 * i.e. this rule set will be added to all other rule sets loaded.
	 */
	public static String PATH_RULES_BASE = "";
	
	/**
	 * Path to the output file where the rules / predictions  will be stored.
	 */
	public static String PATH_OUTPUT = "";

	
	/**
	 * Path to the output file where statistics of the dice are stored.
	 * Can be used in reinforcement learning only. If the null value is not overwritten, nothing is stored.
	 */
	public static String PATH_DICE = null;
	
	
	/**
	 * Path to the output file where the explanations are stored. If not set no explanations are stored.
	 */
	public static String PATH_EXPLANATION = null;
	
	public static PrintWriter EXPLANATION_WRITER = null;
	
	
	
	/**
	 * Takes a snapshot of the rules refined after each time interval specified in seconds.
	 */
	public static int[] SNAPSHOTS_AT = new int[] {10,100};
	
	

	
	
	/**
	* Number of maximal attempts to create body grounding. Every partial body grounding is counted.
	* 
	* NO LONGER IN USE (maybe)
	*/
	public static int TRIAL_SIZE = 1000000;
	
	
	/**
	 * Returns only results for head or tail computation if the results set has less elements than this bound.
	 * The idea is that any results set which has more elements is anyhow not useful for a top-k ranking. 
	 * Should be set to a value thats higher than the k of the requested top-k (however, the higher the value,
	 * the more runtime is required)
	 * 
	 */
	public static int DISCRIMINATION_BOUND = 10000;	
	
	
	/**
	 * This is the upper limit that is allowed as branching factor in a cyclic rule.
	 * If more than this number of children would be created in the search tree, the
	 * branch is not visited. Note that this parameter is nor relevant for the last
	 * step, where the DISCRIMINATION_BOUND is the relevant parameter.
	 * 
	 */
	public static int BRANCHINGFACTOR_BOUND = 1000;

		
	/**
	 * The time that is reserved for one batch in milliseconds. 
	 */
	public static int BATCH_TIME = 5000;
	
	
	
	/**
	 * The maximal number of body atoms in cyclic rules (inclusive this number). If this number is exceeded all computation time
	 * is used for acyclic rules only from that time on.
	 * 
	 */
	public static int MAX_LENGTH_CYCLIC = 3;
	
	
	
	/**
	 * Determines whether or not the zero rules e.g. (gender(X, male) <= [0.5]) are active
	 * 
	 */
	public static boolean ZERO_RULES_ACTIVE = true; 
	
	
	/**
	 * is used for cyclic rules only from that time on.
	 * 
	 */
	public static int MAX_LENGTH_ACYCLIC = 1;
	
	
	/**
	 * The maximal number of body atoms in partially grounded cyclic rules (inclusive this number). If this number is exceeded than a
	 * cyclic path that would allow to construct such a rule (where the constant in the head and in the body is the same) is used for constructing
	 * general rules only, partially grounded rules are not constructed from such a path.
	 */
	public static int MAX_LENGTH_GROUNDED_CYCLIC = 1;
	
	
	/**
	* Experiments have shown that AC2 rules seem make results worse for most datasets. Setting this parameter to true, will result into not learning
	* AC2 rules at all. The rules are not even constructed and computation time is this spent on the other rules-
	*  
	*/
	public static boolean EXCLUDE_AC2_RULES = false;
	
	/**
	 * The saturation defined when to stop the refinement process. Once the saturation is reached, no further refinements are searched for.
	 */
	public static double SATURATION = 0.99;
	
	/**
	 * The threshold for the number of correct prediction created with the refined rule.
	 */
	public static int THRESHOLD_CORRECT_PREDICTIONS = 2;
	
	/**
	 * The threshold for the number of correct prediction created with a zero rule.
	 */
	public static int THRESHOLD_CORRECT_PREDICTIONS_ZERO = 100;

	
	/**
	 * The threshold for the number of correct predictions. Determines which rules are read from a file and which are ignored.
	 */
	public static int READ_THRESHOLD_CORRECT_PREDICTIONS = 2;
	
	/**
	 * The number of negative examples for which we assume that they exist, however, we have not seen them. Rules with high coverage are favored the higher the chosen number. 
	 */
	public static int UNSEEN_NEGATIVE_EXAMPLES = 5;	
	
	
	/**
	 * The number of negative examples for which we assume that they exist, however, we have not seen them.
	 * This number is for each refinements step, including the refinement of a refined rule.
	 */
	public static int UNSEEN_NEGATIVE_EXAMPLES_REFINE = 5;	
	
	

	/**
	* If set to true, the rule application is done on the rule set and each subset that consists of one type as well as each subset that removed one type.
	* This setting should be used in an ablation study.
	* 
	*/
	public static boolean TYPE_SPLIT_ANALYSIS_ = false;

	/**
	 * The threshold for the confidence of the refined rule
	 */
	public static double THRESHOLD_CONFIDENCE = 0.0001;

	/**
	 * The threshold for the confidence of the rule. Determines which rules are read from a file by the rule reader.
	 */
	public static double READ_THRESHOLD_CONFIDENCE = 0.0001;
	
	
	/**
	 * The maximal size of the rules that are stored when reading them from a file. 
	 * Determines which rules are read from a file by the rule reader.
	 * All rules with a body length > then this number are ignored.
	 */
	public static double READ_THRESHOLD_MAX_RULE_LENGTH = 10;
	
	/** 
	 * The number of worker threads which compute the scores of the constructed rules, should be one less then the number of available cores.
	 */
	public static int WORKER_THREADS = 3;
	
	
	/**
	* Defines how to combine probabilities that come from different rules
	* Possible values are: maxplus, max2
	*/
	public static String AGGREGATION_TYPE = "maxplus";
	
	/**
	 * This value is overwritten by the choice made vie the AGGREGATION_TYPE parameter
	 */
	public static int AGGREGATION_ID = 1;
	
	
	// NEW BEAM SAMPLING SETTINGS 
	
	
	/**
	* Used for restricting the number of samples drawn for computing scores as confidence.
	*/
	public static int SAMPLE_SIZE = 2000;
	
	/**
	 * The maximal number of body groundings. Once this number of body groundings has been reached,
	 * the sampling process stops and confidence score is computed.
	 */
	public static int BEAM_SAMPLING_MAX_BODY_GROUNDINGS = 1000;  // default = 1000
	
	/**
	 * The maximal number of attempts to create a body grounding. Once this number of attempts has been reached
	 * the sampling process stops and confidence score is computed.
	 */
	public static int BEAM_SAMPLING_MAX_BODY_GROUNDING_ATTEMPTS = 100000; // default = 100000
	
	/**
	 * If a rule has only few different body groundings this parameter prevents that all attempts are conducted.
	 * The value of this parameter determines how often a same grounding is drawn, before the sampling process stops
	 * and the and confidence score is computed, e.g, 5 means that the algorithm stops if 5 times a grounding is
	 * constructed tat has been found previously. The higher the value the mor probably it is that the sampling
	 * computes the correct value for rules with few body groundings.
	 */
	public static int BEAM_SAMPLING_MAX_REPETITIONS = 5;	// default = 5
	
	
	/**
	 * The weights that is multiplied to compute the applied confidence of a zero
	 * rule. 1.0 means that zero rules are treated in the same way as the other
	 * rules.
	 */
	public static double RULE_ZERO_WEIGHT = 0.01; // default = 0.01

	/**
	 * The weights that is multiplied to compute the applied confidence of an AC2
	 * rule. 1.0 means that AC2 rules are treated in the same way as the other
	 * rules.
	 */
	public static double RULE_AC2_WEIGHT = 0.1; // default = 0.1
	
	
	/**
	 * If the default is the confidence of cyclic rules of length 1 is multiplied by 0.75^0=1,
	 * length 2 is multiplied by 0.75^1=0.75, length 3 is multiplied by 0.75^2=0.65
	 */
	public static final double RULE_LENGTH_DEGRADE = 1.0;
	

	/**
	 * The top-k results that are after filtering kept in the results. 
	 */
	public static int TOP_K_OUTPUT = 10;	
	
	public static int READ_CYCLIC_RULES = 1;
	public static int READ_ACYCLIC1_RULES = 1;
	public static int READ_ACYCLIC2_RULES = 1;
	public static int READ_ZERO_RULES = 1;
			
	/*
	static {
		
		KEYWORD = new HashMap<String, Integer>();
		KEYWORD.put("greedy", 1);
		KEYWORD.put("weighted", 2);
		KEYWORD.put("sup", 1);
		KEYWORD.put("supXcon", 3);
		KEYWORD.put("supXcon/lr", 5);
		KEYWORD.put("supXcon/rl", 5);
	}
	*/
	
	

}
