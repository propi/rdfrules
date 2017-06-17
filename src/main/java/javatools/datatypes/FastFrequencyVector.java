package javatools.datatypes;

import javatools.administrative.D;

/**
 * 
 * This class is part of the Java Tools (see
 * http://mpii.de/yago-naga/javatools). It is licensed under the Creative
 * Commons Attribution License (see http://creativecommons.org/licenses/by/3.0)
 * by the YAGO-NAGA team (see http://mpii.de/yago-naga).
 * 
 * This class FrequencyVector methods with DoubleHashMap and IntHashMap
 * 
 * @author Fabian M. Suchanek
 * 
 * @param <K>
 */
public class FastFrequencyVector {

	/** Computes the fuzzy precision of this vector wrt the other vector */
	public static<K> double fuzzyPrecision(DoubleHashMap<K> me, DoubleHashMap<K> other) {
		return (fuzzyRecall(other, me));
	}
	/** Computes the fuzzy precision of this vector wrt the other vector */
	public static<K> double fuzzyPrecision(IntHashMap<K> me, IntHashMap<K> other) {
		return (fuzzyRecall(other, me));
	}

	/** Computes the fuzzy recall of this vector wrt the other vector */
	public static<K> double fuzzyRecall(DoubleHashMap<K> me, DoubleHashMap<K> other) {
		if (other.isEmpty())
			return (1.0);
		double myMax=me.findMax();
		double otherMax=other.findMax();
		double fuzzyRecall = 0;
		for (K trueTerm : other.keys()) {
			double trueValue = other.get(trueTerm)/otherMax;
			double guessedValue = me.get(trueTerm)/myMax;
			if (trueValue > guessedValue) {
				fuzzyRecall += trueValue - guessedValue;
			}
		}
		fuzzyRecall = 1 - fuzzyRecall / other.computeSum() * otherMax;
		if (fuzzyRecall < 0)
			fuzzyRecall = 0; // Small rounding errors may occur
		return (fuzzyRecall);
	}

	/** Computes the fuzzy recall of this vector wrt the other vector */
	public static<K> double fuzzyRecall(IntHashMap<K> me, IntHashMap<K> other) {
		if (other.isEmpty())
			return (1.0);
		double myMax=me.findMax();
		double otherMax=other.findMax();
		double fuzzyRecall = 0;
		for (K trueTerm : other.keys()) {
			double trueValue = other.get(trueTerm)/otherMax;
			double guessedValue = me.get(trueTerm)/myMax;
			if (trueValue > guessedValue) {
				fuzzyRecall += trueValue - guessedValue;
			}
		}
		fuzzyRecall = 1 - fuzzyRecall / other.computeSum() * otherMax;
		if (fuzzyRecall < 0)
			fuzzyRecall = 0; // Small rounding errors may occur
		return (fuzzyRecall);
	}

	public static void main(String[] args) throws Exception {
		IntHashMap<String> person=new IntHashMap<String>();
		person.put("birthDate", 100);
		person.put("birthPlace", 80);
		person.put("deathPlace", 50);
		person.put("wonPrize", 10);
		IntHashMap<String> livingPerson=new IntHashMap<String>();
		livingPerson.put("birthDate", 20);
		livingPerson.put("birthPlace", 16);
		// no death place
		IntHashMap<String> scientist=new IntHashMap<String>();
		scientist.put("birthDate", 10);
		scientist.put("birthPlace", 10);
		scientist.put("deathPlace", 8);
		scientist.put("wonPrize", 8);
		D.p("LivingPerson covers what person has:", fuzzyRecall(livingPerson,person));
		D.p("LivingPerson is covered by person:", fuzzyPrecision(livingPerson,person));
		D.p("Scientist covers what person has:", fuzzyRecall(scientist,person));
		D.p("Scientist is covered by person:", fuzzyPrecision(scientist,person));
	}
}
