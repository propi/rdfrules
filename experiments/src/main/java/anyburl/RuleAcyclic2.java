package anyburl;

import java.util.HashMap;
import java.util.HashSet;

public class RuleAcyclic2  extends RuleAcyclic {
	
	private String unboundVariable = null;
	
	public RuleAcyclic2(RuleUntyped r) {
		super(r);
	}
	
	
	protected String getUnboundVariable() {
		if (this.unboundVariable != null) return this.unboundVariable;
		// if (this.body.get(this.body.size()-1).isLeftC() || this.body.get(this.body.size()-1).isRightC()) return null;
		HashMap<String, Integer> counter = new HashMap<String, Integer>();
		for (Atom atom : this.body) {
			if (!atom.getLeft().equals("X") && !atom.getLeft().equals("Y")) {
				if (counter.containsKey(atom.getLeft())) counter.put(atom.getLeft(), 2);
				else counter.put(atom.getLeft(), 1);
			}
			if (!atom.getRight().equals("X") && !atom.getRight().equals("Y")) {
				if (counter.containsKey(atom.getRight())) counter.put(atom.getRight(), 2);
				else counter.put(atom.getRight(), 1);
			}
		}
		for (String variable : counter.keySet()) {
			if (counter.get(variable) == 1) {
				this.unboundVariable = variable;
				return variable;
			}
		}
		// this can never happen
		return this.unboundVariable;
	}
	
	
	public double getAppliedConfidence() {
		return Settings.RULE_AC2_WEIGHT * super.getAppliedConfidence();
	}
	
	
	
	
	
	

	public boolean isSingleton(TripleSet triples) {
		return false;
	}
	
	
	
	/* probably out
	public boolean isRedundantACRule(TripleSet triples) {
		Atom last = this.body.getLast();
		if (last.isRightC()) {
			if (triples.getTriplesByRelation(last.getRelation()).size() < Settings.AC_MIN_NUM_OF_LAST_ATOM_GROUNDINGS) {
				return true;
			}
		}
		else {
			if (triples.getTriplesByRelation(last.getRelation()).size() < Settings.AC_MIN_NUM_OF_LAST_ATOM_GROUNDINGS) {
				return true;
			}
		}
		return false;
	}
	*/
	
	/**
	* Returns a lower border the number of groundings w.r.t the given triple set for the bound variable in the last atom.
	* @param triples The triples set to check for groundings.
	* @return The number of groundings if its lower  to Settings.AC_MIN_NUM_OF_LAST_ATOM_GROUNDINGS,
	* otherwise Settings.AC_MIN_NUM_OF_LAST_ATOM_GROUNDINGS
	*/
	public int getGroundingsLastAtom(TripleSet triples) {
		String unboundVariable = this.getUnboundVariable();
		Atom last = this.body.getLast();
		if (last.getRight().equals(unboundVariable)) {
			HashSet<String> values = new HashSet<String>();
			for (Triple t : triples.getTriplesByRelation(last.getRelation())) {
				values.add(t.getHead());
				if (values.size() >= Settings.AC_MIN_NUM_OF_LAST_ATOM_GROUNDINGS) return values.size(); 
			}
			return values.size();
			
		}
		else {
			HashSet<String> values = new HashSet<String>();
			for (Triple t : triples.getTriplesByRelation(last.getRelation())) {
				values.add(t.getTail());
				if (values.size() >= Settings.AC_MIN_NUM_OF_LAST_ATOM_GROUNDINGS) return values.size(); 
			}
			return values.size();
		}
	}
	
	
	@Override
	public HashSet<Triple> getTripleExplanation(String xValue, String yValue, HashSet<Triple> excludedTriples, TripleSet triples) {
		System.err.println("Your are asking for a triple explanation using an AC2 rule (a.k.a. U_d rule). Triple explanations for this rule are so far not implemented.");
		return null;
	}
	
	@Override
	public int[] computeScores(Rule that, TripleSet triples) {
		System.err.println("method not yet available for an untyped rule");
		return new int[2];
	}
	



}
