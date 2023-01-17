package anyburl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

public abstract class Rule {
	
	
	
	protected static Random rand = new Random();
	protected static boolean APPLICATION_MODE = false;
	protected static final String[] variables = new String[] {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P"};
	protected static HashMap<String,Integer> variables2Indices = new HashMap<String, Integer>();

		
	protected Atom head;
	// protected ArrayList<Atom> body; 
	
	protected Body body; 
	
	
	protected int hashcode = 0;
	protected boolean hashcodeInitialized = false;

	
	protected int predicted = 0;
	protected int correctlyPredicted = 0;
	protected double confidence = 0.0; 
	
	protected int nextFreeVariable = 0;
	
	// ********************
	// *** CONSTRUCTORS ***
	// ********************
	
	static {
		for (int i = 0; i < variables.length; i++) {
			variables2Indices.put(variables[i], i);
		}
	}
	
	
	public Rule(RuleUntyped r) {
		this.body = r.body;
		this.head = r.head;
		this.confidence = r.confidence;
		this.correctlyPredicted = r.correctlyPredicted;
		this.predicted = r.predicted;
	}
	
	
	public Rule(Atom head) {
		this.head = head;
		this.body = new Body();
	}
	
	public Rule() {
		this.body = new Body();
	}
	
	
	
	// ***********************
	// *** GETTER / SETTER ***
	// ***********************
	
	public Rule getCopy() {			
		RuleUntyped copy = new RuleUntyped(this.head.createCopy());
		for (Atom bodyLiteral : this.body) {
			copy.body.add(bodyLiteral.createCopy());
		}
		copy.nextFreeVariable = this.nextFreeVariable; // ???
		if (copy.isCyclic()) {
			RuleCyclic r = new RuleCyclic(copy);
			return r;
		}
		if (copy.isAcyclic1()) {
			RuleAcyclic1 r = new RuleAcyclic1(copy);
			return r;
		}
		if (copy.isAcyclic2()) {
			RuleAcyclic2 r = new RuleAcyclic2(copy);
			return r;
		}
		return null;

	}

	
	
	
	
	public static void applicationMode() {
		Rule.APPLICATION_MODE = true;
	}

	public void setHead(Atom head) {
		this.head = head;	
	}


	public void addBodyAtom(Atom atom) {
		this.body.add(atom);
	}
	
	public Atom getBodyAtom(int index) {
		return this.body.get(index);
	}
	
	public Atom getHead() {
		return this.head;
	}
	
	public String getTargetRelation() {
		return this.head.getRelation();
	}
	
	public int getPredicted() {
		return this.predicted;
	}
	
	public int getCorrectlyPredicted() {
		return this.correctlyPredicted;
	}

	public double getConfidence() {
		return confidence;
	}
	
	
	/*
	public double getConfidenceMax() {
		return Math.max(this.confidenceHeads, this.confidenceTails);
	}
	*/


	
	
	public int bodysize() {
		return this.body.size();
	}
	
	public boolean isTrivial() {
		if (this.bodysize() == 1) {
			if (this.head.equals(this.body.get(0))) return true;
		}
		return false;
	}
	
	// public abstract double getAppliedConfidenceHeads();
	
	// public abstract double getAppliedConfidenceTails();
	
	public double getAppliedConfidence() {
		return (double)this.getCorrectlyPredicted() / ((double)this.getPredicted() + Settings.UNSEEN_NEGATIVE_EXAMPLES);
	}
	
	
	public boolean isXYRule() {
		if (this.head.isLeftC() || this.head.isRightC()) return false;
		else return true;
	}
	
	public boolean isXRule() {
		if (this.isXYRule()) return false;
		else {
			if (!this.head.isLeftC()) return true;
			else return false;
		}
	}
	
	public boolean isYRule() {
		if (this.isXYRule()) return false;
		else {
			if (!this.head.isRightC()) return true;
			else return false;
		}
	}


	
	

	// ****************
	// *** TOSTRING ***
	// ****************
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.predicted + "\t");
		sb.append(this.correctlyPredicted + "\t");
		sb.append(this.confidence + "\t");
		sb.append(this.head);
		sb.append(" <= ");
		sb.append(this.body.toString());
		return sb.toString();
	}
	
	
	
	// ********************
	// *** EQUAL + HASH ***
	// ********************
	
	public boolean equals(Object thatObject) {
		if (thatObject instanceof Rule) {
			Rule that = (Rule)thatObject;
			if (this.head.equals(that.head) && this.body.equals(that.body)) {
				return true;
			}
			return false;
		}
		return false;
	}
	
	public int hashCode() {
		if (!this.hashcodeInitialized) {
			StringBuilder sb = new StringBuilder(this.head.toString());
			for (Atom atom : this.body) {
				sb.append(atom.toString());
			}
			this.hashcode = sb.toString().hashCode();
			// this.hashcode = this.toString().hashCode();
			this.hashcodeInitialized = true;
		}
		
		return this.hashcode;
	}
	
	

	
	// *************
	// *** LOGIC ***
	// *************
	
	/**
	 * Computes the scores of the joint rule, i.e., the rule that has the conjunction of this and that rule body as body. If one of the rules
	 * uses a constant and the other rule uses two variables, then one of the variables is replaced in both head and body of the rule by
	 * that constant.
	 * 
	 * @param that The other rule
	 * @param triples The triple set used for computing the scores
	 * @return An int[] with two elements. The first element (index 0) represents the all predictions made by the joint rules.
	 * The second element (index 1) represents all correct predictions made by the joint rule 
	 */
	public abstract int[] computeScores(Rule that, TripleSet triples);
	
	/**
	 * 
	 * @param ts
	 */
	public abstract void computeScores(TripleSet ts);

	/**
	*  Returns the tail results of applying this rule to a given head value.
	* 
	* @param head The given head value.
	* @param ts The triple set used for computing the results.
	* @return An empty set, a set with one value (the constant of the rule) or the set of all body instantiations.
	*/
	public abstract HashSet<String> computeTailResults(String head, TripleSet ts);
	
	
	/**
	*  Returns the head results of applying this rule to a given tail value.
	* 
	* @param tail The given tail value.
	* @param ts The triple set used for computing the results.
	* @return An empty set, a set with one value (the constant of the rule) or the set of all body instantiations.
	*/
	public abstract HashSet<String> computeHeadResults(String tail, TripleSet ts); 
	
	/**
	* Checks if the body of the rule is true for the given subject/object pair.
	* This method is called in the context of rule refinement (also called rule extension).
	* 
	* @param leftValue The subject (or left value).
	* @param rightValue The object (or right value).
	* @param ts The triple set.
	* @return True if the value pair (or one of the values) is predicted.
	*/
	// public abstract boolean isPredictedX(String leftValue, String rightValue, TripleSet ts);
	
	/**
	* Checks if the body of the rule is true for the given subject/object pair, while triviality is avoided by
	* not allowing that the predicted triple is used.
	* This method is called in the context of rule refinement (also called rule extension).
	* 
	* @param leftValue The subject (or left value).
	* @param rightValue The object (or right value).
	* @param ts The triple set.
	* @return True if the value pair (or one of the values) is predicted.
	*/
	public abstract boolean isPredictedX(String leftValue, String rightValue, Triple forbidden, TripleSet ts);
		
	
	
	/**
	 * 
	 * 
	 * @return True, if this rule is refineable. False otherwise.
	 */
	public abstract boolean isRefinable();
	
	/**
	* Returns a randomly chose triples that is both predicted and valid = true against the given triple set.
	*
	* @param ts Triple set deciding the truth of the triples
	* @return The predicted triple.
	*/
	public abstract Triple getRandomValidPrediction(TripleSet ts);
	
	
	/**
	* Returns a randomly chose triples that is both predicted and not valid = false against the given triple set.
	*
	* @param ts Triple set deciding the truth of the triples
	* @return The predicted triple.
	*/
	public abstract Triple getRandomInvalidPrediction(TripleSet ts);
	
	/**
	* Retrieves a sample of prediction (correct or incorrect).
	* 
	* @param ts The triple set used for predicting.
	* @return A list of triples that are predicted,
	*/
	public abstract ArrayList<Triple> getPredictions(TripleSet ts);


	/**
	* If the rule body has only one head variable, it is called singleton, if only one entity full fills the body. 
	* @return
	*/
	public abstract boolean isSingleton(TripleSet triples);
	
	
	/**
	 * Checks if a rule can fire given the observations without the excluded triples. If the rule fires, the triples used to fire are returned.
	 * 
	 * @param xValue The value of the subject.
	 * @param yValue The value of the object.
	 * @param excludedTriples The triples that are not allowed to entail the entailment.
	 * @param triples The triple set which is used to fire the rule (= the given observations).
	 * @return The set of triples that was used to fire the rule. If null or the empty set is returned, then it was not possible to fire the rule.
	 */
	public abstract HashSet<Triple> getTripleExplanation(String xValue, String yValue, HashSet<Triple> excludedTriples, TripleSet triples);


	public TripleSet materialize(TripleSet trainingSet) {
		return null;
	}



	
	
	
}
