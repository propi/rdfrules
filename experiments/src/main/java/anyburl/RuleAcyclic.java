package anyburl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public abstract class RuleAcyclic extends Rule {
	
	public RuleAcyclic(RuleUntyped r) {
		super(r);
	}
	

	public HashSet<String> computeTailResults(String head, TripleSet ts) {
		HashSet<String> resultSet = new HashSet<String>();
		if (this.isXRule()) {
			if (this.head.getRight().equals(head)) return resultSet;
			HashSet<String> previousValues = new HashSet<String>();
			previousValues.add(head);
			previousValues.add(this.head.getRight());
			if (this.isBodyTrueAcyclic("X", head, 0, previousValues, ts)) {
				resultSet.add(this.head.getRight());
				return resultSet;
			}
		}
		else {
			if (this.head.getLeft().equals(head)) {
				this.computeValuesReversed("Y", resultSet, ts);
				return resultSet;
			}
		}
		return resultSet;
	}
	
	/*
	public PriorityQueue<Candidate> computePTailResults(String head, TripleSet ts) {
		Timer count = new Timer();
		PriorityQueue<Candidate> resultSet = new PriorityQueue<Candidate>();
		if (this.isXRule()) {
			if (this.head.getRight().equals(head)) return resultSet;
			HashSet<String> previousValues = new HashSet<String>();
			previousValues.add(head);
			previousValues.add(this.head.getRight());
			// TODO fix P here
			if (this.isBodyTrueAcyclic("X", head, 0, previousValues, ts)) {
				// resultSet.add(this.head.getRight());
				return resultSet;
			}
		}
		else {
			if (this.head.getLeft().equals(head)) {
				this.computePValuesReversed(1.0, "Y", resultSet, ts, count);
				return resultSet;
			}
		}
		return resultSet;
	}
	*/

	public HashSet<String> computeHeadResults(String tail, TripleSet ts) {
		HashSet<String> resultSet = new HashSet<String>();
		if (this.isYRule()) {
			if (this.head.getLeft().equals(tail)) return resultSet;
			HashSet<String> previousValues = new HashSet<String>();
			previousValues.add(tail);
			previousValues.add(this.head.getLeft());
			if (this.isBodyTrueAcyclic("Y", tail, 0, previousValues, ts)) {
				resultSet.add(this.head.getLeft());
				return resultSet;
			}
		}
		else if (this.isXRule()) {
			if (this.head.getRight().equals(tail)) {
				this.computeValuesReversed("X", resultSet, ts);	
				return resultSet;
			}
		}
		return resultSet;
	}
	
	
	@Override
	public void computeScores(TripleSet triples) {
		if (this.isXRule()) {
			HashSet<String> xvalues = new HashSet<String>();
			// if (Settings.BEAM_NOT_DFS) this.beamValuesReversed("X", xvalues, triples);
			//else {
				this.computeValuesReversed("X", xvalues, triples);
			// }
			int predicted = 0, correctlyPredicted = 0;
			for (String xvalue : xvalues) {
				predicted++;
				if (triples.isTrue(xvalue, this.head.getRelation(), this.head.getRight())) correctlyPredicted++;
			}
			this.predicted = predicted;
			this.correctlyPredicted = correctlyPredicted;
			this.confidence = (double)correctlyPredicted / (double)predicted;
		}
		else {
			HashSet<String> yvalues = new HashSet<String>();
			
			// if (Settings.BEAM_NOT_DFS) this.beamValuesReversed("Y", yvalues, triples);
			// else {
				this.computeValuesReversed("Y", yvalues, triples);
			// }
			
			int predicted = 0, correctlyPredicted = 0;
			for (String yvalue : yvalues) {
				predicted++;
				if (triples.isTrue(this.head.getLeft(), this.head.getRelation(), yvalue)) correctlyPredicted++;
			}
			this.predicted = predicted;
			this.correctlyPredicted = correctlyPredicted;
			this.confidence = (double)correctlyPredicted / (double)predicted;
		}
	}
	
	
	@Override
	public int[] computeScores(Rule that, TripleSet triples) {
		int[] scores = new int[2];
		int predictedBoth = 0;
		int correctlyPredictedBoth = 0;
		if (this.isXRule()) {
			HashSet<String> xvalues = new HashSet<String>();
			String yvalue = this.getHead().getRight();
			this.computeValuesReversed("X", xvalues, triples);
			for (String xvalue : xvalues) {
				// System.out.println("... checking " + xvalue + " ~relation~ " + yvalue);
				HashSet<Triple> explanation = that.getTripleExplanation(xvalue, yvalue, new HashSet<Triple>(), triples);
				if (explanation != null && explanation.size() > 0) {
					predictedBoth++;
					if (triples.isTrue(xvalue, this.head.getRelation(), yvalue)) correctlyPredictedBoth++;
				}
			}
		}
		else {
			HashSet<String> yvalues = new HashSet<String>();
			String xvalue = this.getHead().getLeft();
			this.computeValuesReversed("Y", yvalues, triples);
			for (String yvalue : yvalues) {
				// System.out.print("... checking " + xvalue + " ~relation~ " + yvalue);
				HashSet<Triple> explanation = that.getTripleExplanation(xvalue, yvalue, new HashSet<Triple>(), triples);
				if (explanation != null && explanation.size() > 0) {
					predictedBoth++;
					// System.out.println("... BOTH");
					if (triples.isTrue(xvalue, this.head.getRelation(), yvalue)) correctlyPredictedBoth++;
				}
				else {
					// System.out.println("... not by both");
				}
			}
		}
		scores[0] = predictedBoth;
		scores[1] = correctlyPredictedBoth;
		return scores;
	}
	
	
	


	
	// the head is not used here (its only about using the body as extension
	public boolean isPredictedX(String leftValue, String rightValue, Triple forbidden, TripleSet ts) {
		if (forbidden == null) {
			if (this.isXRule()) {
				HashSet<String> previousValues = new HashSet<String>();
				previousValues.add(leftValue);
				return this.isBodyTrueAcyclic("X", leftValue, 0, previousValues, ts);
			}
			else {
				HashSet<String> previousValues = new HashSet<String>();
				previousValues.add(rightValue);
				return this.isBodyTrueAcyclic("Y", rightValue, 0, previousValues, ts);
			}
		}
		else {
			if (this.isXRule()) {
				HashSet<String> previousValues = new HashSet<String>();
				previousValues.add(leftValue);
				return this.isBodyTrueAcyclicX("X", leftValue, 0, forbidden, previousValues, ts);
			}
			else {
				HashSet<String> previousValues = new HashSet<String>();
				previousValues.add(rightValue);
				return this.isBodyTrueAcyclicX("Y", rightValue, 0, forbidden, previousValues, ts);
			}
		}
	}
	
	
	// *** PRIVATE PLAYGROUND **** 
	
	
	protected boolean isBodyTrueAcyclic(String variable, String value, int bodyIndex, HashSet<String> previousValues, TripleSet triples) {
		Atom atom = this.body.get(bodyIndex);
		boolean headNotTail = atom.getLeft().equals(variable);
		// the current atom is the last
		if (this.body.size() -1 == bodyIndex) {
			boolean constant = headNotTail ? atom.isRightC() : atom.isLeftC();
			// get groundings
			// fixed by a constant
			if (constant) {
				String constantValue = headNotTail ? atom.getRight() : atom.getLeft();
				if (previousValues.contains(constantValue) && !constantValue.equals(this.head.getConstant())) return false;
				if (headNotTail) {
					return triples.isTrue(value, atom.getRelation(), constantValue);
				}
				else {
					return triples.isTrue(constantValue, atom.getRelation(),value);
				}
			}
			// existential quantification
			else {
				Set<String> results = triples.getEntities(atom.getRelation(), value, headNotTail);
				for (String r : results) {
					if (!previousValues.contains(r)) return true;
				}
			}
			return false;
		}
		// the current atom is not the last
		else {
			Set<String> results = triples.getEntities(atom.getRelation(), value, headNotTail);
			String nextVariable = headNotTail ? atom.getRight() : atom.getLeft();
			for (String nextValue : results) {
				if (previousValues.contains(nextValue)) continue;
				previousValues.add(nextValue);
				if (isBodyTrueAcyclic(nextVariable, nextValue, bodyIndex+1, previousValues, triples)) {
					return true;
				}
				previousValues.remove(nextValue);
			}
			return false;
		}
	}	
	
	private boolean isBodyTrueAcyclicX(String variable, String value, int bodyIndex, Triple forbidden, HashSet<String> previousValues, TripleSet triples) {
		Atom atom = this.body.get(bodyIndex);
		boolean headNotTail = atom.getLeft().equals(variable);
		// the current atom is the last
		if (this.body.size() -1 == bodyIndex) {
			boolean constant = headNotTail ? atom.isRightC() : atom.isLeftC();
			// get groundings
			// fixed by a constant
			if (constant) {
				String constantValue = headNotTail ? atom.getRight() : atom.getLeft();
				if (previousValues.contains(constantValue) && !constantValue.equals(this.head.getConstant())) return false;
				if (headNotTail) {
					return triples.isTrue(value, atom.getRelation(), constantValue);
				}
				else {
					return triples.isTrue(constantValue, atom.getRelation(),value);
				}
			}
			// existential quantification
			else {
				Set<String> results = triples.getEntities(atom.getRelation(), value, headNotTail);
				for (String r : results) {
					if (!previousValues.contains(r)) return true;
				}
			}
			return false;
		}
		// the current atom is not the last
		else {
			Set<String> results = triples.getEntities(atom.getRelation(), value, headNotTail);
			String nextVariable = headNotTail ? atom.getRight() : atom.getLeft();
			for (String nextValue : results) {
				if (!forbidden.equals(headNotTail, value, atom.getRelation(), nextValue)) {
					if (previousValues.contains(nextValue)) continue;
					previousValues.add(nextValue);
					if (isBodyTrueAcyclicX(nextVariable, nextValue, bodyIndex+1, forbidden, previousValues, triples)) {
						return true;
					}
					previousValues.remove(nextValue);
				}
			}
			return false;
		}
	}
	
	
	
	public void computeValuesReversed(String targetVariable, HashSet<String> targetValues, TripleSet ts) {
		int atomIndex = this.body.size() - 1;
		Atom lastAtom = this.body.get(atomIndex);
		String unboundVariable = this.getUnboundVariable();
		if (unboundVariable == null) {
			boolean nextVarIsLeft;
			if (lastAtom.isLeftC()) nextVarIsLeft = false;
			else nextVarIsLeft = true;
			String constant = lastAtom.getLR(!nextVarIsLeft);
			String nextVariable = lastAtom.getLR(nextVarIsLeft);
			Set<String> values = ts.getEntities(lastAtom.getRelation(), constant, !nextVarIsLeft);
			HashSet<String> previousValues = new HashSet<String>();
			previousValues.add(constant);
			previousValues.add(this.head.getConstant());
			int counter = 0;
			for (String value : values) {
				counter++;
				forwardReversed(nextVariable, value, atomIndex-1, targetVariable, targetValues, ts, previousValues);
				if (!Rule.APPLICATION_MODE && (targetValues.size() >= Settings.SAMPLE_SIZE || counter >= Settings.BEAM_SAMPLING_MAX_BODY_GROUNDING_ATTEMPTS)) return;
				if (Rule.APPLICATION_MODE && targetValues.size() >= Settings.DISCRIMINATION_BOUND) {
					targetValues.clear();
					return;
				}
				
			}
		}
		else {
			boolean nextVarIsLeft;
			if (lastAtom.getLeft().equals(unboundVariable)) nextVarIsLeft = false;
			else nextVarIsLeft = true;
			String nextVariable = lastAtom.getLR(nextVarIsLeft);
			ArrayList<Triple> triples = ts.getTriplesByRelation(lastAtom.getRelation());
			int counter = 0;
			for (Triple t : triples) {
				counter++;
				String value = t.getValue(nextVarIsLeft);
				HashSet<String> previousValues = new HashSet<String>();
				String previousValue = t.getValue(!nextVarIsLeft);				
				previousValues.add(previousValue);
				previousValues.add(this.head.getConstant());
				forwardReversed(nextVariable, value, atomIndex-1, targetVariable, targetValues, ts, previousValues);
				if (!Rule.APPLICATION_MODE && (targetValues.size() >= Settings.SAMPLE_SIZE || counter >= Settings.BEAM_SAMPLING_MAX_BODY_GROUNDING_ATTEMPTS)) return;
				if (Rule.APPLICATION_MODE && targetValues.size() >= Settings.DISCRIMINATION_BOUND) {
					targetValues.clear();
					return;
				}
				
			}
		}
	}
	
	
	public void beamValuesReversed(String targetVariable, HashSet<String> targetValues, TripleSet ts) {
		int atomIndex = this.body.size() - 1;
		Atom lastAtom = this.body.get(atomIndex);
		if (this.getGroundingsLastAtom(ts) < Settings.AC_MIN_NUM_OF_LAST_ATOM_GROUNDINGS) return;
		
		String unboundVariable = this.getUnboundVariable();
		if (unboundVariable == null) {
			boolean nextVarIsLeft;
			if (lastAtom.isLeftC()) nextVarIsLeft = false;
			else nextVarIsLeft = true;
			String constant = lastAtom.getLR(!nextVarIsLeft);
			String nextVariable = lastAtom.getLR(nextVarIsLeft);
			
			String value;
			int counter = 0;
			while ((value = ts.getRandomEntity(lastAtom.getRelation(), constant, !nextVarIsLeft)) != null) {
				counter++;
				HashSet<String> previousValues = new HashSet<String>();
				previousValues.add(constant);
				previousValues.add(this.head.getConstant());
				
				String targetValue = beamForwardReversed(nextVariable, value, atomIndex-1, targetVariable, ts, previousValues);
				if (targetValue != null) targetValues.add(targetValue);
				if (counter > Settings.SAMPLE_SIZE) return;
			}
		}
		else {
			boolean nextVarIsLeft;
			if (lastAtom.getLeft().equals(unboundVariable)) nextVarIsLeft = false;
			else nextVarIsLeft = true;
			String nextVariable = lastAtom.getLR(nextVarIsLeft);
			Triple t;
			int counter = 0;
			while ((t = ts.getRandomTripleByRelation(lastAtom.getRelation())) != null) {
				counter++;
				String value = t.getValue(nextVarIsLeft);
				HashSet<String> previousValues = new HashSet<String>();
				String previousValue = t.getValue(!nextVarIsLeft);				
				previousValues.add(previousValue);
				previousValues.add(this.head.getConstant());
				String targetValue = beamForwardReversed(nextVariable, value, atomIndex-1, targetVariable, ts, previousValues);
				if (targetValue != null) targetValues.add(targetValue);
				if (counter > Settings.SAMPLE_SIZE) return;
			}
		}
	}
	
	
	/*
	public void computePValuesReversed(double p, String targetVariable, PriorityQueue<Candidate> targetValues, TripleSet ts, Timer count) {
		int atomIndex = this.body.size() - 1;
		Atom lastAtom = this.body.get(atomIndex);
		String unboundVariable = this.getUnboundVariable();
		if (unboundVariable == null) {
			boolean nextVarIsLeft;
			if (lastAtom.isLeftC()) nextVarIsLeft = false;
			else nextVarIsLeft = true;
			String constant = lastAtom.getLR(!nextVarIsLeft);
			String nextVariable = lastAtom.getLR(nextVarIsLeft);
			Set<String> values = ts.getEntities(lastAtom.getRelation(), constant, !nextVarIsLeft);
			HashSet<String> previousValues = new HashSet<String>();
			previousValues.add(constant);
			previousValues.add(this.head.getConstant());
			for (String value : values) {
				if (count.timeOut()) throw new TimeOutException();;
				forwardPReversed(p, nextVariable, value, atomIndex-1, targetVariable, targetValues, ts, previousValues);
				if (!Rule.APPLICATION_MODE && targetValues.size() >= Settings.SAMPLE_SIZE) return;
				if (Rule.APPLICATION_MODE && targetValues.size() >= Settings.DISCRIMINATION_BOUND) {
					targetValues.clear();
					return;
				}
				
			}
		}
		else {
			boolean nextVarIsLeft;
			if (lastAtom.getLeft().equals(unboundVariable)) nextVarIsLeft = false;
			else nextVarIsLeft = true;
			String nextVariable = lastAtom.getLR(nextVarIsLeft);
			ArrayList<Triple> triples = ts.getTriplesByRelation(lastAtom.getRelation());
			for (Triple t : triples) {
				if (count.timeOut()) throw new TimeOutException();
				String value = t.getValue(nextVarIsLeft);
				HashSet<String> previousValues = new HashSet<String>();
				String previousValue = t.getValue(!nextVarIsLeft);				
				previousValues.add(previousValue);
				previousValues.add(this.head.getConstant());
				forwardPReversed(p, nextVariable, value, atomIndex-1, targetVariable, targetValues, ts, previousValues);
				if (!Rule.APPLICATION_MODE && targetValues.size() >= Settings.SAMPLE_SIZE) return;
				if (Rule.APPLICATION_MODE && targetValues.size() >= Settings.DISCRIMINATION_BOUND) {
					targetValues.clear();
					return;
				}
				
			}
		}
	}
	*/

	

	
	private void forwardReversed(String variable, String value, int bodyIndex, String targetVariable, HashSet<String> targetValues, TripleSet ts, HashSet<String> previousValues) {
		if (previousValues.contains(value)) return;
		if (bodyIndex < 0) {
			targetValues.add(value);	
		}
		else {
			HashSet<String> currentValues = new HashSet<String>();
			currentValues.add(value);
			currentValues.addAll(previousValues); // ADDING THIS SINGLE LINE WAS I SUPER IMPORTANT BUG FIX
			Atom atom = this.body.get(bodyIndex);
			boolean nextVarIsLeft = false;
			if (atom.getLeft().equals(variable)) nextVarIsLeft = false;
			else nextVarIsLeft = true;
			String nextVariable = atom.getLR(nextVarIsLeft);
			HashSet<String> nextValues = new HashSet<String>();			
			if (!Rule.APPLICATION_MODE && targetValues.size() >= Settings.SAMPLE_SIZE) return;
			nextValues.addAll(ts.getEntities(atom.getRelation(), value, !nextVarIsLeft));
			for (String nextValue : nextValues) {
				forwardReversed(nextVariable, nextValue, bodyIndex-1, targetVariable, targetValues, ts, currentValues);
			}
		}
	}
	
	private String beamForwardReversed(String variable, String value, int bodyIndex, String targetVariable, TripleSet ts, HashSet<String> previousValues) {
		if (previousValues.contains(value)) return null;

		if (bodyIndex < 0) return value;
		else {
			previousValues.add(value);
			Atom atom = this.body.get(bodyIndex);
			boolean nextVarIsLeft = false;
			if (atom.getLeft().equals(variable)) nextVarIsLeft = false;
			else nextVarIsLeft = true;
			String nextVariable = atom.getLR(nextVarIsLeft);
			String nextValue = ts.getRandomEntity(atom.getRelation(), value, !nextVarIsLeft);
			if (nextValue != null) {
				return beamForwardReversed(nextVariable, nextValue, bodyIndex-1, targetVariable, ts, previousValues);
			}
			else {
				return null;
			}
		}
	}
	
	/*
	private void forwardPReversed(double p, String variable, String value, int bodyIndex, String targetVariable, PriorityQueue<Candidate> targetValues, TripleSet ts, HashSet<String> previousValues) {
		if (previousValues.contains(value)) return;
		if (bodyIndex < 0) {
			Candidate c = new Candidate(value, this.getAppliedConfidence() * p);
			targetValues.add(c);
		}
		else {
			HashSet<String> currentValues = new HashSet<String>();
			currentValues.add(value);
			currentValues.addAll(previousValues); // ADDING THIS SINGLE LINE WAS I SUPER IMPORTANT BUG FIX
			Atom atom = this.body.get(bodyIndex);
			boolean nextVarIsLeft = false;
			if (atom.getLeft().equals(variable)) nextVarIsLeft = false;
			else nextVarIsLeft = true;
			String nextVariable = atom.getLR(nextVarIsLeft);
			HashSet<String> nextValues = new HashSet<String>();			
			if (!Rule.APPLICATION_MODE && targetValues.size() >= Settings.SAMPLE_SIZE) return;
			nextValues.addAll(ts.getEntities(atom.getRelation(), value, !nextVarIsLeft));
			for (String nextValue : nextValues) {
				forwardPReversed(p, nextVariable, nextValue, bodyIndex-1, targetVariable, targetValues, ts, currentValues);
			}
		}
	}
	*/
	
	
	
	protected abstract String getUnboundVariable();
	
	public boolean isRefinable() {
		return false;
	}
	
	public Triple getRandomValidPrediction(TripleSet ts) {
		ArrayList<Triple> validPredictions = this.getPredictions(ts, 1);
		if (validPredictions == null || validPredictions.size() == 0) return null;
		int index = rand.nextInt(validPredictions.size());
		return validPredictions.get(index);		
	}
	
	public Triple getRandomInvalidPrediction(TripleSet ts) {
		ArrayList<Triple> validPredictions = this.getPredictions(ts, -1);
		if (validPredictions == null || validPredictions.size() == 0) return null;
		int index = rand.nextInt(validPredictions.size());
		return validPredictions.get(index);		
	}
	
	public ArrayList<Triple> getPredictions(TripleSet ts) {
		return this.getPredictions(ts, 0);
	}
	
	/**
	 * 
	 * @param ts
	 * @param valid 1 = valid; -1 = invalid; 0 valid/invalid does not matter
	 * @return
	 */
	protected ArrayList<Triple> getPredictions(TripleSet ts, int valid) {
		ArrayList<Triple> materialized = new ArrayList<Triple>();
		HashSet<String> resultSet = new HashSet<String>();
		if (this.isXRule()) {
			resultSet = this.computeHeadResults(this.getHead().getRight(), ts);
		}
		else {
			resultSet = this.computeTailResults(this.getHead().getLeft(), ts);
		}
		for (String v : resultSet) {
			Triple t;
			if (this.isXRule()) {
				t = new Triple(v, this.getTargetRelation(), this.getHead().getRight());
			}
			else {
				t = new Triple( this.getHead().getLeft(), this.getTargetRelation(), v);
			}
			if (valid == 1) {
				if (ts.isTrue(t)) materialized.add(t);
			}
			else if (valid == -1) {
				if (!ts.isTrue(t)) materialized.add(t);
			}
			else {
				materialized.add(t);
			}
			
			// System.out.println(t + " due to: " +  this);
		}
		return materialized;
	}
	
	public abstract int getGroundingsLastAtom(TripleSet triples);
	
	/**
	 * First replaces all atoms by deep copies of these atoms to avoid that references from the outside are affected by follow up changes.
	 * Then corrects a rule which uses X in the head at the Y position by replacing X by Y in the head as well as all occurrences
	 */
	public void detachAndPolish() {
		Atom h = this.head.createCopy();
		this.head = h;
		this.body.detach();
		if (this.head.getRight().equals("X")) {
			this.head.setRight("Y");
			for (int i = 0; i < this.bodysize(); i++) {
				Atom a = this.getBodyAtom(i);
				a.replace("X", "Y");
			}
		}
	}




	
}
