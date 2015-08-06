package hudson.plugins.doxygen;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Substitutes Doxygen variables with environment variables.
 * 
 * It replaces variables specified in the doxyfile configuration of the form  "$(var)" with "$var".
 * The latter can then be treated as proper system environment variables and substituted as necessary. 
 * 
 * @author mlos
 *
 */
public class DoxygenVariableSubstitutor {

	// Evaluates "$(varname)" including any white space, putting "$" and "varname"
	// in capturing groups 1 and 2, respectively.
	private static final Pattern DOXY_VAR_PATTERN = Pattern.compile("(\\$)\\s*\\(\\s*(\\w+)\\s*\\)");

	
	/**
	 * Substitutes a Doxygen variable.
	 * @param doxyVar The variable to which to apply the substitution
	 * @return The substituted variable.
	 */
	public String substitute(String doxyVar) {
		
		String[] keys = extractKeys(doxyVar);
		
		String subst = doxyVar;

		for(int i = 0; i < keys.length; i++) {
			
			// Prepend \, otherwise replaceFirst gets confused.
			subst = subst.replaceFirst(regex.toString(),   "\\" + keys[i]);
		}
		
		return subst;
	}

	
	public String[] extractKeys(String input) {
		
		Matcher regexMatcher = DOXY_VAR_PATTERN.matcher(input);
		ArrayList<String> keys = new ArrayList<String>();
		
		try {
			
			while(regexMatcher.find()) {
				
				// Join the $ and the key name.
				keys.add(regexMatcher.group(1) + regexMatcher.group(2));
			}
			
		} catch(Exception e) {

			System.err.println("Regular expression error while parsing keys.");
		}

		String[] result = new String[keys.size()];
		
		return keys.toArray(result);
	}
		
}
