package hudson.plugins.doxygen;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Substitutes Doxygen variables with environment variables.
 * 
 * It replaces variables specified in the doxyfile configuration of the form  "$(VAR)" with the contents
 * of the VAR system environment variable.
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
		
		String sysEnv = "";
		
		for(int i = 0; i < keys.length; i++) {
			
			sysEnv = System.getenv(keys[i]);
			if(sysEnv != null)
				subst = subst.replaceFirst(DOXY_VAR_PATTERN.toString(), sysEnv);
		}
		
		return subst;
	}

	
	public String[] extractKeys(String input) {
		
		Matcher regexMatcher = DOXY_VAR_PATTERN.matcher(input);
		ArrayList<String> keys = new ArrayList<String>();
		
		try {
			
			while(regexMatcher.find()) {
				
				keys.add(regexMatcher.group(2));
			}
			
		} catch(Exception e) {

			System.err.println("Regular expression error while parsing keys.");
		}

		String[] result = new String[keys.size()];
		
		return keys.toArray(result);
	}
		
}
