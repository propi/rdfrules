package anyburl;

import java.util.ArrayList;
import java.util.HashSet;

public class RuleUntyped extends Rule {
	
	public RuleUntyped(Atom head) {
		this.head = head;
		this.body = new Body();
	}
	
	
	public RuleUntyped() {
		this.body = new Body();
	}
	
	
	public boolean isCyclic() {
		if (this.head == null) return false;
		if (this.head.isLeftC() || this.head.isRightC()) return false;
		else return true;
	}
	
	public boolean isAcyclic1() {
		if (this.isCyclic() || this.isZero()) return false;
		else {
			if (this.body.get(this.bodysize()-1).isLeftC() || this.body.get(this.bodysize()-1).isRightC()) {
				return true;
			}
			return false;
		}
	}
	
	public boolean isAcyclic2() {
		if (this.isCyclic() || this.isZero()) return false;
		else {
			if (this.body.get(this.bodysize()-1).isLeftC() || this.body.get(this.bodysize()-1).isRightC()) {
				return false;
			}
			return true;
		}
	}
	
	public boolean isZero() {
		if (this.bodysize() == 0) return true;
		else return false;
	}
	
	public RuleUntyped(int predicted, int correctlyPredicted, double confidence) {
		this.predicted = predicted;
		this.correctlyPredicted = correctlyPredicted;
		this.confidence = confidence; 
		this.body = new Body();
	}
	
	
	protected RuleUntyped getLeftRightGeneralization() {
		RuleUntyped lrG = this.createCopy();
		String leftConstant = lrG.head.getLeft();
		int xcount = lrG.replaceByVariable(leftConstant, "X");
		String rightConstant = lrG.head.getRight();
		int ycount = lrG.replaceByVariable(rightConstant, "Y");
		if (xcount < 2 || ycount < 2) lrG = null;
		return lrG;
	}
	

	
	protected RuleUntyped getLeftGeneralization() {
		RuleUntyped leftG = this.createCopy();
		String leftConstant = leftG.head.getLeft();
		int xcount = leftG.replaceByVariable(leftConstant, "X");
		if (this.bodysize() == 0) return leftG;
		if (xcount < 2) leftG = null;
		return leftG;
	}
	
	protected RuleUntyped getRightGeneralization() {
		RuleUntyped rightG = this.createCopy();
		String rightConstant = rightG.head.getRight();
		int ycount = rightG.replaceByVariable(rightConstant, "Y");
		if (this.bodysize() == 0) return rightG;
		if (ycount < 2) rightG = null;
		return rightG;
	}
	
	
	protected RuleUntyped createCopy() {
		RuleUntyped copy = new RuleUntyped(this.head.createCopy());
		for (Atom bodyLiteral : this.body) {
			copy.body.add(bodyLiteral.createCopy());
		}
		copy.nextFreeVariable = this.nextFreeVariable;
		return copy;
	}
	
	protected int replaceByVariable(String constant, String variable) {
		int count = this.head.replaceByVariable(constant, variable);
		for (Atom batom : this.body) {
			int bcount = batom.replaceByVariable(constant, variable);
			count += bcount;		
		}
		return count;
	}
	
	protected void replaceNearlyAllConstantsByVariables() {
		int counter = 0;
		for (Atom atom : body) {
			counter++;
			if (counter == body.size()) break;
			if (atom.isLeftC()) {
				String c = atom.getLeft();
				this.replaceByVariable(c, Rule.variables[this.nextFreeVariable]);
				this.nextFreeVariable++;
			}
			if (atom.isRightC()) {
				String c = atom.getRight();
				this.replaceByVariable(c, Rule.variables[this.nextFreeVariable]);
				this.nextFreeVariable++;
			}
		}
	}
	
	
	protected void replaceAllConstantsByVariables() {
		for (Atom atom : body) {
			if (atom.isLeftC()) {
				String c = atom.getLeft();
				this.replaceByVariable(c, Rule.variables[this.nextFreeVariable]);
				this.nextFreeVariable++;
			}
			if (atom.isRightC()) {
				String c = atom.getRight();
				this.replaceByVariable(c, Rule.variables[this.nextFreeVariable]);
				this.nextFreeVariable++;
			}
		}
	}


	
	public HashSet<String> computeTailResults(String head, TripleSet ts) {
		System.err.println("method not available for an untyped rule");
		return null;
	}



	@Override
	public HashSet<String> computeHeadResults(String tail, TripleSet ts) {
		System.err.println("method not available for an untyped rule");
		return null;
	}


	@Override
	public void computeScores(TripleSet ts) {
		System.err.println("method not available for an untyped rule");
		
	}
	
	public boolean isPredictedX(String leftValue, String rightValue, Triple forbidden, TripleSet ts) {
		System.err.println("method not available for an untyped rule");
		return false;
	}
	
	public boolean isRefinable() {
		return false;
	}
	
	
	public Triple getRandomValidPrediction(TripleSet ts) {
		System.err.println("method not available for an untyped rule");
		return null;
	}
	
	public Triple getRandomInvalidPrediction(TripleSet ts) {
		System.err.println("method not available for an untyped rule");
		return null;
	}
	
	public ArrayList<Triple> getPredictions(TripleSet ts) {
		System.err.println("method not available for an untyped rule");
		return null;
	}
	
	
	public boolean isSingleton(TripleSet triples) {
		// does nit really make sense for this type
		return false;
	}


	@Override
	public HashSet<Triple> getTripleExplanation(String xValue, String yValue, HashSet<Triple> excludedTriples, TripleSet triples) {
		System.err.println("Your are asking for a triple explanation using an untyped rule. Such a rule cannot explain anything.");
		return null;
	}


	@Override
	public int[] computeScores(Rule that, TripleSet triples) {
		System.err.println("method not available for an untyped rule");
		return new int[2];
	}

	
	





}
