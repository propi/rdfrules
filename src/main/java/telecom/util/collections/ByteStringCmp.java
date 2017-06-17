package telecom.util.collections;

import java.util.Comparator;
import java.util.TreeSet;

import javatools.datatypes.ByteString;

public class ByteStringCmp implements Comparator<ByteString> {

	@Override
	public int compare(ByteString o1, ByteString o2) {
		return o1.toString().compareTo(o2.toString());
	}
	
	public static void main(String[] args) {
		TreeSet<ByteString> tree = new TreeSet<>();
		tree.add(ByteString.of("Luis"));
		tree.add(ByteString.of("Diana"));
		tree.add(ByteString.of("Jorge"));
		tree.add(ByteString.of("Gonzalo"));
		tree.add(ByteString.of("Letty"));		
		tree.add(ByteString.of("Pedro"));		
		tree.add(ByteString.of("Carita"));
		System.out.println(tree);
	}

}
