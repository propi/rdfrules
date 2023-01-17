package anyburl;

import java.util.HashSet;
import java.util.Set;


public class Atom {
	

	private String relation;
	private String left;
	private String right;
	
	private boolean leftC;
	private boolean rightC;
	
	private int hashcode = 0; 
	private boolean hashcodeInitialized = false;
	
	public Atom(String left, String relation, String right, boolean leftC, boolean rightC) {
		this.left = left;
		this.right = right;
		this.relation = relation;
		this.leftC = leftC;
		this.rightC = rightC;
		
	}
	
	public Atom getXYGeneralization() {
		Atom copy = this.createCopy();
		copy.setLeft("X");
		copy.setLeftC(false);
		copy.setRight("Y");
		copy.setRightC(false);
		return copy;
	}
	
	public Atom(String a) {
		
		if (a.endsWith(" ")) a = a.substring(0, a.length()-1);
		if (a.endsWith(",")) a = a.substring(0, a.length()-1);
		if (a.endsWith(";")) a = a.substring(0, a.length()-1);
	
		String left = null;
		String right = null;
		
		
		String[] t1 = a.split("\\(", 2);
		String relation = t1[0];
		String aa = t1[1];
		
		if (aa.matches("[A-Z],.*\\)")) {
			left = aa.substring(0,1);
			right = aa.substring(2,aa.length()-1);
		}
		else {
			left = aa.substring(0,aa.length()-3);
			right = aa.substring(aa.length()-2,aa.length()-1);
		}
		this.relation = relation.intern();
		this.left = left.intern();
		this.right = right.intern();
		this.leftC = (this.left.length() == 1) ? false : true;
		this.rightC = (this.right.length() == 1) ? false : true;
	}

	public String getRelation() {
		return relation;
	}

	public void setRelation(String relation) {
		this.relation = relation;
	}

	public String getLeft() {
		return left;
	}

	public void setLeft(String left) {
		this.left = left;
	}

	public String getRight() {
		return right;
	}

	public void setRight(String right) {
		this.right = right;
	}

	public boolean isLeftC() {
		return leftC;
	}

	public void setLeftC(boolean leftC) {
		this.leftC = leftC;
	}

	public boolean isRightC() {
		return rightC;
	}

	public void setRightC(boolean rightC) {
		this.rightC = rightC;
	}

	

	
	public String toString(int indent) {
		String l = this.left;
		String r = this.right;
		if (indent > 0) {
			if (!this.isLeftC() && !this.left.equals("X") && !this.left.equals("Y")) {
				int li = Rule.variables2Indices.get(this.left);
				l = Rule.variables[li+indent];
			}
			if (!this.isRightC() && !this.right.equals("X") && !this.right.equals("Y")) {
				int ri = Rule.variables2Indices.get(this.right);
				r = Rule.variables[ri+indent];
			}
		}
		return this.relation + "(" + l + "," + r + ")"; 
	}
	
	public boolean equals(Object thatObject) {
		if (thatObject instanceof Atom) {
			Atom that = (Atom)thatObject;
			if (this.getRelation().equals(that.getRelation()) && this.getLeft().equals(that.getLeft()) && this.getRight().equals(that.getRight())) {
				return true;
			}
		}
		return false;
	}
	
	
	public boolean equals(Atom that, String vThis, String vThat) {
		if (!this.getRelation().equals(that.getRelation())) return false;
		if ((this.left.equals(vThis)  && that.left.equals(vThat)) || (this.left.equals(vThis)  && that.left.equals(vThat)) ) return true;
		return false;
	}
	
	
	/**
	* Returns true if this is more special than the given atom g.
	* 
	* @param g
	* @return
	*/
	public boolean moreSpecial(Atom g) {
		if (this.getRelation().equals(g.getRelation())) {			
			if (this.equals(g)) {
				return true;
			}
			
			if (this.left.equals(g.left)) {
				if (!g.rightC && this.rightC) return true;
				return false;
			}
			if (this.right.equals(g.right)) {
				if (!g.leftC && this.leftC) return true;
				return false;
			}
			if (!g.leftC && !g.rightC && this.leftC && this.rightC) return true;
			return false;
		}
		return false;
	}
	
	/**
	* Returns true if this is more special than the given atom g, given that vThis is substituted by vThat.
	* 
	* @param g The more general atom.
	* @return
	*/
	public boolean moreSpecial(Atom that, String vThis, String vThat) {
		if (this.getRelation().equals(that.getRelation())) {			
			if ((this.left.equals(vThis) && that.left.equals(vThat))) {
				if (!that.rightC && this.rightC) return true;
				if (that.right.equals(this.right)) return true;
				return false;
			}
			if ((this.right.equals(vThis) && that.right.equals(vThat))) {
				if (!that.leftC && this.leftC) return true;
				if (that.left.equals(this.left)) return true;
				return false;
			}
			return false;
		}
		return false;
	}
	
	
	public int hashCode() {
		if (!this.hashcodeInitialized ) {
			this.hashcode = this.toString().hashCode();
			this.hashcodeInitialized = true;
		}
		return this.hashcode;
	}

	
	/**
	 * Creates and returns a deep copy of this atom.
	 * 
	 * @return A deep copy of this atom.
	 */
	public Atom createCopy() {
		Atom copy = new Atom(this.left, this.relation, this.right, this.leftC, this.rightC);
		return copy;
	}

	public int replaceByVariable(String constant, String variable) {
		int i = 0;
		if (this.leftC && this.left.equals(constant)) {
			this.leftC = false;
			this.left = variable;
			i++;
		}
		if (this.rightC && this.right.equals(constant)) {
			this.rightC = false;
			this.right = variable;
			i++;
		}
		return i;
	}
	
	public int replace(String vOld, String vNew, int block) {
		if (this.left.equals(vOld) && block != -1) {
			this.left = vNew;
			return -1;
			
		}
		if (this.right.equals(vOld) && block != 1) {
			this.right = vNew;
			return 1;
		}
		return 0;
	}
	
	public void replace(String vOld, String vNew) {
		this.replace(vOld, vNew, 0);
	}

	public boolean uses(String constantOrVariable) {
		if (this.getLeft().equals(constantOrVariable)) {
			return true;
		}
		if (this.getRight().equals(constantOrVariable)) {
			return true;
		}
		return false;
	}

	public boolean isLRC(boolean leftNotRight) {
		if (leftNotRight) return this.isLeftC();
		else return this.isRightC();
	}

	public String getLR(boolean leftNotRight) {
		if (leftNotRight) return this.getLeft();
		else return this.getRight();
	}

	public boolean contains(String term) {
		if (this.left.equals(term) || this.right.equals(term)) return true;
		return false;
	}

	public String getConstant() {
		if (isLeftC()) return this.left;
		if (isRightC()) return this.right;
		return null;
	}

	public boolean isInverse(int pos) {
		boolean inverse;
		if (this.isRightC() || this.isLeftC()) {
			if (this.isRightC()) inverse = false;
			else inverse = true;			
		}
		else {
			if (this.right.compareTo(this.left) < 0) inverse = true;
			else inverse = false;			
			if (pos == 0) return !inverse;
		}
		return inverse;
	}
	

	public Set<String> getVariables() {
		HashSet<String> vars = new HashSet<String>();
		if (!this.isLeftC() && !this.left.equals("X") && !this.left.equals("Y")) {
			vars.add(this.left);
		}
		if (!this.isRightC() && !this.right.equals("X") && !this.right.equals("Y")) {
			vars.add(this.right);
		}
		return vars;
	}

	public String getOtherTerm(String v) {
		if (this.left.equals(v)) {
			return this.right;
		}
		else if (this.right.equals(v)) {
			return this.left;
		}
		return null;
	}

	public String toString(String c, String v) {
		return this.relation + "(" + (this.left.equals(c) ? v : this.left) + "," + (this.right.equals(c) ? v : this.right) + ")"; 
	}
	
	public String toString() {
		return this.relation + "(" + this.left + "," + this.right + ")"; 
	}








}
