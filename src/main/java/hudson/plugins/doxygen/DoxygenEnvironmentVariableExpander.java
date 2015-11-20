package hudson.plugins.doxygen;

import hudson.EnvVars;

import java.io.*;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Expands Doxygen environment variables.
 * 
 * It replaces variables specified in the Doxyfile configuration of the form  "$(VAR)" with the contents
 * of the VAR system environment variable.
 * 
 * @author mlos
 *
 */
public class DoxygenEnvironmentVariableExpander implements Serializable {

	// Evaluates "$(varname)" including any white space, putting "$" and "varname"
	// in capturing groups 1 and 2, respectively.
	private static final Pattern DOXY_VAR_PATTERN = Pattern.compile("(\\$)\\s*\\(\\s*(\\w+)\\s*\\)");
	
    private static final Logger LOGGER = Logger.getLogger(DoxygenEnvironmentVariableExpander.class.getName());


	private EnvVars environment;
	
	public DoxygenEnvironmentVariableExpander(EnvVars environment) {
		this.environment = environment;
	}
	/**
	 * Expands a Doxygen environment variable.
	 * @param doxyVar The variable to expand
	 * @return The expanded variable.
	 */
	public String expand(String doxyVar) {
		
		String[] keys = extractKeys(doxyVar);
		String expandedVar = doxyVar;
		
		String val = "";
		
		for(int i = 0; i < keys.length; i++) {
			
			if(environment != null) {
				val = environment.expand(keys[i]);
			
				if(val != null) {
					expandedVar = expandedVar.replaceFirst(DOXY_VAR_PATTERN.toString(), val);
				} else {
					LOGGER.log(Level.WARNING, "The environment variable '" + keys[i] + "' was not set.");
				}
			}
		}
		
		return expandedVar;
	}

	
	private String[] extractKeys(String input) {
		
		Matcher regexMatcher = DOXY_VAR_PATTERN.matcher(input);
		ArrayList<String> keys = new ArrayList<String>();
		
		try {
			
			while(regexMatcher.find()) {
				
				keys.add(regexMatcher.group(1) + regexMatcher.group(2));
			}
			
		} catch(Exception e) {

			System.err.println("Regular expression error while parsing keys.");
		}

		String[] result = new String[keys.size()];
		
		// Keep it simple for the caller, use arrays.
		return keys.toArray(result);
	}
		
}
