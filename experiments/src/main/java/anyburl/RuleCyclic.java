package anyburl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class RuleCyclic extends Rule  {

	public RuleCyclic(RuleUntyped r) {
		super(r);
		// modify it to its canonical form
		
		
		if (this.body.get(0).contains("Y") && this.bodysize() > 1) {
			// if (this.bodysize() > 3) System.out.println("before: " + this);
			for (int i = 0; i <= (this.bodysize() / 2) - 1; i++) {
				int j = (this.bodysize() - i) - 1;
				Atom atom_i = this.body.get(i);
				Atom atom_j = this.body.get(j);
				this.body.set(i, atom_j);
				this.body.set(j, atom_i);
			}
			this.body.normalizeVariableNames();
			// if (this.bodysize() > 3) System.out.println("after: " + this);
		}

	}
	
	
	public TripleSet materialize(TripleSet trainingSet) {
		//TripleSet materializedRule = new TripleSet();
	
		return null;
	}

	public HashSet<String> computeTailResults(String head, TripleSet ts) {
		HashSet<String> results = new HashSet<String>();
		//if (Settings.BEAM_NOT_DFS) {
		//	results = this.beamPGBodyCyclic("X", "Y", head, 0, true, ts);
		//}
		//else {
			this.getCyclic("X", "Y", head, 0, true, ts,  new HashSet<String>(), results);
		//}
		return results;
	}

	public HashSet<String> computeHeadResults(String tail, TripleSet ts) {
		HashSet<String> results = new HashSet<String>();
		// if (Settings.BEAM_NOT_DFS) {
		//	results = this.beamPGBodyCyclic("Y", "X", tail, this.bodysize() - 1, false, ts);
		//}
		//else {
			this.getCyclic("Y", "X", tail, this.bodysize() - 1, false, ts,  new HashSet<String>(), results);
		//}
		return results;
	}
	
	@Override
	public void computeScores(TripleSet triples) {
		// X is given in first body atom
		SampledPairedResultSet xypairs;
		SampledPairedResultSet xypairsReverse;
		
		if (this.body.get(0).contains("X")) {
			if (Settings.BEAM_NOT_DFS) {	
				if (Settings.BEAM_TYPE_EDIS) {
					xypairs= beamBodyCyclicEDIS("X", "Y", triples);
					xypairsReverse = beamBodyCyclicReverseEDIS("X", "Y", triples);
				}
				else {
					xypairs = beamBodyCyclic("X", "Y", triples);
					xypairsReverse = beamBodyCyclicReverse("X", "Y", triples);
				}
			}
			else {
				xypairs = groundBodyCyclic("X", "Y", triples);
				xypairsReverse = new SampledPairedResultSet();
			}
		}
		else {
			if (Settings.BEAM_NOT_DFS) {			
				if (Settings.BEAM_TYPE_EDIS) {
					xypairs = beamBodyCyclicEDIS("Y", "X", triples);
					xypairsReverse = beamBodyCyclicReverseEDIS("Y", "X", triples);
				}
				else {
					xypairs = beamBodyCyclic("Y", "X", triples);
					xypairsReverse = beamBodyCyclicReverse("Y", "X", triples);
				}
			}
			else {
				xypairs = groundBodyCyclic("Y", "X", triples);
				xypairsReverse = new SampledPairedResultSet();
			}
		}
		
		
		int predictedAll = 0;
		int correctlyPredictedAll = 0;
		// body groundings for head prediction	
		int correctlyPredicted = 0;
		int predicted = 0;
		for (String key : xypairsReverse.getValues().keySet()) {
			for (String value : xypairsReverse.getValues().get(key)) {	
				predicted++;
				if (triples.isTrue(key, this.head.getRelation(), value)) correctlyPredicted++;		
			}
		}
		
		predictedAll += predicted;
		correctlyPredictedAll += correctlyPredicted;
		
		correctlyPredicted = 0;
		predicted = 0;
		
		for (String key : xypairs.getValues().keySet()) {
			for (String value : xypairs.getValues().get(key)) {	
				predicted++;
				if (triples.isTrue(key, this.head.getRelation(), value)) correctlyPredicted++;		
			}
		}
	
		
		predictedAll += predicted;
		correctlyPredictedAll += correctlyPredicted;
		
	
		
		this.predicted = predictedAll ;
		this.correctlyPredicted = correctlyPredictedAll;
		this.confidence = (double)this.correctlyPredicted / (double)this.predicted;

	}	
	

	public int[] computeScores(Rule that, TripleSet triples) {
		int[] scores = new int[2];
		if (!this.getTargetRelation().equals(that.getTargetRelation())) {
			System.err.print("your are computing the scores of a concjuntion of two rules with different target relations, that does not make sense");
			return scores;
		}
		SampledPairedResultSet xypairs;
		SampledPairedResultSet xypairsReverse;
		
		if (this.body.get(0).contains("X")) {
			if (Settings.BEAM_NOT_DFS) {	
				if (Settings.BEAM_TYPE_EDIS) {
					xypairs= beamBodyCyclicEDIS("X", "Y", triples);
					xypairsReverse = beamBodyCyclicReverseEDIS("X", "Y", triples);
				}
				else {
					xypairs = beamBodyCyclic("X", "Y", triples);
					xypairsReverse = beamBodyCyclicReverse("X", "Y", triples);
				}
			}
			else {
				xypairs = groundBodyCyclic("X", "Y", triples);
				xypairsReverse = xypairs;
			}
		}
		else {
			if (Settings.BEAM_NOT_DFS) {			
				if (Settings.BEAM_TYPE_EDIS) {
					xypairs = beamBodyCyclicEDIS("Y", "X", triples);
					xypairsReverse = beamBodyCyclicReverseEDIS("Y", "X", triples);
				}
				else {
					xypairs = beamBodyCyclic("Y", "X", triples);
					xypairsReverse = beamBodyCyclicReverse("Y", "X", triples);
				}
			}
			else {
				xypairs = groundBodyCyclic("Y", "X", triples);
				xypairsReverse = xypairs;
			}
		}
		
		
		int predictedBoth = 0;
		int correctlyPredictedBoth = 0;
		
		
		for (String key : xypairs.getValues().keySet()) {
			for (String value : xypairs.getValues().get(key)) {	
				HashSet<Triple> explanation = that.getTripleExplanation(key, value, new HashSet<Triple>(), triples);
				if (explanation != null && explanation.size() > 0) {
					predictedBoth++;
					if (triples.isTrue(key, this.head.getRelation(), value)) correctlyPredictedBoth++;		
				}
			}
		}
		for (String key : xypairsReverse.getValues().keySet()) {
			for (String value : xypairsReverse.getValues().get(key)) {	
				// change this to that
				HashSet<Triple> explanation = that.getTripleExplanation(key, value, new HashSet<Triple>(), triples);
				if (explanation != null && explanation.size() > 0) {
					predictedBoth++;
					if (triples.isTrue(key, this.head.getRelation(), value)) correctlyPredictedBoth++;		
				}
			}
		}
		
		
		
		scores[0] = predictedBoth;
		scores[1] = correctlyPredictedBoth;
		
		
		return scores;
	}
	
	/*
	public int estimateAllBodyGroundings() {
		for (int i = 0; i < this.bodysize(); i++) {
			
			Atom atom = this.getBodyAtom(i);
			atom.get
			
			
			
			
		}
		
	}
	*/
	
	
	
	/**
	 * The new implementation if the sample based computation of the scores.
	 * Samples completely random attempts to create a beam over the body.
	 * 
	 * @param triples
	 */
	/*
	public void beamScores(TripleSet triples) {
		long startScoring = System.currentTimeMillis();
		// X is given in first body atom
		SampledPairedResultSet xypairs;
		if (this.body.get(0).contains("X")) {
			xypairs = beamBodyCyclic("X", "Y", triples);
		}
		else {
			xypairs = beamBodyCyclic("Y", "X", triples);
		}
		// body groundings		
		int correctlyPredicted = 0;
		int predicted = 0;
		for (String key : xypairs.getValues().keySet()) {
			for (String value : xypairs.getValues().get(key)) {	
				if (Settings.PREDICT_ONLY_UNCONNECTED) {
					Set<String> links = triples.getRelations(key, value);
					Set<String> invLinks = triples.getRelations(value, key);
					if (invLinks.size() > 0) continue;
					if (!links.contains(this.head.getRelation()) && links.size() > 0) continue;
					if (links.contains(this.head.getRelation()) && links.size() > 1) continue;
				}
				predicted++;
				if (triples.isTrue(key, this.head.getRelation(), value)) {
					correctlyPredicted++;		
				}
			}
		}
		this.predicted = predicted;
		this.correctlyPredicted = correctlyPredicted;
		this.confidence = (double)correctlyPredicted / (double)predicted;
		
	}
	*/	
	
	public Triple getRandomValidPrediction(TripleSet triples) {
		ArrayList<Triple> validPredictions = this.getPredictions(triples, 1);
		if (validPredictions == null || validPredictions.size() == 0) return null;
		if (validPredictions.size() == 0) return null;
		int index = rand.nextInt(validPredictions.size());
		return validPredictions.get(index);
	}
	
	public Triple getRandomInvalidPrediction(TripleSet triples) {
		ArrayList<Triple> validPredictions = this.getPredictions(triples, -1);
		if (validPredictions == null || validPredictions.size() == 0) return null;
		if (validPredictions.size() == 0) return null;
		int index = rand.nextInt(validPredictions.size());
		return validPredictions.get(index);
	}
	
	
	public ArrayList<Triple> getPredictions(TripleSet triples) {
		return this.getPredictions(triples, 0);
	}
	
	
	/**
	* 
	* @param triples
	* @param valid 1= must be valid; -1 = must be invalid; 0 = valid and invalid is okay
	* @return
	*/
	protected ArrayList<Triple> getPredictions(TripleSet triples, int valid) {
		SampledPairedResultSet xypairs;
		if (this.body.get(0).contains("X")) xypairs = groundBodyCyclic("X", "Y", triples);
		else xypairs = groundBodyCyclic("Y", "X", triples);
		ArrayList<Triple> predictions = new ArrayList<Triple>();
		for (String key : xypairs.getValues().keySet()) {
			for (String value : xypairs.getValues().get(key)) {
				if (valid == 1) {
					if (triples.isTrue(key, this.head.getRelation(), value)) {
						Triple validPrediction = new Triple(key, this.head.getRelation(), value);
						predictions.add(validPrediction);
					}
				}
				else if (valid == -1) {
					if (!triples.isTrue(key, this.head.getRelation(), value)) {
						Triple invalidPrediction = new Triple(key, this.head.getRelation(), value);
						predictions.add(invalidPrediction);
					}
				}
				else {
					Triple validPrediction = new Triple(key, this.head.getRelation(), value);
					predictions.add(validPrediction);						
				}
			}
		}
		return predictions;
	}
	

	public boolean isPredictedX(String leftValue, String rightValue, Triple forbidden, TripleSet ts) {
		System.err.println("method not YET available for an extended/refinde rule");
		return false;
	}
	
	// *** PRIVATE PLAYGROUND **** 

	
	
	
	private void getCyclic(String currentVariable, String lastVariable, String value, int bodyIndex, boolean direction, TripleSet triples, HashSet<String> previousValues, HashSet<String> finalResults) {
		
		
		if (Rule.APPLICATION_MODE && finalResults.size() >= Settings.DISCRIMINATION_BOUND) {
			finalResults.clear();
			return;
		}
		// XXX if (!Rule.APPLICATION_MODE && finalResults.size() >= Settings.SAMPLE_SIZE) return;
		// check if the value has been seen before as grounding of another variable
		Atom atom = this.body.get(bodyIndex);
		boolean headNotTail = atom.getLeft().equals(currentVariable);
		if (previousValues.contains(value)) return;		
		// the current atom is the last
		if ((direction == true && this.body.size() -1 == bodyIndex) || (direction == false && bodyIndex == 0)) {
			// get groundings
			for (String v : triples.getEntities(atom.getRelation(), value, headNotTail)) {
				if (!previousValues.contains(v) && !value.equals(v)) finalResults.add(v);
			}
			return;
		}
		// the current atom is not the last
		else {
			Set<String> results = triples.getEntities(atom.getRelation(), value, headNotTail);
			if (results.size() > Settings.BRANCHINGFACTOR_BOUND && Settings.DFS_SAMPLING_ON == true) return;
			String nextVariable = headNotTail ? atom.getRight() : atom.getLeft();
			HashSet<String> currentValues = new HashSet<String>();
			currentValues.addAll(previousValues);
			if (Settings.OI_CONSTRAINTS_ACTIVE) currentValues.add(value);
			// int i = 0;
			
			for (String nextValue : results) {
				// XXX if (!Rule.APPLICATION_MODE && i >= Settings.SAMPLE_SIZE) break;
				int updatedBodyIndex = (direction) ? bodyIndex + 1 : bodyIndex - 1;
				this.getCyclic(nextVariable, lastVariable, nextValue, updatedBodyIndex, direction, triples, currentValues, finalResults);
				// i++;
			}
			return;
		}
	}
	
	
	
	
	private SampledPairedResultSet groundBodyCyclic(String firstVariable, String lastVariable, TripleSet triples) {
		return groundBodyCyclic(firstVariable, lastVariable, triples, Settings.DFS_SAMPLING_ON);
	}
	
	private SampledPairedResultSet groundBodyCyclic(String firstVariable, String lastVariable, TripleSet triples, boolean samplingOn) {
		SampledPairedResultSet groundings = new SampledPairedResultSet();
		Atom atom = this.body.get(0);
		boolean headNotTail = atom.getLeft().equals(firstVariable);
		ArrayList<Triple> rtriples = triples.getTriplesByRelation(atom.getRelation());
		int counter = 0;
		for (Triple t : rtriples) {
			counter++;
			HashSet<String> lastVariableGroundings = new HashSet<String>();
			// the call itself
			this.getCyclic(firstVariable, lastVariable, t.getValue(headNotTail), 0, true, triples, new HashSet<String>(), lastVariableGroundings);
			if (lastVariableGroundings.size() > 0) {
				if (firstVariable.equals("X")) {
					groundings.addKey(t.getValue(headNotTail));
					for (String lastVariableValue : lastVariableGroundings) {
						groundings.addValue(lastVariableValue);
					}
				}
				else {
					for (String lastVariableValue : lastVariableGroundings) {
						groundings.addKey(lastVariableValue);
						groundings.addValue(t.getValue(headNotTail));
					}
				}
			}
			if ((counter >  Settings.SAMPLE_SIZE || groundings.size() > Settings.SAMPLE_SIZE) && samplingOn) {
				break;
			}
		}
		return groundings;
	}
	
	
	private SampledPairedResultSet beamBodyCyclic(String firstVariable, String lastVariable, TripleSet triples) {
		SampledPairedResultSet groundings = new SampledPairedResultSet();
		Atom atom = this.body.get(0);
		boolean headNotTail = atom.getLeft().equals(firstVariable);
		Triple t;
		int attempts = 0;
		int repetitions = 0;
		while ((t = triples.getRandomTripleByRelation(atom.getRelation())) != null) {
			attempts++;
			String lastVarGrounding = this.beamCyclic(firstVariable, t.getValue(headNotTail), 0, true, triples, new HashSet<String>());
			if (lastVarGrounding != null) {
				if (firstVariable.equals("X")) {
					groundings.addKey(t.getValue(headNotTail));
					if (groundings.addValue(lastVarGrounding)) repetitions = 0;
					else repetitions++;
				}
				else {
					groundings.addKey(lastVarGrounding);
					if (groundings.addValue(t.getValue(headNotTail))) repetitions = 0;
					else repetitions++;
				}
			}
			if (Settings.BEAM_SAMPLING_MAX_REPETITIONS <= repetitions) break;
			if (Settings.BEAM_SAMPLING_MAX_BODY_GROUNDING_ATTEMPTS <= attempts) break;
			if (Settings.BEAM_SAMPLING_MAX_BODY_GROUNDINGS <= groundings.size()) break;
		}
		return groundings;
	}
	
	
	private SampledPairedResultSet beamBodyCyclicEDIS(String firstVariable, String lastVariable, TripleSet triples) {
		SampledPairedResultSet groundings = new SampledPairedResultSet();
		Atom atom = this.body.get(0);
		boolean headNotTail = atom.getLeft().equals(firstVariable);
		int repetitions = 0;
		ArrayList<String> entities = triples.getNRandomEntitiesByRelation(atom.getRelation(), headNotTail, Settings.BEAM_SAMPLING_MAX_BODY_GROUNDING_ATTEMPTS);
		for (String e : entities) {
			String lastVarGrounding = this.beamCyclic(firstVariable, e, 0, true, triples, new HashSet<String>());
			if (lastVarGrounding != null) {
				if (firstVariable.equals("X")) {
					groundings.addKey(e);
					if (groundings.addValue(lastVarGrounding)) repetitions = 0;
					else repetitions++;
				}
				else {
					groundings.addKey(lastVarGrounding);
					if (groundings.addValue(e)) repetitions = 0;
					else repetitions++;
				}
			}
			if (Settings.BEAM_SAMPLING_MAX_REPETITIONS <= repetitions) break;
			if (Settings.BEAM_SAMPLING_MAX_BODY_GROUNDINGS <= groundings.size()) break;
		}
		groundings.setChaoEstimate(repetitions);
		return groundings;
	}
	
	// see http://www.vldb.org/conf/1995/P311.PDF
	
	public int getChaoEstimate(int f1, int f2, int d) {
		return (int)(d + ((double)(f1 * f1) / (double)(2.0 * f2)));
	}
	
	
	
	private SampledPairedResultSet beamBodyCyclicReverse(String firstVariable, String lastVariable, TripleSet triples) {
		SampledPairedResultSet groundings = new SampledPairedResultSet();
		Atom atom = this.body.getLast();
		boolean headNotTail = atom.getLeft().equals(lastVariable);
		Triple t;
		int attempts = 0;
		int repetitions = 0;
		while ((t = triples.getRandomTripleByRelation(atom.getRelation())) != null) {
			attempts++;
			String firstVarGrounding = this.beamCyclic(lastVariable, t.getValue(headNotTail), this.bodysize()-1, false, triples, new HashSet<String>());
			// until here
			if (firstVarGrounding != null) {
				if (firstVariable.equals("X")) {
					groundings.addKey(firstVarGrounding);
					if (groundings.addValue(t.getValue(headNotTail))) repetitions = 0;
					else repetitions++;
				}
				else {
					groundings.addKey(t.getValue(headNotTail));
					if (groundings.addValue(firstVarGrounding)) repetitions = 0;
					else repetitions++;
				}
			}
			if (Settings.BEAM_SAMPLING_MAX_REPETITIONS <= repetitions) break;
			if (Settings.BEAM_SAMPLING_MAX_BODY_GROUNDING_ATTEMPTS <= attempts) break;
			if (Settings.BEAM_SAMPLING_MAX_BODY_GROUNDINGS <= groundings.size()) break;
		}
		return groundings;
	}
	
	private SampledPairedResultSet beamBodyCyclicReverseEDIS(String firstVariable, String lastVariable, TripleSet triples) {
		SampledPairedResultSet groundings = new SampledPairedResultSet();
		Atom atom = this.body.getLast();
		boolean headNotTail = atom.getLeft().equals(lastVariable);
		int repetitions = 0;
		ArrayList<String> entities = triples.getNRandomEntitiesByRelation(atom.getRelation(), headNotTail, Settings.BEAM_SAMPLING_MAX_BODY_GROUNDING_ATTEMPTS);
		for (String e : entities) {
			// System.out.println("e="+ e);
			String firstVarGrounding = this.beamCyclic(lastVariable, e, this.bodysize()-1, false, triples, new HashSet<String>());
			if (firstVarGrounding != null) {
				if (firstVariable.equals("X")) {
					groundings.addKey(firstVarGrounding);
					if (groundings.addValue(e)) repetitions = 0;
					else repetitions++;
				}
				else {
					groundings.addKey(e);
					if (groundings.addValue(firstVarGrounding)) repetitions = 0;
					else repetitions++;
				}
			}
			if (Settings.BEAM_SAMPLING_MAX_REPETITIONS <= repetitions) break;
			if (Settings.BEAM_SAMPLING_MAX_BODY_GROUNDINGS <= groundings.size()) break;
		}
		return groundings;
	}
	
	

	
	// (String currentVariable, String lastVariable, TripleSet triples,) {
	/*
	private HashSet<String> beamPGBodyCyclic(String firstVariable, String lastVariable, String value, int bodyIndex, boolean direction, TripleSet triples) {
		HashSet<String> groundings = new HashSet<String>();
		Atom atom = this.body.get(bodyIndex);
		boolean headNotTail = atom.getLeft().equals(firstVariable);
		int attempts = 0;
		int repetitions = 0;
		// System.out.println("startsFine: " + atom.getRelation() + " - " + value + " - " + headNotTail);
		boolean startFine = !triples.getEntities(atom.getRelation(), value, headNotTail).isEmpty();
		//System.out.println("startsFine=" + startFine);
		while (startFine) {
			attempts++;
			String grounding = this.beamCyclic(firstVariable, value, bodyIndex, direction, triples, new HashSet<String>());
			if (grounding != null) {
				if (groundings.add(grounding)) repetitions = 0;
				else repetitions++;
			}
			if (Settings.BEAM_SAMPLING_MAX_REPETITIONS <= repetitions) break;
			if (Settings.BEAM_SAMPLING_MAX_BODY_GROUNDING_ATTEMPTS <= attempts) break;
			if (Settings.BEAM_SAMPLING_MAX_BODY_GROUNDINGS <= groundings.size()) break;
		}
		// System.out.println(this);
		// System.out.println("  => r=" + repetitions + " a=" + attempts + " g=" + groundings.size());
		// System.out.println(Settings.BEAM_SAMPLING_MAX_BODY_GROUNDING_ATTEMPTS);
		// System.out.println(Settings.BEAM_SAMPLING_MAX_BODY_GROUNDINGS);
		
		return groundings;
	}
	*/
	
	
	/**
	 * Tries to create a random grounding for a partially grounded body. 
	 * 
	 * @param currentVariable The name of the current variable for which a value is given.
	 * @param value The value of the current variable.
	 * @param bodyIndex The index of the body atom that we are currently concerned with.
	 * @param direction The direction to search. True means to search from first to last atom, false the opposite direction.
	 * @param triples The data set used for grounding the body
	 * @param previousValues The values that were used as groundings for variables visited already.
	 * @return A grounding for the last variable (or constants). Null if not a full grounding of the body has been constructed.
	 */
	protected String beamCyclic(String currentVariable, String value, int bodyIndex, boolean direction, TripleSet triples, HashSet<String> previousValues) {
		// System.out.println(currentVariable + ", " + value + ", " + bodyIndex +", " +direction + ", " + previousValues.size());
		if (value == null) return null;
		// check if the value has been seen before as grounding of another variable
		Atom atom = this.body.get(bodyIndex);
		boolean headNotTail = atom.getLeft().equals(currentVariable);
		// OI-OFF
		if (previousValues.contains(value)) return null;		
		// the current atom is the last
		if ((direction == true && this.body.size() -1 == bodyIndex) || (direction == false && bodyIndex == 0)) {
			String finalValue = triples.getRandomEntity(atom.getRelation(), value, headNotTail);
			// System.out.println("Y = " + finalValue + " out of " + triples.getEntities(atom.getRelation(), value, headNotTail).size());
			
			// OI-OFF
			if (previousValues.contains(finalValue)) return null;
			// OI-OFF
			if (value.equals(finalValue)) return null;
			return finalValue;
		}
		// the current atom is not the last
		else {
			String nextValue = triples.getRandomEntity(atom.getRelation(), value, headNotTail);
			String nextVariable = headNotTail ? atom.getRight() : atom.getLeft();
			// OI-OFF
			if (Settings.OI_CONSTRAINTS_ACTIVE) previousValues.add(value);
			int updatedBodyIndex = (direction) ? bodyIndex + 1 : bodyIndex - 1;
			return this.beamCyclic(nextVariable, nextValue, updatedBodyIndex, direction, triples, previousValues);
		}
	}
	
	
	
	
	public boolean isRefinable() {
		return true;
	}
	

	
	
	public double getAppliedConfidence() {
		double cop = (double)this.getCorrectlyPredicted();
		double pred = (double)this.getPredicted();
		double rsize = this.bodysize();
		
		if (Settings.RULE_LENGTH_DEGRADE == 1.0) return cop / (pred + Settings.UNSEEN_NEGATIVE_EXAMPLES);
		else return (cop * Math.pow(Settings.RULE_LENGTH_DEGRADE, rsize - 1.0)) / (pred + Settings.UNSEEN_NEGATIVE_EXAMPLES);

	}
	
	
	
	public boolean isSingleton(TripleSet triples) {
		return false;
	}
	
	public HashSet<Triple> getTripleExplanation(String xValue, String yValue, HashSet<Triple> excludedTriples, TripleSet triples) {

		HashSet<Triple> groundings = new HashSet<Triple>();
		ArrayList<Atom> bodyAtoms = new ArrayList<Atom>();
		ArrayList<String> variables = new ArrayList<String>();
		for (int i = 0; i < this.bodysize(); i++) bodyAtoms.add(this.getBodyAtom(i));
		variables.add("X");
		for (int i = 0; i < this.bodysize() - 1; i++) variables.add(Rule.variables[i]);
		variables.add("Y");
		HashSet<String> visitedValues = new HashSet<String>();
		visitedValues.add(xValue);
		visitedValues.add(yValue);
		searchTripleExplanation(xValue, yValue, 0, this.bodysize() - 1, variables, excludedTriples, triples, groundings, visitedValues);
		return groundings;
		
	}

	
	
	private void searchTripleExplanation(String firstValue, String lastValue, int firstIndex, int lastIndex, ArrayList<String> variables, HashSet<Triple> excludedTriples, TripleSet triples, HashSet<Triple> groundings, HashSet<String> visitedValues) {
		
		String firstVar = variables.get(firstIndex);
		String lastVar = variables.get(lastIndex+1);
	
		if (firstIndex == lastIndex) {
			Atom atom = this.getBodyAtom(firstIndex);	
			if (atom.getLeft().equals(firstVar)) {
				if (triples.isTrue(firstValue, atom.getRelation(), lastValue)) {
					Triple g = new Triple(firstValue, atom.getRelation(), lastValue);
					if (!excludedTriples.contains(g)) {
						groundings.add(g);
						// System.out.println("Hit! ADDED " +  g + " and extended the groundings to " + groundings.size() + " triples");
					}
				}
			}
			else {
				if (triples.isTrue(lastValue, atom.getRelation(), firstValue)) {
					Triple g = new Triple(lastValue, atom.getRelation(), firstValue);
					if (!excludedTriples.contains(g)) {
						groundings.add(g);
						// System.out.println("Hit! ADDED " +  g + " and extended the groundings to " + groundings.size() + " triples");
					}
				}
			}
			return;
		}		
		Atom firstAtom = this.getBodyAtom(firstIndex);
		Atom lastAtom = this.getBodyAtom(lastIndex);
		
		Set<String> valuesFromFirst = null;
		boolean firstValuesAreTails;
		if (firstAtom.getLeft().equals(firstVar)) {
			valuesFromFirst = triples.getTailEntities(firstAtom.getRelation(), firstValue);
			firstValuesAreTails = true;
		}
		else {
			valuesFromFirst = triples.getHeadEntities(firstAtom.getRelation(), firstValue);
			firstValuesAreTails = false;
		}
		Set<String> valuesFromLast = null;
		boolean lastValuesAreTails;
		if (lastAtom.getLeft().equals(lastVar)) {
			valuesFromLast = triples.getTailEntities(lastAtom.getRelation(), lastValue);
			lastValuesAreTails = true;
		}
		else {
			valuesFromLast = triples.getHeadEntities(lastAtom.getRelation(), lastValue);
			lastValuesAreTails = false;
		}
		if (valuesFromFirst.size() < valuesFromLast.size()) {
			for (String value : valuesFromFirst) {
				Triple g;
				if (firstValuesAreTails) g = new Triple(firstValue, firstAtom.getRelation(), value);
				else g = new Triple(value, firstAtom.getRelation(), firstValue);
				if (excludedTriples.contains(g)) continue;
				if (visitedValues.contains(value)) continue;
				groundings.add(g);
				visitedValues.add(value);
				// System.out.println("add [" + firstIndex + ","  + lastIndex + "]" + g);
				searchTripleExplanation(value, lastValue, firstIndex +1, lastIndex, variables, excludedTriples, triples, groundings, visitedValues);
				if (groundings.size() < this.bodysize()) {
					// System.out.println("removing " + g + " (num of triples in groundings = " + groundings.size() + ")");
					groundings.remove(g);
				}
				else break;
			}
		}
		else {
			for (String value : valuesFromLast) {
				Triple g;
				if (lastValuesAreTails) g = new Triple(lastValue, lastAtom.getRelation(), value);
				else g = new Triple(value, lastAtom.getRelation(),lastValue);
				if (excludedTriples.contains(g)) continue;
				if (visitedValues.contains(value)) continue;
				groundings.add(g);
				visitedValues.add(value);
				// System.out.println("add [" + firstIndex + ","  + lastIndex + "]" + g);
				searchTripleExplanation(firstValue, value, firstIndex, lastIndex - 1, variables, excludedTriples, triples, groundings, visitedValues);	
				if (groundings.size() < this.bodysize()) {
					groundings.remove(g);
					// System.out.println("removing " + g + " (num of triples in groundings = " + groundings.size() + ")");
				}
				else break;
			}
		}
	}







}
