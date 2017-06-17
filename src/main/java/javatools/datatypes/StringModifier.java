package javatools.datatypes;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javatools.database.Database;

/** 
This class is part of the Java Tools (see http://mpii.de/yago-naga/javatools).
It is licensed under the Creative Commons Attribution License 
(see http://creativecommons.org/licenses/by/3.0) by 
the YAGO-NAGA team (see http://mpii.de/yago-naga).
  

  
  This class is a collection of additional string modification methods.
   
  For instance, it provides methods to generate a string from an array, 
  separating array elements by a given delimiter; 
  
  String array[]={"cat", "mouse", "cheese"}; 
  String imploded=StringModifier.implode(array," eats ");
  System.out.println(imploded);  
  -> cat eats mouse eats cheese;
  
  imploded=StringModifier.implode(array,",");
  System.out.println(imploded);
  -> cat,mouse,cheese;
  
  This is helpfull for instance to generate a list of values 
  for database insertion; note that there is also a version
  of that functionality that makes sure the array strings 
  are all formated for database insertion/queries (e.g. put in
  quotations).
   
   
 */
public abstract class StringModifier {
  


  /* Concatenates the Strings contained in an array to a combined string, 
   * separating each two array Strings with the given delimeter */
  public static String implode(String[] array, String delim){
    if (array.length==0) {
      return "";
    } else {
      StringBuffer sb = new StringBuffer();
      sb.append(array[0]);
      for (int i=1;i<array.length;i++) {
        sb.append(delim);
        sb.append(array[i]);
      }
      return sb.toString();
    }
  }

  /* Concatenates the Strings contained in a collection to a combined string, 
   * separating each two array Strings with the given delimeter */
  public static String implode(Collection<?> col, String delim){
    if(col==null)
      return "";
    Iterator<?> it=col.iterator();
    if(!it.hasNext())
      return "";
    else{
      StringBuffer sb = new StringBuffer();
      sb.append(it.next());
      while (it.hasNext()){
        sb.append(delim);
        sb.append(it.next().toString());              
      }
      return sb.toString();
    }
  }  
  
  /* Concatenates the Strings contained in a collection to a combined string, 
   * separating each two array Strings with the given delimeter 
   * while applying the database.format function on each array string piece */
  public static String implodeForDB(Collection<?> col, String delim, Database database ){    
    return implodeForDB(col.iterator(),delim, database);
  }
  
  /* Concatenates the String pieces produced by an iterator to a combined string, 
   * separating each two array Strings with the given delimeter 
   * while applying the database.format function to each array string piece */
  public static String implodeForDB(Iterator<?> it, String delim, Database database ){
    if(it==null)
      return "";
    if(!it.hasNext())
      return "";
    else{
      StringBuilder sb = new StringBuilder();
      sb.append(database.format(it.next()));
      while (it.hasNext()){
        sb.append(delim);
        sb.append(database.format(it.next()));              
      }
      return sb.toString();
    }
  }
  
  /* Concatenates the String pieces contained in an array to a combined string, 
   * separating each two array values with the given delimeter 
   * while applying the database.format function to each array entry */
  public static <T> String implodeForDB(T[] col, String delim, Database database ){        
    if(col.length<=0)
      return "";
    else{
    	StringBuilder sb = new StringBuilder();
      sb.append(database.format(col[0]));
      for (int i=1;i<col.length;i++){
        sb.append(delim);
        sb.append(database.format(col[i]));              
      }
      return sb.toString();
    }
  }
  
  
  /** Concatenates key value pairs of a Map into a combined String 
   * representing the pairs as independent column conditions for a database query, 
   * applying the database.format function to each value
   * @param map the Map to be imploded 
   * @param database  the Database instance for which the pairs shall be formatted  */
  public static String implodeForDBAsConditions(Map<?,?> map, Database database ){
    if(map==null)
      return "";
    if(map.isEmpty())
      return "";
    else{
      return implodeForDB(map.entrySet().iterator()," = "," AND ",database,false,true);
    }
  }
  
  
  /** Concatenates key value pairs of a Map into a combined String, 
   * separating each key from its value by a key-value delimeter 
   * and each key-value pair by pair delimeter 
   * while optionally applying the database.format function to each key/value
   * @param map the Map to be imploded
   * @param keyValueDelimeter delimeter inserted between each key and its value
   * @param pairDelimeter delimeter inserted between key-value pairs 
   * @param database  the Database instance for which the pairs shall be formatted 
   * @param formatKey flag indicating whether to format the keys with database.format 
   * @param formatValue flag indicating whether to format the values with database.format */
  public static String implodeForDB(Map<?,?> map, String keyValueDelim, String pairDelim, Database database, boolean formatKey, boolean formatValue ){
    if(map==null)
      return "";
    if(map.isEmpty())
      return "";
    else{
      return implodeForDB(map.entrySet().iterator(),keyValueDelim,pairDelim,database,formatKey,formatValue);
    }
  }
  


  /** Concatenates key value pairs of a Map.Entry iterator into a combined String, 
   * separating each key from its value by a key-value delimeter 
   * and each key-value pair by pair delimeter 
   * while optionally applying the database.format function to each key/value
   * @param it  the Map.Entry iterator
   * @param keyValueDelimeter delimeter inserted between each key and its value
   * @param pairDelimeter delimeter inserted between key-value pairs 
   * @param database  the Database instance for which the pairs shall be formatted 
   * @param formatKey flag indicating whether to format the keys with database.format 
   * @param formatValue flag indicating whether to format the values with database.format */
  public static <T,K> String implodeForDB(Iterator<Entry<T,K>> it, String keyValueDelim, String pairDelim, Database database, boolean formatKey, boolean formatValue ){
    if(it==null)
      return "";
    if(!it.hasNext())
      return "";
    else{
      StringBuffer sb = new StringBuffer();
      Map.Entry<T, K> entry=it.next();
      sb.append(formatKey?database.format(entry.getKey()):entry.getKey());
      sb.append(keyValueDelim);
      sb.append(formatValue?database.format(entry.getValue()):entry.getValue());
      
      while (it.hasNext()){
        sb.append(pairDelim);
        
        entry=it.next();
        sb.append(formatKey?database.format(entry.getKey()):entry.getKey());
        sb.append(keyValueDelim);
        sb.append(formatValue?database.format(entry.getValue()):entry.getValue());        
      }
      return sb.toString();
    }
  }

  
  
  /** limits the length of a String to the given size
   * ie applies s.substring(0,length) for given length iff
   * length<s.length() */
  public static String limitLength(String s, int length){
    if(s.length()>length)
      return s.substring(0,length);
    else return s;
  }
  
  
  /** produces an n-gram set from the string 
   * @param original  the String to be split into n-grams of size n 
   * @param n size of the n-grams
   * @return  set of n-grams */
  public static Set<String> toNGram(String original, int n ){
    Set<String> ngrams = new HashSet<String>();
    for (int i=0;i<original.length();i++)
      ngrams.add(original.substring(i, i+n));
    return ngrams;
  }
  
  /** produces a weighed n-gram set from the string 
   * @param original  the String to be split into n-grams of size n 
   * @param n size of the n-grams
   * @return  map of n-grams with their weight (frequency/overall number of ngrams) */
  public static Map<String,Double> toWeighedNGram(String original, int n ){
    Map<String,Double> ngrams = new HashMap<String,Double>();
    for (int i=0;i<original.length();i++){
      String ngram=original.substring(i, Math.min(i+n, original.length()-1));
      Double oldVal=ngrams.get(ngram);
      ngrams.put(ngram, oldVal!=null?oldVal+(1d/original.length()):(1d/original.length()));
    }    
    return ngrams;
  }
  
  /** applies lowercase to all Strings in an array, returns them as a set */
  public static final Set<String> lowercase (String... ar){
	    Set<String> s=new HashSet<String>(ar.length);
	    for(String a:ar)
	      s.add(a.toLowerCase());
	    return s;
	  }
  
  /** checks for equality between two potential strings */
  public static final boolean areEqual(String s1, String s2){
	  if(s1==null)
		  if(s2!=null)
			  return false;
		  else 
			  return true;
	  return s1.equals(s2);
  }
  
  /** Test method */
  public static void main(String[] argv) throws Exception {
    String array[]={"cat", "mouse", "cheese"}; 
    String imploded=StringModifier.implode(array," eats ");
    System.out.println(imploded);    
           
    imploded=StringModifier.implode(array,",");
    System.out.println(imploded);    
  }
     
  

}
