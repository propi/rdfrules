package telecom.util.datatypes;

public class BitSet {
	
	private boolean[] data;
	
	public BitSet(int size) {
		data = new boolean[size];
		for (int i = 0; i < size; ++i) {
			data[i] = false;
		}
	}
	
	public void set(int bitIndex, boolean value) {
		data[bitIndex] = value;
	}
	
	public boolean get(int bitIndex) {
		return data[bitIndex];
	}
	
	public int length() {
		return data.length;
	}
	
	public int size() {
		return data.length;
	}
	
	public boolean[] getData() {
		return data;
	}
}
