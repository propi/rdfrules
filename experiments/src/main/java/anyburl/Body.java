package anyburl;

import java.util.*;


public class Body implements Iterable<Atom> {
	
	private int hashcode = 0;
	private boolean hashcodeInitialized = false;
	
	protected ArrayList<Atom> literals;
	
	public Body() {
		this.literals = new ArrayList<Atom>();	
	}

	public void add(Atom atom) {
		this.literals.add(atom);
	}

	public Atom get(int index) {
		return this.literals.get(index);
	}
	
	public void set(int index, Atom atom) {
		this.literals.set(index, atom);
	}

	public int size() {
		return this.literals.size();
	}

	@Override
	public Iterator<Atom> iterator() {
		return this.literals.iterator();
	}
	
	
	public boolean contains(Atom a) {
		for (Atom lit : this.literals) {
			if (a.equals(lit)) return true;
		}
		return false;
	}
	
	
	public int hashCode() {
		if (this.hashcodeInitialized) return this.hashcode;
		StringBuilder sb = new StringBuilder();
		for (Atom a : this.literals) {
			sb.append(a.toString());
		}
		this.hashcode = sb.toString().hashCode();
		this.hashcodeInitialized = true;
		return this.hashcode;
	}
	
	public String toString() {
		if (this.literals.size() == 0) return "";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < this.literals.size()-1; i++) {
			sb.append(this.literals.get(i));
			sb.append(", ");
		}
		sb.append(this.literals.get(this.literals.size() -1));
		return sb.toString();
	}
	
	public String toString(int indent) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < this.literals.size()-1; i++) {
			sb.append(this.literals.get(i).toString(indent));
			sb.append(", ");
		}
		sb.append(this.literals.get(this.literals.size() -1).toString(indent));
		return sb.toString();
	}
	
	public boolean equals(Object thatObject) {
		if (thatObject instanceof Body) {
			Body that = (Body)thatObject;
			if (this.literals.size() == that.literals.size()) {
				HashMap<String, String> variablesThis2That = new HashMap<String, String>();
				HashMap<String, String> variablesThat2This = new HashMap<String, String>();
				for (int i = 0; i < this.literals.size(); i++) { 
					Atom atom1 = this.literals.get(i);
					Atom atom2 = that.literals.get(i);
					if (!atom1.getRelation().equals(atom2.getRelation())) {
						return false;
					}
					else {
						if (!checkValuesAndVariables(variablesThis2That, variablesThat2This, atom1, atom2, true)) return false;
						if (!checkValuesAndVariables(variablesThis2That, variablesThat2This, atom1, atom2, false)) return false;
					}
				}
				return true;
			}
		}
		return false;
	}
	
	private boolean checkValuesAndVariables(HashMap<String, String> variablesThis2That, HashMap<String, String> variablesThat2This, Atom atom1, Atom atom2, boolean leftNotRight) {
		if (atom1.isLRC(leftNotRight) && atom2.isLRC(leftNotRight)) {
			if (!atom1.getLR(leftNotRight).equals(atom2.getLR(leftNotRight))) { return false; }
		}
		if (atom1.isLRC(leftNotRight) != atom2.isLRC(leftNotRight)) {
			// one variable and one constants do not fit
			return false;
		}
		if (!atom1.isLRC(leftNotRight) && !atom2.isLRC(leftNotRight)) {
			// special cases X must be at same position as X, Y at same as Y
			if (atom1.getLR(leftNotRight).equals("X") && !atom2.getLR(leftNotRight).equals("X")) return false;
			if (atom2.getLR(leftNotRight).equals("X") && !atom1.getLR(leftNotRight).equals("X")) return false;
			
			if (atom1.getLR(leftNotRight).equals("Y") && !atom2.getLR(leftNotRight).equals("Y")) return false;
			if (atom2.getLR(leftNotRight).equals("Y") && !atom1.getLR(leftNotRight).equals("Y")) return false;
			
			if (variablesThis2That.containsKey(atom1.getLR(leftNotRight))) {
				String thatV = variablesThis2That.get(atom1.getLR(leftNotRight));
				if (!atom2.getLR(leftNotRight).equals(thatV)) return false;
			}
			if (variablesThat2This.containsKey(atom2.getLR(leftNotRight))) {
				String thisV = variablesThat2This.get(atom2.getLR(leftNotRight));
				if (!atom1.getLR(leftNotRight).equals(thisV)) return false;
			}
			if (!variablesThis2That.containsKey(atom1.getLR(leftNotRight))) {
				variablesThis2That.put(atom1.getLR(leftNotRight), atom2.getLR(leftNotRight));
				variablesThat2This.put(atom2.getLR(leftNotRight), atom1.getLR(leftNotRight));
			}
		}
		return true;
	}

	public int getNumOfVariables() {
		HashSet<String> variables = new HashSet<String>();
		for (Atom a : this.literals) {
			variables.addAll(a.getVariables());
		}
		return variables.size();
	}
	
	/**
	 * Returns the last atom in this body.
	 * 
	 * @return The last atom in this body.
	 */
	public Atom getLast() {
		return this.get(this.literals.size()-1);
	}

	public void normalizeVariableNames() {
		HashMap<String, String> old2New = new HashMap<String, String>();
		int indexNewVariableNames = 0;
		
		
		for (int i = 0; i < this.size(); i++) {
			Atom atom = this.get(i);
			Set<String> variables = atom.getVariables();
			int block = 0;
			for (String v : variables) {
				if (v.equals("X") || v.equals("Y")) continue;
				if (!old2New.containsKey(v)) {
					String vNew = Rule.variables[indexNewVariableNames];
					old2New.put(v, vNew);
					indexNewVariableNames++;
				}
				block = atom.replace(v, old2New.get(v), block);
			}
		}
		
	}

	/**
	 * Replaces all atoms by deep copies of these atoms to avoid that references from the outside are affected by follow up changes.
	*/
	public void detach() {
		for (int i = 0; i < this.literals.size(); i++) {
			Atom atom = this.literals.get(i).createCopy();
			this.literals.set(i, atom);
		}
	}


	
	
	
}
