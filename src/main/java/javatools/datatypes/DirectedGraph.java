package javatools.datatypes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javatools.filehandlers.FileLines;

/** 
This class is part of the Java Tools (see http://mpii.de/yago-naga/javatools).
It is licensed under the Creative Commons Attribution License 
(see http://creativecommons.org/licenses/by/3.0) by 
the YAGO-NAGA team (see http://mpii.de/yago-naga).
  
This class implements a directed Graph.
*/
public class DirectedGraph<E extends Comparable<E>> {

  /** Represents a node of in a graph*/
	public static class Node<E extends Comparable<E>> implements Comparable<Node<E>>{
	  /** Points to the parents of this node*/
		protected SortedSet<Node<E>> parents=new TreeSet<Node<E>>();
		/** Points to the children of thise node*/
		protected SortedSet<Node<E>> children=new TreeSet<Node<E>>();
		/** Holds the label of node*/
		protected E label;
		
		@SuppressWarnings("unchecked")
		public boolean equals(Object o) {
			return(o!=null && o instanceof Node && ((Node<E>)o).label.equals(label));
		}
		/** Created a node with a label -- in a directed or undirected graph*/
		protected Node(E l, boolean directed) {
			label=l;
			if(!directed) children=parents;
		}
		/** Adds a child*/
    public void addChild(Node<E> child) {
      children.add(child);
      child.parents.add(this);
    }
    /** Adds a parents*/
    public void addParent(Node<E> parent) {
      parents.add(parent);
      parent.children.add(this);
    }
    /** Adds a node in an undirected graph*/
    public void addLink(Node<E> node) {
      children.add(node);
      node.parents.add(this);
    }
    /** returns the links in an undirected graph*/
    public Set<Node<E>> links() {
      return(parents);
    }
    /** Returns the children*/
    public Set<Node<E>> children() {
      return(children);
    }
    /** Returns the parents*/
    public Set<Node<E>> parents() {
      return(parents);
    }
		@Override
		public int hashCode() {
			return label.hashCode();
		}
		@Override
		public String toString() {
		  return(label.toString());
		}
		/** Helper for Graph.makeClosure*/
		protected void closure(Set<Node<E>> ancestors) {
			for(Node<E> n : ancestors) n.addChild(this);
				ancestors.add(this);
				List<Node<E>> realchildren=new ArrayList<Node<E>>(children);
				for(Node<E> c : realchildren) c.closure(ancestors);
				ancestors.remove(this);
		}
		/** Computes the ancestors of this node*/
    public SortedSet<Node<E>> ancestors() {
      SortedSet<Node<E>> result=new TreeSet<DirectedGraph.Node<E>>();
      ancestors(result);
      return(result);
    }
    /** Helper method*/
    protected void ancestors(Set<Node<E>> ancestors) {
      for(Node<E> n : parents) {
        if(ancestors.contains(n)) continue;
        ancestors.add(n);
        n.ancestors(ancestors);
      }
    }   
    /** Computes the ancestors of this node*/
    public Set<Node<E>> descendants() {
      Set<Node<E>> result=new TreeSet<DirectedGraph.Node<E>>();
      descendants(result);
      return(result);
    }
    /** Helper method*/
    protected void descendants(Set<Node<E>> descendants) {
      for(Node<E> n : children) {
        if(descendants.contains(n)) continue;
        descendants.add(n);
        n.descendants(descendants);
      }
    }   
		@Override
		public int compareTo(Node<E> arg0) {
			return label.compareTo(arg0.label);
		}
		/** Returns the label*/
    public E label() {
      return label;
    }
	}
	/** Adds a link from a parent to a child*/
	public void addLink(E parent, E child) {
		getOrMake(parent).addChild(getOrMake(child));
	}
	/** Holds the nodes*/
	protected Map<E,Node<E>> nodes=new TreeMap<E,Node<E>>();
	
	/** Returns or creates a node*/
	public Node<E> getOrMake(E label) {
		if(!nodes.containsKey(label)) nodes.put(label, new Node<E>(label, true));
		return(nodes.get(label));
	}
	
	/** Constructs a directed graph from a File. Previously a constructor, but that caused compilation problems*/
	public static DirectedGraph<String> create(File file, Pattern pattern) throws IOException {
	  DirectedGraph<String> result=new DirectedGraph<String>();
	  for(String line : new FileLines(file, "Loading graph")) {
			Matcher m=pattern.matcher(line);
			if(!m.find()) continue;
			result.addLink(m.group(2),m.group(1));
		}
	  return(result);
	}
	/** Constructor for subclasses*/
	public DirectedGraph() {
	}

	public String toString() {
		StringBuilder result=new StringBuilder();
		for(Node<E> n : nodes()) result.append(n.label).append(" -> ").append(n.children).append('\n');
		return(result.toString());
	}

	/** Computes the nodes that have no parents*/
	public SortedSet<Node<E>> roots() {
		TreeSet<Node<E>> result=new TreeSet<DirectedGraph.Node<E>>();
		for(Node<E> n : nodes.values()) {
			if(n.parents.size()==0) result.add(n);
		}
		return(result);
	}

	/** Computes the nodes that have no leaves*/
	 public SortedSet<Node<E>> leaves() {
	    TreeSet<Node<E>> result=new TreeSet<DirectedGraph.Node<E>>();
	    for(Node<E> n : nodes.values()) {
	      if(n.children.size()==0) result.add(n);
	    }
	    return(result);
	  }

	 /** Returns all nodes*/
  public Collection<Node<E>> nodes(){
    return(nodes.values());
  }
  /** Returns the number of nodes*/
  public int numNodes(){
    return(nodes.size());
  }
  
  /** Computes the set of labels*/
  public Collection<E> labels(){
    TreeSet<E> result=new TreeSet<E>();
    for(Node<E> n : nodes()) result.add(n.label);
    return(result);
  }
	
  /** Returns the node with the given label (or null)*/
	public Node<E> get(E label) {
		return(nodes.get(label));
	}
	/** True if that node exists*/
	public boolean contains(E label) {
		return(nodes.get(label)!=null);
	}
	/** Adds all transitive links*/
	public void makeClosure() {
		for(Node<E> n : roots()) {
			n.closure(new TreeSet<Node<E>>());
		}
	}
}
