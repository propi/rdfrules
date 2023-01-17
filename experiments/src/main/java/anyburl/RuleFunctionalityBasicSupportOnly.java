package anyburl;

/**
 * This exception is thrown if a rule does not support a more complex functionality that goes beyond the 
 * standard methods of rules required for running anyburl.
 * 
 * It has nothing to do with logical functionality.
 * 
 * @author Christian
 *
 */
public class RuleFunctionalityBasicSupportOnly extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2037385071211375547L;
	

	public RuleFunctionalityBasicSupportOnly() {
	
	}
		
	public String toString() {
		return "RuleFunctionalityBasicSupportOnly Exception (some specific method is called for a rule type that supports (currently) only basic methods)";

	}
	
	

}
