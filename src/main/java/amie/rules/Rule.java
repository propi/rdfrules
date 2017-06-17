/**
 * @author lgalarra
 * @date Aug 8, 2012
 */
package amie.rules;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javatools.datatypes.ByteString;
import javatools.datatypes.IntHashMap;
import amie.data.KB;

/**
 * A class that represents Horn rules of the form A =&gt; B where A is a conjunction of binary atoms
 * of the form r(x, y). Each atom is represented as a triple [x, r, y] (subject, relation, object).
 * @author lgalarra
 *
 */
public class Rule {

    /**
     * The triple patterns
     */
    List<ByteString[]> triples;

    /**
     * ****** Standard Metrics ************
     */
    /**
     * Support normalized w.r.t to the size of the head relation
     */
    double headCoverage;

    /**
     * Support w.r.t the set of all subjects in the database
     */
    double supportRatio;

    /**
     * Absolute number of bindings of the projection variables of the query
     * (positive examples)
     */
    double support;

    /**
     * In AMIE the cardinality may change when the rule is enhanced with
     * "special" atoms such as the DIFFERENTFROMbs database command. Since the
     * cardinality is used in the hashCode function (good to garantee balanced
     * hash tables), we store the first computed cardinality of the query.
     * Unlike the real cardinality, this values remains constant since the
     * creation of the object.
     */
    long hashSupport;

    /**
     * String unique key for the head of the query
     */
    private String headKey;

    /**
     * Parent query
     */
    private Rule parent;

    /**
     * List of parents: queries that are equivalent to the current query but
     * contain a body atom less.
     */
    private List<Rule> ancestors;

    /**
     * The position of the counting variable in the head atom
     */
    private int functionalVariablePosition;

    /**
     * The number of instances of the counting variable in the antecedent. (size
     * of B in B => r(x, y))
     */
    private long bodySize;

    /**
     * Integer counter used to guarantee unique variable names
     */
    private static int varsCount = 0;

    /**
     * Body - Head (whatever is false or unknown in the database)
     */
    private long bodyMinusHeadSize;

    /**
     * Body - Head* (existential version of the head)
     */
    private double pcaBodySize;

    /**
     * Highest letter used for variable names
     */
    private char highestVariable;
    
    /**
     * Highest numerical suffix associated to variables
     */
    private int highestVariableSuffix;

    /**
     * ****** End of Standard Metrics ************
     */
    /**
     * ****** AMIE+ and approximations ************
     */
    /**
     * Standard confidence theorethical upper bound for standard confidence
     *
     */
    private double _stdConfidenceUpperBound;

    /**
     * PCA confidence theorethical upper bound for PCA confidence
     */
    private double _pcaConfidenceUpperBound;

    /**
     * PCA confidence rough estimation for the hard cases
     */
    private double _pcaConfidenceEstimation;

    /**
     * Time to run the denominator for the expression of the PCA confidence
     */
    private double _pcaConfidenceRunningTime;

    /**
     * Time to run the denominator for the expression of the standard confidence
     */
    private double _confidenceRunningTime;

    /**
     * The rule is below an established support threshold
     */
    private boolean _belowMinimumSupport;

    /**
     * Information about the precision of the rule in some evaluation context
     */
    int[] _evaluationResult;

    /**
     * ****** End of AMIE+ and approximations ************
     */
    /**
     * True is the last atom of the rule contains a variable that binds to a
     * single instance (a case where the AMIE algorithm would prune the rule).
     */
    private boolean _containsQuasiBindings;

    /**
     * ******** Joint Prediction *********
     */
    /**
     * This corresponds to all the fields associated to the project of
     * prediction using rules as multiples sources of evidence 
     *
     */
    /**
     * A unique integer identifier for rules.
     */
    private int id;

    /**
     * It puts the arguments in an array.
     *
     * @param sub
     * @param pred
     * @param obj
     * @return
     */
    public static ByteString[] triple(ByteString sub, ByteString pred, ByteString obj) {
        ByteString[] newTriple = new ByteString[3];
        newTriple[0] = sub;
        newTriple[1] = pred;
        newTriple[2] = obj;
        return newTriple;
    }

    public static boolean equal(ByteString[] pattern1, ByteString pattern2[]) {
        return pattern1[0] == pattern2[0]
                && pattern1[1] == pattern2[1]
                && pattern1[2] == pattern2[2];
    }

    /**
     * It creates a new unbound atom with fresh variables for the subject and object
     * and an undefined property, i.e., ?s[n] ?p ?o[n]. n is optional and is always greater
     * than 1.
     * @return
     */
    public ByteString[] fullyUnboundTriplePattern() {
        ByteString[] result = new ByteString[3];
        
        if (this.highestVariableSuffix < 1) {
        	result[0] = ByteString.of("?" + highestVariable);
        } else {
        	result[0] = ByteString.of("?" + highestVariable + this.highestVariableSuffix);        	
        }
        
        result[1] = ByteString.of("?p");
        
        this.highestVariable = (char) (highestVariable + 1);
        
        if (this.highestVariable > 'z') {
        	this.highestVariableSuffix++;
        	this.highestVariable = 'a';
        }
        
        if (this.highestVariableSuffix < 1) {
            result[2] = ByteString.of("?" + highestVariable);
        } else {
        	result[2] = ByteString.of("?" + highestVariable + highestVariableSuffix);        	
        }
        
        this.highestVariable = (char) (highestVariable + 1);
        
        if (this.highestVariable > 'z') {
        	this.highestVariableSuffix++;
        	this.highestVariable = 'a';
        }
        
        return result;
    }

    /** It creates a new unbound atom with fresh variables for the subject and object
    * and an undefined property, i.e., ?s[n] ?p ?o[n]. n is optional and is always greater
    * than 1.
    **/
    public static synchronized ByteString[] fullyUnboundTriplePattern1() {
        ByteString[] result = new ByteString[3];
        ++varsCount;
        result[0] = ByteString.of("?s" + varsCount);
        result[1] = ByteString.of("?p" + varsCount);
        result[2] = ByteString.of("?o" + varsCount);
        return result;
    }

    public static boolean equals(ByteString[] atom1, ByteString[] atom2) {
        return atom1[0].equals(atom2[0]) && atom1[1].equals(atom2[1]) && atom1[2].equals(atom2[2]);
    }

    /**
     * Instantiates an empty rule.
     */
    public Rule() {
        this.triples = new ArrayList<>();
        this.headKey = null;
        this.support = -1;
        this.hashSupport = 0;
        this.parent = null;
        this.bodySize = -1;
        this.highestVariable = 'a';
        this.pcaBodySize = 0.0;
        this._stdConfidenceUpperBound = 0.0;
        this._pcaConfidenceUpperBound = 0.0;
        this._pcaConfidenceEstimation = 0.0;
        this._belowMinimumSupport = false;
        this._containsQuasiBindings = false;
        this.ancestors = new ArrayList<>();
        this.highestVariableSuffix = 0;
    }

    /**
     * Instantiates a rule of the form [] =&gt; r(?a, ?b) with empty body
     * and the given pattern as rule. 
     * @param headAtom The head atom as an array of the form [?a, r, ?b].
     * @param cardinality
     */
    public Rule(ByteString[] headAtom, double cardinality) {
        this.triples = new ArrayList<>();
        this.support = cardinality;
        this.hashSupport = (int) cardinality;
        this.parent = null;
        this.triples.add(headAtom);
        this.functionalVariablePosition = 0;
        this.bodySize = 0;
        computeHeadKey();
        this.highestVariable = (char) (headAtom[2].charAt(1) + 1);
        this._stdConfidenceUpperBound = 0.0;
        this._pcaConfidenceUpperBound = 0.0;
        this._pcaConfidenceEstimation = 0.0;
        this._belowMinimumSupport = false;
        this._containsQuasiBindings = false;
        this.ancestors = new ArrayList<>();
        this.highestVariableSuffix = 0;
    }

    /**
     * Creates a new query as a clone of the query sent as argument with the given
     * support.
     * @param otherQuery
     * @param support
     */
    public Rule(Rule otherQuery, double support) {
        triples = new ArrayList<>();
        for (ByteString[] sequence : otherQuery.triples) {
            triples.add(sequence.clone());
        }
        this.support = support;
        this.hashSupport = (int) support;
        this.pcaBodySize = otherQuery.pcaBodySize;
        this.bodySize = otherQuery.bodySize;
        this.bodyMinusHeadSize = otherQuery.bodyMinusHeadSize;
        this.functionalVariablePosition = otherQuery.functionalVariablePosition;
        computeHeadKey();
        this.parent = null;
        this.bodySize = -1;
        this.highestVariable = otherQuery.highestVariable;
        this.highestVariableSuffix = otherQuery.highestVariableSuffix;
        this._stdConfidenceUpperBound = 0.0;
        this._pcaConfidenceUpperBound = 0.0;
        this._pcaConfidenceEstimation = 0.0;
        this._containsQuasiBindings = false;
        this.ancestors = new ArrayList<>();
    }

    public Rule(ByteString[] head, List<ByteString[]> body, double cardinality) {
        triples = new ArrayList<ByteString[]>();
        triples.add(head.clone());
        IntHashMap<ByteString> varsHistogram = new IntHashMap<>();
        this.highestVariable = 96; // The ascii code before lowercase 'a'
        if (KB.isVariable(head[0])) {
            varsHistogram.increase(head[0]);
            if (head[0].charAt(1) > highestVariable) {
                this.highestVariable = head[0].charAt(1);
            }
        }

        if (KB.isVariable(head[2])) {
            varsHistogram.increase(head[2]);
            if (head[2].charAt(1) > highestVariable) {
                this.highestVariable = head[2].charAt(1);
            }
        }

        for (ByteString[] atom : body) {
            triples.add(atom.clone());
            if (KB.isVariable(atom[0])) {
                varsHistogram.increase(atom[0]);
                if (atom[0].charAt(1) > highestVariable) {
                    this.highestVariable = atom[0].charAt(1);
                }
            }
            if (KB.isVariable(atom[2])) {
                varsHistogram.increase(atom[2]);
                if (atom[2].charAt(1) > highestVariable) {
                    this.highestVariable = atom[2].charAt(1);
                }
            }
        }

        computeHeadKey();
        this.support = cardinality;
        this.hashSupport = (int) cardinality;
        this.functionalVariablePosition = 0;
        this.parent = null;
        this.bodySize = -1;
        this._stdConfidenceUpperBound = 0.0;
        this._pcaConfidenceUpperBound = 0.0;
        this._pcaConfidenceEstimation = 0.0;
        ++highestVariable;
        this._containsQuasiBindings = false;
        this.ancestors = new ArrayList<>();
    }

    /**
     * Calculate a simple hash key based on the constant arguments of the head
     * variables.
     */
    private void computeHeadKey() {
        headKey = triples.get(0)[1].toString();
        if (!KB.isVariable(triples.get(0)[2])) {
            headKey += triples.get(0)[2].toString();
        } else if (!KB.isVariable(triples.get(0)[0])) {
            headKey += triples.get(0)[0].toString();
        }
    }

    /**
     * Return the list of triples of the query. Modifications to this
     * list alter the query.
     * @return
     */
    public List<ByteString[]> getTriples() {
        return triples;
    }

    /**
     * Returns the triples of a query except for those containing DIFFERENTFROM
     * constraints. Modifications to this list do not alter the query. However, modifications
     * to the atoms do alter the query.
     * @return
     */
    public List<ByteString[]> getRealTriples() {
        List<ByteString[]> resultList = new ArrayList<>();
        for (ByteString[] triple : triples) {
            if (!triple[1].equals(KB.DIFFERENTFROMbs)) {
                resultList.add(triple);
            }
        }

        return resultList;
    }

    /**
     * Returns the head of a query B =&gt; r(a, b) as a triple [?a, r, ?b]. 
     * @return
     */
    public ByteString[] getHead() {
        return triples.get(0);
    }

    /**
     * Returns the head of a query B =&gt; r(a, b) as a triple [?a, r, ?b]. 
     * Alias for the method getHead().
     * @return
     */
    public ByteString[] getSuccedent() {
        return triples.get(0);
    }

    /**
     * Returns the list of triples in the body of the rule.
     * @return Non-modifiable list of atoms.
     */
    public List<ByteString[]> getBody() {
        return triples.subList(1, triples.size());
    }

    /**
     * Returns the list of triples in the body of the rule. It is an alias
     * for the method getBody()
     * @return Non-modifiable list of atoms.
     */
    public List<ByteString[]> getAntecedent() {
        return triples.subList(1, triples.size());
    }

    /**
     * Returns a list with copies of the triples of the rule.
     * Modifications to either the list or the atoms do not alter the rule.
     *
     * @return 
     */
    public List<ByteString[]> getAntecedentClone() {
        List<ByteString[]> cloneList = new ArrayList<>();
        for (ByteString[] triple : getAntecedent()) {
            cloneList.add(triple.clone());
        }

        return cloneList;
    }

    protected void setTriples(ArrayList<ByteString[]> triples) {
        this.triples = triples;
    }

    /**
     * @return the mustBindVariables
     */
    public List<ByteString> getOpenVariables() {
        IntHashMap<ByteString> histogram = variablesHistogram();
        List<ByteString> variables = new ArrayList<ByteString>();
        for (ByteString var : histogram) {
            if (histogram.get(var) < 2) {
                variables.add(var);
            }
        }

        return variables;
    }

    public double getHeadCoverage() {
        return headCoverage;
    }

    public void setHeadCoverage(double headCoverage) {
        this.headCoverage = headCoverage;
    }

    /**
     * @return the support
     */
    public double getSupportRatio() {
        return supportRatio;
    }

    /**
     * @param support the support to set
     */
    public void setSupportRatio(double support) {
        this.supportRatio = support;
    }

    /**
     * @return the headBodyCount
     */
    public double getSupport() {
        return support;
    }

    /**
     * The cardinality number used to hash the rule.
     *
     * @return
     */
    public long getHashCardinality() {
        return hashSupport;
    }

    /**
     * @param cardinality the headBodyCount to set
     */
    public void setSupport(double cardinality) {
        this.support = cardinality;
    }

    /**
     * The support of the body of the rule. If the rule has the form B =&gt; r(x,y) 
     * then the body size is support(B).
     *
     * @return
     */
    public long getBodySize() {
        return bodySize;
    }

    /**
     *
     * @param bodySize
     */
    public void setBodySize(long bodySize) {
        this.bodySize = bodySize;
    }

    /**
     * @return the confidence
     */
    public double getStdConfidence() {
        return (double) support / bodySize;
    }

    /**
     * @return the evaluationResult
     */
    public int[] getEvaluationResult() {
        return _evaluationResult;
    }

    /**
     * @param evaluationResult the evaluationResult to set
     */
    public void setEvaluationResult(int[] evaluationResult) {
        this._evaluationResult = evaluationResult;
    }

    /**
     * @return the pcaConfidence
     */
    public double getPcaConfidence() {
        return support / pcaBodySize;
    }

    public double getConfidenceRunningTime() {
        return _confidenceRunningTime;
    }

    public void setConfidenceRunningTime(double confidenceRunningTime) {
        this._confidenceRunningTime = confidenceRunningTime;
    }

    public double getPcaConfidenceRunningTime() {
        return _pcaConfidenceRunningTime;
    }

    public void setPcaConfidenceRunningTime(double pcaConfidenceRunningTime) {
        this._pcaConfidenceRunningTime = pcaConfidenceRunningTime;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * Returns the last triple pattern added to this rule.
     *
     * @return
     */
    public ByteString[] getLastTriplePattern() {
        if (triples.isEmpty()) {
            return null;
        } else {
            return triples.get(triples.size() - 1);
        }
    }

    /**
     * Return the last triple pattern which is not the a pseudo-atom.
     *
     * @return
     */
    public ByteString[] getLastRealTriplePattern() {
        int index = getLastRealTriplePatternIndex();
        if (index == -1) {
            return null;
        } else {
            return triples.get(index);
        }
    }

    /**
     * Return the index of the last triple pattern which is not the a
     * pseudo-atom.
     *
     * @return
     */
    public int getLastRealTriplePatternIndex() {
        if (triples.isEmpty()) {
            return -1;
        } else {
            int index = triples.size() - 1;
            ByteString[] last = null;
            while (index >= 0) {
                last = triples.get(index);
                if (!last[1].equals(KB.DIFFERENTFROMbs)) {
                    break;
                }
                --index;
            }

            return index;
        }
    }

    /**
     * Returns the number of times the relation occurs in the atoms of the query
     *
     * @return
     */
    public int cardinalityForRelation(ByteString relation) {
        int count = 0;
        for (ByteString[] triple : triples) {
            if (triple[1].equals(relation)) {
                ++count;
            }
        }
        return count;
    }

    /**
     * Returns true if the triple pattern contains constants in all its
     * positions
     *
     * @param pattern
     * @return
     */
    public static boolean isGroundAtom(ByteString[] pattern) {
        // TODO Auto-generated method stub
        return !KB.isVariable(pattern[0])
                && !KB.isVariable(pattern[1])
                && !KB.isVariable(pattern[2]);
    }

    /**
     * Look for the redundant atoms with respect to a reference atom
     * @param withRespectToIdx The index of the reference atom
     * @return
     */
    public List<ByteString[]> getRedundantAtoms(int withRespectToIdx) {
        ByteString[] newAtom = triples.get(withRespectToIdx);
        List<ByteString[]> redundantAtoms = new ArrayList<ByteString[]>();
        for (ByteString[] pattern : triples) {
            if (pattern != newAtom) {
                if (isUnifiable(pattern, newAtom)
                        || isUnifiable(newAtom, pattern)) {
                    redundantAtoms.add(pattern);
                }
            }
        }

        return redundantAtoms;
    }

    /**
     * Checks whether the last atom in the query is redundant.
     * @return
     */
    public List<ByteString[]> getRedundantAtoms() {
        ByteString[] newAtom = getLastTriplePattern();
        List<ByteString[]> redundantAtoms = new ArrayList<ByteString[]>();
        for (ByteString[] pattern : triples) {
            if (pattern != newAtom) {
                if (isUnifiable(pattern, newAtom) || isUnifiable(newAtom, pattern)) {
                    redundantAtoms.add(pattern);
                }
            }
        }

        return redundantAtoms;
    }

    public ByteString getFunctionalVariable() {
        return getHead()[functionalVariablePosition];
    }

    public ByteString getNonFunctionalVariable() {
        return triples.get(0)[getNonFunctionalVariablePosition()];
    }

    public int getFunctionalVariablePosition() {
        return functionalVariablePosition;
    }

    public void setFunctionalVariablePosition(int functionalVariablePosition) {
        this.functionalVariablePosition = functionalVariablePosition;
    }

    public int getNonFunctionalVariablePosition() {
        return functionalVariablePosition == 0 ? 2 : 0;
    }

    /**
     * Determines if the second argument is unifiable to the first one.
     * Unifiable means there is a valid unification mapping (variable -&gt;
     * variable, variable -&gt; constant) between the components of the triple
     * pattern
     *
     * @param pattern
     * @param newAtom
     * @return boolean
     */
    public static boolean isUnifiable(ByteString[] pattern, ByteString[] newAtom) {
        // TODO Auto-generated method stub
        boolean unifiesSubject = pattern[0].equals(newAtom[0]) || KB.isVariable(pattern[0]);
        if (!unifiesSubject) {
            return false;
        }

        boolean unifiesPredicate = pattern[1].equals(newAtom[1]) || KB.isVariable(pattern[1]);
        if (!unifiesPredicate) {
            return false;
        }

        boolean unifiesObject = pattern[2].equals(newAtom[2]) || KB.isVariable(pattern[2]);
        if (!unifiesObject) {
            return false;
        }

        return true;
    }

    public static boolean areEquivalent(ByteString[] pattern, ByteString[] newAtom) {
        boolean unifiesSubject = pattern[0].equals(newAtom[0])
                || (KB.isVariable(pattern[0]) && KB.isVariable(newAtom[0]));
        if (!unifiesSubject) {
            return false;
        }

        boolean unifiesPredicate = pattern[1].equals(newAtom[1])
                || (KB.isVariable(pattern[1]) && KB.isVariable(newAtom[1]));
        if (!unifiesPredicate) {
            return false;
        }

        boolean unifiesObject = pattern[2].equals(newAtom[2])
                || (KB.isVariable(pattern[2]) && KB.isVariable(newAtom[2]));
        if (!unifiesObject) {
            return false;
        }

        return true;
    }

    public static boolean unifies(ByteString[] test, List<ByteString[]> query) {
        for (ByteString[] pattern : query) {
            if (isUnifiable(pattern, test)) {
                return true;
            }

        }

        return false;
    }

    /**
     * It returns a list with all the redundant atoms contained in the first
     * list, i.e., atoms whose removal does not affect the results of the query
     * defined in the second list.
     *
     * @param test
     * @param query
     * @return
     */
    public static List<ByteString[]> redundantAtoms(ByteString[] test, List<ByteString[]> query) {
        List<ByteString[]> redundantAtoms = new ArrayList<ByteString[]>();
        for (ByteString[] pattern : query) {
            if (isUnifiable(pattern, test)) {
                redundantAtoms.add(pattern);
            }

        }

        return redundantAtoms;
    }

    /**
     * Determines whether the last atom of the query.
     *
     * @return boolean
     */
    public boolean containsUnifiablePatterns() {
        int nPatterns = triples.size();
        for (int i = 0; i < nPatterns; ++i) {
            for (int j = i + 1; j < nPatterns; ++j) {
                if (isUnifiable(triples.get(j), triples.get(i))
                        || isUnifiable(triples.get(i), triples.get(j))) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Simple string representation of the rule. Check the methods
     * getRuleString, getFullRuleString and getBasicRuleString.
     */
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (ByteString[] pattern : triples) {
            stringBuilder.append(pattern[0]);
            stringBuilder.append(" ");
            stringBuilder.append(pattern[1]);
            stringBuilder.append(" ");
            stringBuilder.append(pattern[2]);
            stringBuilder.append("  ");
        }

        return stringBuilder.toString();
    }

    /**
     * Returns a list with all the different variables in the query.
     *
     * @return 
     */
    public List<ByteString> getVariables() {
        List<ByteString> variables = new ArrayList<ByteString>();
        for (ByteString[] triple : triples) {
            if (KB.isVariable(triple[0])) {
                if (!variables.contains(triple[0])) {
                    variables.add(triple[0]);
                }
            }

            if (KB.isVariable(triple[2])) {
                if (!variables.contains(triple[2])) {
                    variables.add(triple[2]);
                }
            }
        }

        return variables;
    }

    /**
     * Determines if a pattern contains repeated components, which are
     * considered hard to satisfy (i.e., ?x somePredicate ?x)
     *
     * @return boolean
     */
    public boolean containsRepeatedVariablesInLastPattern() {
        // TODO Auto-generated method stub
        ByteString[] triple = getLastTriplePattern();
        return triple[0].equals(triple[1]) || 
        		triple[0].equals(triple[2]) || 
        		triple[1].equals(triple[2]);
    }

    /**
     * Returns true if the rule contains redundant recursive atoms, i.e., atoms
     * with a relation that occurs more than once AND that do not have any
     * effect on the query result.
     *
     * @return
     */
    public boolean isRedundantRecursive() {
        List<ByteString[]> redundantAtoms = getRedundantAtoms();
        ByteString[] lastPattern = getLastTriplePattern();
        for (ByteString[] redundantAtom : redundantAtoms) {
            if (equals(lastPattern, redundantAtom)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return boolean True if the rule has atoms.
     */
    public boolean isEmpty() {
        // TODO Auto-generated method stub
        return triples.isEmpty();
    }

    /**
     * Returns a histogram with the number of different atoms variables occur
     * in. It discards pseudo-atoms containing the keywords DIFFERENTFROM and
     * EQUALS.
     *
     * @return
     */
    private IntHashMap<ByteString> variablesHistogram() {
        IntHashMap<ByteString> varsHistogram = new IntHashMap<>();
        for (ByteString triple[] : triples) {
            if (triple[1].equals(KB.DIFFERENTFROMbs)) {
                continue;
            }

            if (KB.isVariable(triple[0])) {
                varsHistogram.increase(triple[0]);
            }
            // Do not count twice if a variable occurs twice in the atom, e.g., r(x, x)
            if (!triple[0].equals(triple[2])) {
                if (KB.isVariable(triple[2])) {
                    varsHistogram.increase(triple[2]);
                }
            }
        }

        return varsHistogram;
    }

    /**
     *
     * @return
     */
    private Map<ByteString, Integer> alternativeHistogram() {
        Map<ByteString, Integer> hist = new HashMap<>(triples.size(), 1.0f);
        for (int i = 1; i < triples.size(); ++i) {
            ByteString[] triple = triples.get(i);
            if (triple[1].equals(KB.DIFFERENTFROMbs)) {
                continue;
            }

            if (KB.isVariable(triple[0])) {
                Integer val = hist.get(triple[0]);
                if (val == null) {
                    hist.put(triple[0], 1);
                } else {
                    hist.put(triple[0], val + 1);
                }
            }
            // Do not count twice if a variable occurs twice in the atom, e.g., r(x, x)
            if (!triple[0].equals(triple[2])) {
                if (KB.isVariable(triple[2])) {
                    Integer val = hist.get(triple[2]);
                    if (val == null) {
                        hist.put(triple[2], 1);
                    } else {
                        hist.put(triple[2], val + 1);
                    }
                }
            }
        }

        return hist;
    }

    /**
     * @return boolean True if the rule is closed, i.e., each variable in the
     * rule occurs at least in two atoms.
     */
    public boolean isClosed() {
        if (triples.isEmpty()) {
            return false;
        }

        IntHashMap<ByteString> varsHistogram = variablesHistogram();

        for (ByteString variable : varsHistogram) {
            if (varsHistogram.get(variable) < 2) {
                return false;
            }
        }

        return true;
    }

    /**
     * @return boolean. True if the rule has PCA confidence 1.0
     */
    public boolean isPerfect() {
        return getPcaConfidence() == 1.0;
    }

    /**
     * Return a key for the rule based on the constant arguments of the head
     * atom. It can be used as a hash key.
     *
     * @return
     */
    public String getHeadKey() {
        if (headKey == null) {
            computeHeadKey();
        }

        return headKey;
    }

    /**
     * Returns the rule's head relation as a String
     *
     * @return
     */
    public String getHeadRelation() {
        return triples.get(0)[1].toString();
    }

    /**
     * Returns the rule's head relation as ByteString.
     *
     * @return
     */
    public ByteString getHeadRelationBS() {
        return triples.get(0)[1];
    }

    /**
     * Returns the number of atoms of the rule.
     *
     * @return
     */
    public int getLength() {
        return triples.size();
    }

    /**
     * Returns the number of atoms of the rule that are not pseudo-atoms
     * Pseudo-atoms contain the Database keywords "DIFFERENTFROM" and "EQUALS"
     *
     * @return
     */
    public int getRealLength() {
        int length = 0;
        for (ByteString[] triple : triples) {
            if (!triple[1].equals(KB.DIFFERENTFROMbs)) {
                ++length;
            }
        }

        return length;
    }

    /**
     * Returns the number of atoms of the rule that are neither pseudo-atoms nor
     * type constraints. Pseudo-atoms contain the Database keywords
     * "DIFFERENTFROM"
     *
     * @return
     */
    public int getRealLengthWithoutTypes(ByteString typeString) {
        int length = 0;
        for (ByteString[] triple : triples) {
            if (!triple[1].equals(KB.DIFFERENTFROMbs)
                    && (!triple[1].equals(typeString) || KB.isVariable(triple[2]))) {
                ++length;
            }
        }
        return length;
    }

    /**
     * Returns the number of atoms of the rule that are not type constraints of
     * the form rdf:type(?x, C) where C is a class, i.e., Person.
     *
     * @param typeString
     * @return
     */
    public int getLengthWithoutTypes(ByteString typeString) {
        int size = 0;
        for (ByteString[] triple : triples) {
            if (!triple[1].equals(typeString)
                    || KB.isVariable(triple[2])) {
                ++size;
            }
        }

        return size;
    }

    /**
     * Returns the number of atoms of the rule that are neither type constraints
     * of the form rdf:type(?x, C) or linksTo atoms.
     *
     * @param typeString
     * @return
     */
    public int getLengthWithoutTypesAndLinksTo(ByteString typeString, ByteString linksString) {
        int size = 0;
        for (ByteString[] triple : triples) {
            if ((!triple[1].equals(typeString) || KB.isVariable(triple[2]))
                    && !triple[1].equals(linksString)) {
                ++size;
            }
        }

        return size;
    }

    /**
     * Sets the rule's parent rule.
     *
     * @param parent
     */
    public void setParent(Rule parent) {
        if (parent != null) {
            ancestors.add(parent);
        }
    }

    /**
     * Returns a new rule that contains all the atoms of the current rule plus
     * the atom provided as argument.
     *
     * @param newAtom The new atom.
     * @param cardinality The support of the new rule.
     * @param joinedVariable The position of the common variable w.r.t to the
     * rule in the new atom, i.e., 0 if the new atoms joins on the subject or 2
     * if it joins on the object.
     * @param danglingVariable The position of the fresh variable in the new
     * atom.
     * @return
     */
    public Rule addAtom(ByteString[] newAtom,
            int cardinality, ByteString joinedVariable, ByteString danglingVariable) {
        Rule newQuery = new Rule(this, cardinality);
        ByteString[] copyNewEdge = newAtom.clone();
        newQuery.triples.add(copyNewEdge);
        return newQuery;
    }

    public Rule addAtom(ByteString[] newAtom, double cardinality) {
        Rule newQuery = new Rule(this, cardinality);
        ByteString[] copyNewEdge = newAtom.clone();
        newQuery.triples.add(copyNewEdge);
        return newQuery;
    }

    /**
     * The alternative hash code for the parents of the rule. The alternative
     * hash code if a small variant of the hashCode method that does not use the
     * support of the rule.
     *
     * @return
     */
    public int alternativeParentHashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getHeadKey() == null) ? 0 : getHeadKey().hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) hashSupport;
        result = prime * result + (int) getRealLength();
        result = prime * result + ((headKey == null) ? 0 : headKey.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Rule other = (Rule) obj;
        if (getHeadKey() == null) {
            if (other.getHeadKey() != null) {
                return false;
            }
        } else if (!getHeadKey().equals(other.getHeadKey())) {
            return false;
        }
        if (support != other.support) {
            return false;
        }

        return QueryEquivalenceChecker.areEquivalent(triples, other.triples);
    }

    public static void printRuleBasicHeaders() {
        System.out.println("Rule\tHead Coverage\tStd Confidence\t"
                + "PCA Confidence\tPositive Examples\tBody size\tPCA Body size\t"
                + "Functional variable");
    }

    public static void printRuleHeaders() {
        System.out.println("Rule\tHead Coverage\tStd Confidence\t"
                + "PCA Confidence\tPositive Examples\tBody size\tPCA Body size\t"
                + "Functional variable\tStd. Lower Bound\tPCA Lower Bound\t"
                + "PCA Conf estimation");
    }

    public String getRuleString() {
        //Guarantee that atoms in rules are output in the same order across runs of the program
        class TripleComparator implements Comparator<ByteString[]> {

            public int compare(ByteString[] t1, ByteString[] t2) {
                int predicateCompare = t1[1].toString().compareTo(t2[1].toString());
                if (predicateCompare == 0) {
                    int objectCompare = t1[2].toString().compareTo(t2[2].toString());
                    if (objectCompare == 0) {
                        return t1[0].toString().compareTo(t2[0].toString());
                    }
                    return objectCompare;
                }

                return predicateCompare;
            }
        }

        TreeSet<ByteString[]> sortedBody = new TreeSet<ByteString[]>(new TripleComparator());
        sortedBody.addAll(getAntecedent());
        StringBuilder strBuilder = new StringBuilder();
        for (ByteString[] pattern : sortedBody) {
            if (pattern[1].equals(KB.DIFFERENTFROMbs)) {
                continue;
            }
            strBuilder.append(pattern[0]);
            strBuilder.append("  ");
            strBuilder.append(pattern[1]);
            strBuilder.append("  ");
            strBuilder.append(pattern[2]);
            strBuilder.append("  ");
        }

        strBuilder.append(" => ");
        ByteString[] head = triples.get(0);
        strBuilder.append(head[0]);
        strBuilder.append("  ");
        strBuilder.append(head[1]);
        strBuilder.append("  ");
        strBuilder.append(head[2]);

        return strBuilder.toString();
    }

    public String getFullRuleString() {
        DecimalFormat df = new DecimalFormat("#.#########");
        DecimalFormat df1 = new DecimalFormat("#.##");
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append(getRuleString());

        strBuilder.append("\t" + df.format(getHeadCoverage()));
        strBuilder.append("\t" + df.format(getStdConfidence()));
        strBuilder.append("\t" + df.format(getPcaConfidence()));
        strBuilder.append("\t" + df1.format(getSupport()));
        strBuilder.append("\t" + df1.format(getBodySize()));
        strBuilder.append("\t" + df1.format(getPcaBodySize()));
        strBuilder.append("\t" + getFunctionalVariable());
        strBuilder.append("\t" + _stdConfidenceUpperBound);
        strBuilder.append("\t" + _pcaConfidenceUpperBound);
        strBuilder.append("\t" + _pcaConfidenceEstimation);

        return strBuilder.toString();
    }

    public String getBasicRuleString() {
        DecimalFormat df = new DecimalFormat("#.#########");
        DecimalFormat df1 = new DecimalFormat("#.##");
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append(getRuleString());

        strBuilder.append("\t" + df.format(getHeadCoverage()));
        strBuilder.append("\t" + df.format(getStdConfidence()));
        strBuilder.append("\t" + df.format(getPcaConfidence()));
        strBuilder.append("\t" + df.format(getSupport()));
        strBuilder.append("\t" + getBodySize());
        strBuilder.append("\t" + df.format(getPcaBodySize()));
        strBuilder.append("\t" + getFunctionalVariable());

        return strBuilder.toString();
    }

    public static String toDatalog(ByteString[] atom) {
        return atom[1].toString().replace("<", "").replace(">", "") + "(" + atom[0] + ", " + atom[2] + ")";
    }

    public String getDatalogString() {
        StringBuilder builder = new StringBuilder();

        builder.append(Rule.toDatalog(getHead()));
        builder.append(" <=");
        for (ByteString[] atom : getBody()) {
            builder.append(" ");
            builder.append(Rule.toDatalog(atom));
            builder.append(",");
        }

        if (builder.charAt(builder.length() - 1) == ',') {
            builder.deleteCharAt(builder.length() - 1);
        }

        return builder.toString();
    }

    /**
     * Returns a new query where the variable at the dangling position of the
     * last atom has been unified to the provided constant.
     *
     * @param danglingPosition
     * @param constant
     * @param cardinality
     * @return
     */
    public Rule instantiateConstant(int danglingPosition, ByteString constant, double cardinality) {
        Rule newQuery = new Rule(this, cardinality);
        ByteString[] lastNewPattern = newQuery.getLastTriplePattern();
        lastNewPattern[danglingPosition] = constant;
        newQuery.computeHeadKey();
        return newQuery;
    }

    /**
     * Return a new query where the variable at position danglingPosition in
     * triple at position triplePos is bound to constant.
     *
     * @param triplePos
     * @param danglingPosition
     * @param constant
     * @param cardinality
     * @return
     */
    public Rule instantiateConstant(int triplePos, int danglingPosition, ByteString constant, double cardinality) {
        Rule newQuery = new Rule(this, cardinality);
        ByteString[] targetEdge = newQuery.getTriples().get(triplePos);
        targetEdge[danglingPosition] = constant;
        newQuery.cleanInequalityConstraints();
        return newQuery;
    }

    private void cleanInequalityConstraints() {
        List<ByteString[]> toRemove = new ArrayList<>();
        IntHashMap<ByteString> varHistogram = variablesHistogram();
        for (ByteString[] triple : triples) {
            if (triple[1].equals(KB.DIFFERENTFROMbs)) {
                int varPos = KB.firstVariablePos(triple);
                // Check if the variable became orphan
                if (!varHistogram.containsKey(triple[varPos])) {
                    toRemove.add(triple);
                }
            }
        }

        triples.removeAll(toRemove);
    }

    public List<Rule> getAncestors() {
        return ancestors;
    }

    private void gatherAncestors(Rule q, Set<Rule> output) {
        if (q.ancestors == null
                || q.ancestors.isEmpty()) {
            return;
        } else {
            // Let's do depth search
            for (Rule ancestor : q.ancestors) {
                output.add(ancestor);
                gatherAncestors(ancestor, output);
            }
        }
    }

    public List<Rule> getAllAncestors() {
        Set<Rule> output = new LinkedHashSet<>();
        for (Rule ancestor : ancestors) {
            output.add(ancestor);
            gatherAncestors(ancestor, output);
        }
        return new ArrayList<>(output);
    }

    public void setBodyMinusHeadSize(int size) {
        bodyMinusHeadSize = size;
    }

    public long getBodyMinusHeadSize() {
        return bodyMinusHeadSize;
    }

    public void setPcaBodySize(double size) {
        pcaBodySize = size;
    }

    public double getPcaBodySize() {
        return pcaBodySize;
    }

    public boolean _isBelowMinimumSupport() {
        return _belowMinimumSupport;
    }

    public void _setBelowMinimumSupport(boolean belowMinimumSupport) {
        this._belowMinimumSupport = belowMinimumSupport;
    }

    public boolean _containsQuasiBindings() {
        return this._containsQuasiBindings;
    }

    public void _setContainsQuasibinding(boolean containsQuasiBindings) {
        this._containsQuasiBindings = containsQuasiBindings;

    }

    public Rule rewriteQuery(ByteString[] remove, ByteString[] target, ByteString victimVar, ByteString targetVar) {
        List<ByteString[]> newTriples = new ArrayList<ByteString[]>();
        for (ByteString[] t : triples) {
            if (t != remove) {
                ByteString[] clone = t.clone();
                for (int i = 0; i < clone.length; ++i) {
                    if (clone[i].equals(victimVar)) {
                        clone[i] = targetVar;
                    }
                }

                newTriples.add(clone);
            }
        }

        Rule result = new Rule();
        //If the removal triple is the head, make sure the target is the new head
        if (remove == triples.get(0)) {
            for (int i = 0; i < newTriples.size(); ++i) {
                if (Arrays.equals(target, newTriples.get(i))) {
                    ByteString tmp[] = newTriples.get(0);
                    newTriples.set(0, newTriples.get(i));
                    newTriples.set(i, tmp);
                }
            }
        }

        result.triples.addAll(newTriples);

        return result;
    }

    public boolean variableCanBeDeleted(int triplePos, int varPos) {
        ByteString variable = triples.get(triplePos)[varPos];
        for (int i = 0; i < triples.size(); ++i) {
            if (i != triplePos) {
                if (KB.varpos(variable, triples.get(i)) != -1) {
                    return false;
                }
            }
        }
        //The variable does not appear anywhere else (no constraints)
        return true;
    }

    public static int findFunctionalVariable(Rule q, KB d) {
        ByteString[] head = q.getHead();
        if (KB.numVariables(head) == 1) {
            return KB.firstVariablePos(head);
        }
        return d.functionality(head[1]) > d.inverseFunctionality(head[1]) ? 0 : 2;
    }

    public void setConfidenceUpperBound(double stdConfUpperBound) {
        this._stdConfidenceUpperBound = stdConfUpperBound;
    }

    public void setPcaConfidenceUpperBound(double pcaConfUpperBound) {
        // TODO Auto-generated method stub
        this._pcaConfidenceUpperBound = pcaConfUpperBound;
    }

    /**
     * @return the pcaEstimation
     */
    public double getPcaEstimation() {
        return _pcaConfidenceEstimation;
    }

    /**
     * @param pcaEstimation the pcaEstimation to set
     */
    public void setPcaEstimation(double pcaEstimation) {
        this._pcaConfidenceEstimation = pcaEstimation;
    }

    /**
     * For rules with an even number of atoms (n &gt; 2), it checks if it contains
     * level 2 redundant subgraphs, that is, each relation occurs exactly twice
     * in the rule.
     *
     * @return
     */
    public boolean containsLevel2RedundantSubgraphs() {
        if (!isClosed() || triples.size() < 4 || triples.size() % 2 == 1) {
            return false;
        }

        IntHashMap<ByteString> relationCardinalities = new IntHashMap<>();
        for (ByteString[] pattern : triples) {
            relationCardinalities.increase(pattern[1]);
        }

        for (ByteString relation : relationCardinalities) {
            if (relationCardinalities.get(relation) != 2) {
                return false;
            }
        }

        return true;
    }

    public boolean containsDisallowedDiamond() {
        if (!isClosed() || triples.size() < 4 || triples.size() % 2 == 1) {
            return false;
        }

        // Calculate the relation count
        HashMap<ByteString, List<ByteString[]>> subgraphs = new HashMap<ByteString, List<ByteString[]>>();
        for (ByteString[] pattern : triples) {
            List<ByteString[]> subgraph = subgraphs.get(pattern[1]);
            if (subgraph == null) {
                subgraph = new ArrayList<ByteString[]>();
                subgraphs.put(pattern[1], subgraph);
                if (subgraphs.size() > 2) {
                    return false;
                }
            }
            subgraph.add(pattern);

        }

        if (subgraphs.size() != 2) {
            return false;
        }

        Object[] relations = subgraphs.keySet().toArray();
        List<int[]> joinInfoList = new ArrayList<int[]>();
        for (ByteString[] p1 : subgraphs.get(relations[0])) {
            int[] bestJoinInfo = null;
            int bestCount = -1;
            ByteString[] bestMatch = null;
            for (ByteString[] p2 : subgraphs.get(relations[1])) {
                int[] joinInfo = Rule.doTheyJoin(p1, p2);
                if (joinInfo != null) {
                    int joinCount = joinCount(joinInfo);
                    if (joinCount > bestCount) {
                        bestCount = joinCount;
                        bestJoinInfo = joinInfo;
                        bestMatch = p2;
                    }
                }
            }
            subgraphs.get(relations[1]).remove(bestMatch);
            joinInfoList.add(bestJoinInfo);
        }

        int[] last = joinInfoList.get(0);
        for (int[] joinInfo : joinInfoList.subList(1, joinInfoList.size())) {
            if (!Arrays.equals(last, joinInfo) || (last[1] == 1 && joinInfo[1] == last[1])) {
                return false;
            }
        }

        return true;
    }

    private int joinCount(int[] vector) {
        int count = 0;
        for (int v : vector) {
            count += v;
        }

        return count;
    }

    private static int[] doTheyJoin(ByteString[] p1, ByteString[] p2) {
        int subPos = KB.varpos(p1[0], p2);
        int objPos = KB.varpos(p1[2], p2);

        if (subPos != -1 || objPos != -1) {
            int[] result = new int[3];
            result[0] = (subPos == 0 ? 1 : 0); //subject-subject
            result[1] = (subPos == 2 ? 1 : 0);
            result[1] += (objPos == 0 ? 1 : 0); //subject-object
            result[2] = (objPos == 2 ? 1 : 0);
            return result;
        } else {
            return null;
        }
    }

    /**
     * Applies the mappings provided as first argument to the subject and object
     * positions of the query included in the second argument.
     *
     * @param mappings
     * @param inputTriples 
     */
    public static void bind(Map<ByteString, ByteString> mappings,
            List<ByteString[]> inputTriples) {
        for (ByteString[] triple : inputTriples) {
            ByteString binding = mappings.get(triple[0]);
            if (binding != null) {
                triple[0] = binding;
            }
            binding = mappings.get(triple[2]);
            if (binding != null) {
                triple[2] = binding;
            }
        }
    }

    /**
     * Replaces all occurrences of oldVal with newVal in the subject and object
     * positions of the input query.
     *
     * @param oldVal
     * @param newVal
     * @param query
     */
    public static void bind(ByteString oldVal,
            ByteString newVal, List<ByteString[]> query) {
        for (ByteString[] triple : query) {
            if (triple[0].equals(oldVal)) {
                triple[0] = newVal;
            }

            if (triple[2].equals(oldVal)) {
                triple[2] = newVal;
            }
        }
    }

    /**
     * Verifies if the given rule has higher confidence that its parent rules.
     * The parent rules are those rules that were refined in previous stages of
     * the AMIE algorithm and led to the construction of the current rule.
     *
     * @return true if the rule has better confidence that its parent rules.
     */
    public boolean hasConfidenceGain() {
        if (isClosed()) {
            if (parent != null && parent.isClosed()) {
                return getPcaConfidence() > parent.getPcaConfidence();
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * It returns the query expression corresponding to the normalization value
     * used to calculate the PCA confidence.
     *
     * @return
     */
    public List<ByteString[]> getPCAQuery() {
        if (isEmpty()) {
            return Collections.emptyList();
        }

        List<ByteString[]> newTriples = new ArrayList<>();
        for (ByteString[] triple : triples) {
            newTriples.add(triple.clone());
        }
        ByteString[] existentialTriple = newTriples.get(0);
        existentialTriple[getNonFunctionalVariablePosition()] = ByteString.of("?x");
        return newTriples;
    }

    /**
     * Given a list of rules A1 =&gt; X1, ... An =&gt; Xn, having the same head
     * relation, it returns the combined rule A1,..., An =&gt; X', where X' is the
     * most specific atom. For example given the rules A1 =&gt; livesIn(x, y) and
     * A2 =&gt; livesIn(x, USA), the method returns A1, A2 =&gt; livesIn(x, USA).
     *
     * @param rules
     * @return
     */
    public static Rule combineRules(List<Rule> rules) {
        if (rules.size() == 1) {
            return new Rule(rules.get(0), rules.get(0).getSupport());
        }

        // Look for the most specific head
        Rule canonicalHeadRule = rules.get(0);
        for (int i = 0; i < rules.size(); ++i) {
            int nVariables = KB.numVariables(rules.get(i).getHead());
            if (nVariables == 1) {
                canonicalHeadRule = rules.get(i);
            }
        }

        // We need to rewrite the rules	
        ByteString[] canonicalHead = canonicalHeadRule.getHead().clone();
        ByteString canonicalSubjectExp = canonicalHead[0];
        ByteString canonicalObjectExp = canonicalHead[2];
        Set<ByteString> nonHeadVariables = new LinkedHashSet<>();
        int varCount = 0;
        List<ByteString[]> commonAntecendent = new ArrayList<>();

        for (Rule rule : rules) {
            List<ByteString[]> antecedentClone = rule.getAntecedentClone();

            Set<ByteString> otherVariables = rule.getNonHeadVariables();
            for (ByteString var : otherVariables) {
                Rule.bind(var, ByteString.of("?v" + varCount), antecedentClone);
                ++varCount;
                nonHeadVariables.add(var);
            }

            ByteString[] head = rule.getHead();
            Map<ByteString, ByteString> mappings = new HashMap<>();
            mappings.put(head[0], canonicalSubjectExp);
            mappings.put(head[2], canonicalObjectExp);
            Rule.bind(mappings, antecedentClone);

//            commonAntecendent.addAll(antecedentClone);
//            commonAntecendent.add(atom);
            for (ByteString[] atom : antecedentClone) {
            	boolean repeated = false;
            	for (ByteString[] otherAtom : commonAntecendent) {
            		if (equals(atom, otherAtom)) {
            			repeated = true;
            			break;
            		}
            	}
            	if (!repeated) {
            		commonAntecendent.add(atom);
            	}
            }
        }

        Rule resultRule = new Rule(canonicalHead, commonAntecendent, 0.0);
        return resultRule;
    }

    /**
     * The set of variables that are not in the conclusion of the rule.
     */
    private Set<ByteString> getNonHeadVariables() {
        ByteString[] head = getHead();
        Set<ByteString> nonHeadVars = new LinkedHashSet<>();
        for (ByteString[] triple : getAntecedent()) {
            if (KB.isVariable(triple[0])
                    && KB.varpos(triple[0], head) == -1) {
                nonHeadVars.add(triple[0]);
            }

            if (KB.isVariable(triple[2])
                    && KB.varpos(triple[2], head) == -1) {
                nonHeadVars.add(triple[2]);
            }
        }

        return nonHeadVars;
    }

    public boolean containsRelation(ByteString relation) {
        for (ByteString[] triple : triples) {
            if (triple[1].equals(relation)) {
                return true;
            }
        }

        return false;
    }

    public int containsRelationTimes(ByteString relation) {
        int count = 0;
        for (ByteString[] triple : triples) {
            if (triple[1].equals(relation)) {
                ++count;
            }
        }
        return count;
    }

    /**
     * Given the antecedent and the succedent of a rule as sets of atoms, it
     * returns the combinations of atoms of size 'i' that are "parents" of the
     * current rule, i.e., subsets of atoms of the original rule.
     *
     * @param antecedent
     * @param head
     */
    public static void getParentsOfSize(List<ByteString[]> antecedent,
            ByteString[] head,
            int windowSize, List<List<ByteString[]>> output) {
        int fixedSubsetSize = windowSize - 1;
        if (antecedent.size() < windowSize) {
            return;
        }
        List<ByteString[]> fixedPrefix = antecedent.subList(0, fixedSubsetSize);
        for (int i = fixedSubsetSize; i < antecedent.size(); ++i) {
            List<ByteString[]> combination = new ArrayList<>();
            // Add the head atom.
            combination.add(head);
            combination.addAll(fixedPrefix);
            combination.add(antecedent.get(i));
            output.add(combination);
        }
        if (windowSize > 1) {
            getParentsOfSize(antecedent.subList(1, antecedent.size()), head, windowSize, output);
        }
    }

    /**
     * It determines whether the rule contains a single path that connects the
     * head variables in the body. For example, the rule: - worksAt(x, w),
     * locatedInCity(w, z), locatedInCountry(z, y) =&gt; livesIn(x, y) meets such
     * criteria because there is a single path of variables that connects the
     * head variables in the body: x -&gt; w -&gt; z -&gt; y.
     *
     * @return
     */
    public boolean containsSinglePath() {
        ByteString[] head = getHead();
        if (!KB.isVariable(head[0])
                || !KB.isVariable(head[2])) {
            // We are not interested in rules with a constant in the head.
            return false;
        }

        Map<ByteString, Integer> histogram = alternativeHistogram();
        for (int i = 1; i < triples.size(); ++i) {
            ByteString[] atom = triples.get(i);
            for (int k : new int[]{0, 2}) {
                if (KB.isVariable(atom[k])) {
                    Integer freq = histogram.get(atom[k]);
                    if (freq != null) {
                        if (occursInHead(atom[k])) {
                            if (freq != 1) {
                                return false;
                            }
                        } else {
                            if (freq != 2) {
                                return false;
                            }
                        }
                    }
                } else {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Returns true if the given expression (variable or constant) occurs in
     * the rule's head.
     * @param expression
     * @return
     */
    private boolean occursInHead(ByteString expression) {
        ByteString[] head = getHead();
        return expression.equals(head[0]) || expression.equals(head[2]);
    }

    /**
     * Returns an array with the variables that occur in the body but not in the
     * head.
     * @return
     */
    public Set<ByteString> getBodyVariables() {
        List<ByteString> headVariables = getHeadVariables();
        Set<ByteString> result = new LinkedHashSet<>();
        for (ByteString[] triple : getBody()) {
            if (KB.isVariable(triple[2])
                    && !headVariables.contains(triple[2])) {
                result.add(triple[2]);
            }

            if (KB.isVariable(triple[2])
                    && !headVariables.contains(triple[2])) {
                result.add(triple[2]);
            }
        }
        return result;
    }

    /**
     * Returns the head variables of the rule.
     *
     * @return
     */
    public List<ByteString> getHeadVariables() {
        List<ByteString> headVariables = new ArrayList<>();
        ByteString[] head = getHead();
        if (KB.isVariable(head[0])) {
            headVariables.add(head[0]);
        }
        if (KB.isVariable(head[2])) {
            headVariables.add(head[2]);
        }
        return headVariables;
    }

    /**
     * Given a rule that contains a single variables path for the head variables
     * in the body (the method containsSinglePath returns true), it returns the
     * atoms sorted so that the path can be reproduced.
     *
     * @return
     */
    public List<ByteString[]> getCanonicalPath() {
        // First check the most functional variable
        ByteString funcVar = getFunctionalVariable();
        ByteString nonFuncVar = getNonFunctionalVariable();
        List<ByteString[]> body = getBody();
        Map<ByteString, List<ByteString[]>> variablesToAtom = new HashMap<>(triples.size(), 1.0f);
        List<ByteString[]> path = new ArrayList<>();
        // Build a multimap, variable -> {atoms where the variable occurs}
        for (ByteString[] bodyAtom : body) {
            if (KB.isVariable(bodyAtom[0])) {
                telecom.util.collections.Collections.addToMap(variablesToAtom, bodyAtom[0], bodyAtom);
            }

            if (KB.isVariable(bodyAtom[2])) {
                telecom.util.collections.Collections.addToMap(variablesToAtom, bodyAtom[2], bodyAtom);
            }
        }

        // Start with the functional variable.
        ByteString joinVariable = funcVar;
        ByteString[] lastAtom = null;
        while (true) {
            List<ByteString[]> atomsList = variablesToAtom.get(joinVariable);
            // This can be only the head variable
            if (atomsList.size() == 1) {
                lastAtom = atomsList.get(0);
            } else {
                for (ByteString[] atom : atomsList) {
                    if (atom != lastAtom) {
                        // Bingo
                        lastAtom = atom;
                        break;
                    }
                }
            }
            path.add(lastAtom);
            joinVariable
                    = lastAtom[0].equals(joinVariable) ? lastAtom[2] : lastAtom[0];
            if (joinVariable.equals(nonFuncVar)) {
                break;
            }
        }

        return path;
    }

    /**
     * Given 2 atoms joining in at least one variable, it returns the first
     * pairs of variable positions of each atom.
     *
     * @param a1
     * @param a2
     * @return
     */
    public static int[] joinPositions(ByteString[] a1, ByteString[] a2) {
        if (a1[0].equals(a2[0])) {
            return new int[]{0, 0};
        } else if (a1[2].equals(a2[2])) {
            return new int[]{2, 2};
        } else if (a1[0].equals(a2[2])) {
            return new int[]{0, 2};
        } else if (a1[2].equals(a2[0])) {
            return new int[]{2, 0};
        } else {
            return null;
        }
    }

    /**
     * Returns a new rule that is a copy of the current rules plus the two edges
     * sent as arguments.
     *
     * @param newEdge1
     * @param newEdge2
     * @return
     */
    public Rule addEdges(ByteString[] newEdge1, ByteString[] newEdge2) {
        Rule newQuery = new Rule(this, (int) this.support);
        newQuery.triples.add(newEdge1.clone());
        newQuery.triples.add(newEdge2.clone());
        return newQuery;
    }

    public List<ByteString> getBodyRelations() {
        List<ByteString> bodyRelations = new ArrayList<>();
        for (ByteString[] atom : getBody()) {
            if (!bodyRelations.contains(atom[1])) {
                bodyRelations.add(atom[1]);
            }
        }
        return bodyRelations;
    }
    
    public static void main(String[] args) {
    	Rule q = new Rule();
    	for (int i = 0;  i < 50; ++i) {
    		System.out.println(Arrays.toString(q.fullyUnboundTriplePattern()));
    	}
    }
}
