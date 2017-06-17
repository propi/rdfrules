package javatools.datatypes;

/** 
This class is part of the Java Tools (see http://mpii.de/yago-naga/javatools).
It is licensed under the Creative Commons Attribution License 
(see http://creativecommons.org/licenses/by/3.0) by 
the YAGO-NAGA team (see http://mpii.de/yago-naga).
  
This class implements an undirected graph.
*/

public class UndirectedGraph<E extends Comparable<E>> extends DirectedGraph<E> {

	public UndirectedGraph() {
      super();
	}

	/** Returns a node or creates it*/
	public Node<E> getOrMake(E label) {
		if(!nodes.containsKey(label)) nodes.put(label, new Node<E>(label, false));
		return(nodes.get(label));
	}

}
