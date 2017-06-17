package javatools.datatypes;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import javatools.administrative.D;

/**
 * This class is part of the Java Tools (see
 * http://mpii.de/yago-naga/javatools). It is licensed under the Creative
 * Commons Attribution License (see http://creativecommons.org/licenses/by/3.0)
 * by the YAGO-NAGA team (see http://mpii.de/yago-naga).
 * 
 * The class implements the Trie data type.
 */
public class Trie extends AbstractSet<CharSequence> {

	/** Holds the children */
	public TreeMap<Character, Trie> children = new TreeMap<Character, Trie>();

	/** true if this is a word */
	public boolean isWord = false;

	/** number of elements */
	protected int size=0;
	
	/** maps to parent*/
	protected Trie parent;
	
  /** Constructs a Trie*/
  public Trie() {
    
  }
  /** Constructs a Trie*/
  protected Trie(Trie p) {
    parent=p;
  }
	
	@Override
	public boolean add(CharSequence s) {
	  boolean a=add(s, 0);
	  if(a) size++;
		return (a);
	}

	@Override
	public void clear() {
	  children.clear();
	  isWord=false;
	  size=0;
	}
	
	@Override
	public boolean isEmpty() {
	   return(size==0);
	}
	
	/** Adds a sequence starting from start position */
	protected boolean add(CharSequence s, int start) {
		if (s.length() == start) {
			if (isWord)
				return (false);
			isWord = true;
			return (true);
		}
		Character c = s.charAt(start);
		if (children.get(c) == null)
			children.put(c, new Trie(this));
		return (children.get(c).add(s, start + 1));
	}

	@Override
	public boolean contains(Object s) {
		return (s instanceof CharSequence && containsCS((CharSequence) s, 0));
	}

	/** TRUE if the trie contains the sequence from start position on */
	protected boolean containsCS(CharSequence cs, int start) {
		if (cs.length() == start)
			return (isWord);
		Character c = cs.charAt(start);
		if (children.get(c) == null)
			return (false);
		return (children.get(c).containsCS(cs, start + 1));
	}

	@Override
	public PeekIterator<CharSequence> iterator() {
	  if(isEmpty()) return(PeekIterator.emptyIterator());
		return(new PeekIterator<CharSequence>() {

		  StringBuilder currentString=new StringBuilder();
		  Trie currentTrie=Trie.this;
      @Override
      protected CharSequence internalNext() throws Exception {
        do {
          SortedMap<Character,Trie> chars=currentTrie.children;
          // If we cannot go down
          while(chars.isEmpty()) {
            // If we cannot go up, we give up
            if(currentTrie.parent==null) return(null);
            // Go up
            currentTrie=currentTrie.parent;
            // Take next neighbor
            chars=currentTrie.children.headMap(currentString.charAt(currentString.length()-1));
            currentString.setLength(currentString.length()-1);
          }
          // Finally, we can go down
          Character c=chars.lastKey();
          currentString.append(c.charValue());
          currentTrie=currentTrie.children.get(c);
        } while(!currentTrie.isWord);
        return(currentString.toString());
      }
		  
		});
	}

	@Override
	public String toString() {
	  return "Trie with "+size()+" elements and "+children.size()+" children";
	}
	@Override
	public int size() {
		return (size);
	}

	/**
	 * Returns the length of the longest contained subsequence, starting from
	 * start position
	 */
	public int containedLength(CharSequence s, int startPos) {
		if (isWord)
			return (0);
		if(s.length()<=startPos) return(-1);
		Character c = s.charAt(startPos);
		if (children.get(c) == null)
			return (-1);
		int subtreelength = children.get(c).containedLength(s, startPos + 1);
		if (subtreelength == -1)
			return (-1);
		return (subtreelength + 1);
	}

	/** Returns all words found */
	public PeekIterator<CharSequence> wordsIn(final CharSequence text) {
		return (new PeekIterator<CharSequence>() {
			int pos = -1;

			public CharSequence internalNext() {
				while (++pos < text.length()) {
					int subtreeLength = containedLength(text, pos);
					if (subtreeLength != -1)
						return (text.subSequence(pos, subtreeLength + pos));					
				}
				return (null);
			}
		});
	}

	/** Test method */
	public static void main(String[] args) {
		Trie t = new Trie();
		t.add("hallo");
		t.add("du");
		for(String s : new IterableForIterator<String>(t.stringIterator())) D.p(s);
		D.p(t.wordsIn("Blah hallo blub hallo fasel du").asList());
	}

  public Iterator<String> stringIterator() {
    return(new MappedIterator<CharSequence, String>(iterator(), MappedIterator.stringMapper));
  }
  public Iterable<String> strings() {
    return(new MappedIterator<CharSequence, String>(iterator(), MappedIterator.stringMapper));
  }
}
