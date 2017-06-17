package telecom.util.datatypes;


public class BigBinaryNumber {
	
	private final BitSet data;

	/**
	 * 
	 * @param number
	 */
	public BigBinaryNumber(String numberStr) {
		data = new BitSet(numberStr.length());
		for (int i = 0; i < data.length(); ++i) {
			char c = numberStr.charAt(i);
			if (c != '0' && c != '1')
				throw new IllegalArgumentException("BigBinaryNumber expects a string containing only 1s or 0s");
			data.set(i, c == '1');
		}
	}
	
	public BigBinaryNumber(int size) {
		data = new BitSet(size);
	}
	
	/**
	 * Returns the highest position containing a 1
	 * @return -1 if there are not 1s in the number
	 */
	private int highest(boolean val) {
		for (int i = data.length() - 1; i >=0; --i) {
			if (data.get(i) == val) {
				return i;
			}
		}
		
		return -1;
	}
	
	/**
	 * Returns the highest position containing a 1
	 * @return -1 if there are not 1s in the number
	 */
	private int lowest(boolean val) {
		for (int i = 0; i < data.length(); ++i) {
			if (data.get(i) == val) {
				return i;
			}
		}
		
		return -1;
	}
	
	/**
	 * It adds 1 to the value of the number
	 * @return the same object modified or null if the operation did 
	 * not succeded (the number stores the highest possible value representable
	 * with the given number of bits)
	 */
	public BigBinaryNumber increment() {
		int highestOne = highest(true);
		int lowestZero = lowest(false);
		
		if (highestOne == data.length() - 1
				&& lowestZero == -1) {
			return null;
		}
		
		if (lowestZero < highestOne) {
			// Nice case
			data.set(lowestZero, true);
			for (int i = 0; i < lowestZero; ++i) {
				data.set(i, false);
			}
		} else {
			// Nice case
			data.set(lowestZero, true);
			for (int i = 0; i <= highestOne; ++i) {
				data.set(i, false);
			}
		}
		
		return this;
	}
	
	@Override
	public String toString() {
		StringBuilder strBuilder = new StringBuilder();
		for (int i = 0; i < data.length(); ++i) {
			strBuilder.append(data.get(i) ? '1' : '0');
		}
		
		return strBuilder.toString();
	}
	
	@Override
	public int hashCode() {
		return data.hashCode();
	}
	
	public boolean[] toArray() {
		return data.getData();
	}
	
	/**
	 * Return the number of 1s in the binary number
	 * @return
	 */
	public int nOnes() {
		int ones = 0;
		for (int i = 0; i < data.length(); ++i) {
			if (data.get(i))
				++ones;
		}
		
		return ones;
	}
	
	public static void main(String args[]) {
		BigBinaryNumber n = new BigBinaryNumber("00000000");
		do {
			System.out.println(n);
		} while(n.increment() != null);
	}
}
