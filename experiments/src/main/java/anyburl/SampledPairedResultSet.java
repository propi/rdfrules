package anyburl;

import java.util.HashMap;
import java.util.HashSet;

public class SampledPairedResultSet {
	
	private int valueCounter = 0;
	
	private HashMap<String, HashSet<String>> values;
	private boolean sampling;
	
	private String currentKey = "";
	
	private int chao = 0;
	
	public void setChaoEstimate(int f2) {
		int count = 0;
		for (String s : values.keySet()) {
			count += values.get(s).size();
		}
		this.chao = this.getChaoEstimate(count, f2 + 1, count + f2 + 1);
	}
	
	public int getChaoEstimate() {
		return this.chao;
	}
	
	private int getChaoEstimate(int f1, int f2, int d) {
		int c = (int)(d + ((double)(f1 * f1) / (double)(2.0 * f2)));
		//System.out.println("chao=" + c + " f1=" + f1 + " f2=" + f2 + " d=" + d);
		
		return c;
	}
	
	public SampledPairedResultSet() {
		this.values = new HashMap<String, HashSet<String>>();
		this.sampling = false;
	}
	
	public void addKey(String key) {
		this.currentKey = key;
		if (this.values.containsKey(key)) return;
		else this.values.put(key, new HashSet<String>());
	}
	
	public HashMap<String, HashSet<String>> getValues() {
		return this.values;
	}
	
	public boolean usedSampling() {
		return this.sampling;
	}

	public boolean addValue(String value) {
		if (this.values.get(currentKey).add(value)) {
			this.valueCounter++;
			return true;
		}
		return false;
	}
	
	public int size() {
		return this.valueCounter;
	}

}
