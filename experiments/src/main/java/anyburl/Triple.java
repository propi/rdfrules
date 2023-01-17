package anyburl;

/**
 * A triple represents a labeled edge a knowledge graph.
 * 
 *
 */
public class Triple {
	
	public boolean invalid = false;

	private String head; // subject
	private String tail; // object
	private String relation;
	
	private int h = 0;
	private boolean h_set = false;
	
	private static int ignoreCounter = 0;
	private static int ignoreMaxDisplay = 0;
	
	
	public Triple(String head, String relation, String tail) {
		if (head.length() < 2 || tail.length() < 2) {
			if (ignoreCounter < ignoreMaxDisplay) {
				System.err.println("the triple set you are trying to load contains constants of length 1 ... a constant (entity) needs to be described by at least two letters");
				System.err.println("ignoring: " + head + " " + relation + " " + tail);
				ignoreCounter++;
			}
	
			// System.exit(1);
			this.invalid = true;
		}
		this.head = head;
		this.relation = relation;
		if (Settings.REWRITE_REFLEXIV && head.equals(tail)) {
			this.tail = Settings.REWRITE_REFLEXIV_TOKEN;
		}
		else {
			this.tail = tail;
		}
	}
	
	public static Triple createTriple(String head, String relation, String tail, boolean reverse) {
		if (reverse) {
			return new Triple(tail, relation, head);
		}
		else {
			return new Triple(head, relation, tail);
		}
	}
	
	public String getHead() {
		return this.head;
	}

	public String getTail() {
		return this.tail;
	}
	
	/**
	 * 
	 * If headNotTail is true, the head of this triples is used as return value. Otherwise the tail.
	 * 
	 * @param headNotTail
	 * @return
	 */
	public String getValue(boolean headNotTail) {
		if (headNotTail) return this.head;
		else return this.tail;
	}

	public String getRelation() {
		return relation;
	}
	
	
	public String toString() {
		return this.head + " " + this.relation + " " + this.tail;
	}
	
	
	/*
	public String toString() {
		return this.head + "\t" + this.relation + "\t" + this.tail;
	}
	*/
	
	public boolean equals(Object that) {
		if (that instanceof Triple) {
			Triple thatTriple = (Triple)that;
			if (this.head.equals(thatTriple.head) && this.tail.equals(thatTriple.tail) && this.relation.equals(thatTriple.relation)) {
				return true;
			}
		}
		return false;
	}
	
	public int hashCode() {
		if (!h_set) {
			h = this.head.hashCode() + this.tail.hashCode() + this.relation.hashCode();
		}	
		return h;
	}

	public boolean equals(boolean headNotTail, String subject, String rel, String object) {
		if (headNotTail) {
			return (this.head.equals(subject) && this.tail.equals(object) && this.relation.equals(rel)); 
		}
		else {
			return (this.head.equals(object) && this.tail.equals(subject) && this.relation.equals(rel)); 
		}
	}
	
	public double getConfidence() {
		return 1.0;
	}

	/**
	 * Returns a string representation of this triples by replacing the constant by a variable wherever it appears
	 * 
	 * @param constant The constant to be replaced.
	 * @param variable The variable that is shown instead of the constant.
	 * 
	 * @return The footprint of a triples that can be compared by equals against the atom in a AC1 rule.
	 */
	public String getSubstitution(String constant, String variable) {
		String t = this.tail.equals(constant) ? variable : this.tail;
		String h = this.head.equals(constant) ? variable : this.head;
		return this.relation + "(" + h + "," + t + ")";
	}
	
	
	/**
	 * Returns a string representation of this triples by replacing the constant by a variable wherever it appears and repalcing the other constant by a sedocn variable.
	 * 
	 * @param constant The constant to be replaced.
	 * @param variable The variable that is shown instead of the constant.
	 * @param otherVariable The variable that is shown instead of the other constant.
	 * 
	 * @return The footprint of a triples that can be compared by equals against the atom in a AC2 rule .
	 */
	public String getSubstitution(String constant, String variable, String otherVariable) {
		String t = this.tail.equals(constant) ? variable : this.tail;
		String h = this.head.equals(constant) ? variable : this.head;
		if (t.equals(variable)) h = otherVariable;
		if (h.equals(variable)) t = otherVariable;
		return this.relation + "(" + h + "," + t + ")";
	}


 	

}
