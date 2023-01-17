package anyburl;

import java.util.HashSet;


/**
 * A rule of the form h(a,X) <= b1(X,A1), ..., bn(An-1,c) with a constant in the head and in the last body atom.
 * 
 */
public class RuleAcyclic1 extends RuleAcyclic {

	
	public RuleAcyclic1(RuleUntyped r) {
		super(r);
	}
	
	
	protected String getUnboundVariable() {
		return null;
	}
	
	
	// public double getAppliedConfidence() {
	// 	return (double)this.getCorrectlyPredictedHeads() / ((double)this.getPredictedHeads() + Settings.UNSEEN_NEGATIVE_EXAMPLES + Settings.UNSEEN_NEGATIVE_EXAMPLES_ATYPED[2]);
	// }
	
	/*
	public double getAppliedConfidence() {
		return (double)this.getCorrectlyPredicted() / ((double)this.getPredicted() + Math.pow(Settings.UNSEEN_NEGATIVE_EXAMPLES, this.bodysize()));
	}
	*/
	

	/**
	* Returns the number of groundings w.r.t the given triple set for the variable in the last atom.
	* @param triples The triples set to check for groundings.
	* @return The number of groundings.
	*/
	public int getGroundingsLastAtom(TripleSet triples) {
		Atom last = this.body.getLast();
		if (last.isRightC()) {
			return triples.getHeadEntities(last.getRelation(), last.getRight()).size();
		}
		else {
			return triples.getTailEntities(last.getRelation(), last.getLeft()).size();
		}
	}




	public boolean isSingleton(TripleSet triples) {
		// return false;
		
		if (this.body.get(0).getRight().equals("X") && this.body.get(0).getRight().equals("Y")) {
			String head = this.body.get(0).getLeft();
			String relation = this.body.get(0).getRelation();
			if (triples.getTailEntities(relation, head).size() > 1) return false;
			else return true;
		}
		else {
			String tail = this.body.get(0).getRight();
			String relation = this.body.get(0).getRelation();
			if (triples.getHeadEntities(relation, tail).size() > 1) return false;
			else return true;
		}	
	}
	
	
	public boolean isCyclic() {
		if (this.getHead().getConstant().equals(this.body.getLast().getConstant())) return true;
		return false;
	}


	public String toXYString() {
		if (this.head.getLeft().equals("X")) {
			String c = this.head.getRight();
			StringBuilder sb = new StringBuilder();
			sb.append(this.getHead().toString(c, "Y"));
			for (int i = 0; i < this.bodysize(); i++) { sb.append(this.getBodyAtom(i).toString(c,"Y")); }
			String rs = sb.toString();
			return rs;
		}
		if (this.head.getRight().equals("Y")) {
			String c = this.head.getLeft();
			StringBuilder sb = new StringBuilder();
			sb.append(this.getHead().toString(c, "X"));
			for (int i = this.bodysize()-1; i >= 0; i--) { sb.append(this.getBodyAtom(i).toString(c,"X")); }
			String rs = sb.toString();
			return rs;
		}
		System.err.println("toXYString of the following rule not implemented: " + this);
		System.exit(1);
		return null;
	}


	public boolean validates(String h, String relation, String t, TripleSet ts) {
		if (this.getTargetRelation().equals(relation)) {
			// this rule is a X rule
			if (this.head.isRightC() && this.head.getRight().equals(t)) {
				// could be true if body is true
				HashSet<String> previousValues = new HashSet<String>();
				previousValues.add(h);
				previousValues.add(this.head.getRight());
				return (this.isBodyTrueAcyclic("X", h, 0, previousValues, ts));
			}
			// this rule is a Y rule
			if (this.head.isLeftC() && this.head.getLeft().equals(h)) {
				// could be true if body is true
				
				HashSet<String> previousValues = new HashSet<String>();
				previousValues.add(t);
				previousValues.add(this.head.getLeft());
				return (this.isBodyTrueAcyclic("Y", t, 0, previousValues, ts));
			}
			return false;
		}
		return false;
		
	}
	
	/**
	 * This method computes for an x and y value pair, if there is a body grounding in the given triple set.
	 * If this is the case it returns a non empty set of triples, which is the set of triples that has been used
	 * to ground the body. It is not allowed to use triples from the set of excludedTriples. 
	 * 
	 * The method should be extremly fast, as its restricted to rules of length 1 only.
	 * 
	 * IMPORTANT NOTE: The method is currently in a certain sense hard-coded for rules of length 1.
	 * Longer rules are not supported so far.
	 * 
	 * @param xValue The value of the X variable.
	 * @param yValue The value of the Y variable.
	 * @param excludedGroundings The triples that are forbidden to be used.
	 * 
	 * @return A minimal set of triples that results into a body grounding.
	 */
	public HashSet<Triple> getTripleExplanation(String xValue, String yValue, HashSet<Triple> excludedTriples, TripleSet triples) {
		
		
		if (this.bodysize() != 1) {
			System.err.println("Trying to get a triple explanation for an acyclic rule with constant in head any body of length != 1. This is not yet implemented.");
			System.exit(-1);
		}
		HashSet<Triple> groundings = new HashSet<Triple>();
		boolean xInHead = false;
		if (this.head.getLeft().equals("X")) xInHead  = true;
		if (xInHead) {
			if (this.head.getRight().equals(yValue) || (this.head.getRight().equals(Settings.REWRITE_REFLEXIV_TOKEN) && xValue.equals(yValue))) {
				String left = this.body.get(0).getLeft();
				String right = this.body.get(0).getRight();
				String rel = this.body.get(0).getRelation();
				if (left.equals("X") && triples.isTrue(xValue, rel, right)) {
					Triple t = new Triple(xValue, rel, right);
					if (!excludedTriples.contains(t)) groundings.add(t);
				}
				if (right.equals("X") && triples.isTrue(left, rel, xValue)) {
					Triple t = new Triple(left, rel, xValue);
					if (!excludedTriples.contains(t)) groundings.add(t);
				}
			}
		}
		else {
			if (this.head.getLeft().equals(xValue) || (this.head.getLeft().equals(Settings.REWRITE_REFLEXIV_TOKEN) && xValue.equals(yValue))) {
				String left = this.body.get(0).getLeft();
				String right = this.body.get(0).getRight();
				String rel = this.body.get(0).getRelation();
				if (left.equals("Y") && triples.isTrue(yValue, rel, right)) {
					Triple t = new Triple(yValue, rel, right);
					if (!excludedTriples.contains(t)) groundings.add(t);
				}
				if (right.equals("Y") && triples.isTrue(left, rel, yValue)) {
					Triple t = new Triple(left, rel, yValue);
					if (!excludedTriples.contains(t)) groundings.add(t);
				}
			}
		}
		return groundings;
	}
	


}
