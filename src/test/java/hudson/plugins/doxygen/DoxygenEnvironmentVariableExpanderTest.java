package hudson.plugins.doxygen;

import static org.junit.Assert.assertEquals;


import hudson.EnvVars;

import org.junit.Test;


public class DoxygenEnvironmentVariableExpanderTest {

	@Test
	public void test() {
			
		// NOTE: This test will only run properly when the following environment values
		// are set (this can be done from mvn/surefire
		//
		/*
		 * TEST_DOXY_SUBS	/my/doxygen/path
		 * TEST_DOXY_A		/another/pathA
		 * TEST_DOXY_B		/another/pathB
		 * TEST_DOXY_C		/another/pathC
		 * TEST_DOXY_D		/another/pathD
		 */
		EnvVars environment = new EnvVars();
		
		environment.addLine("TEST_DOXY_SUBS=/my/doxygen/path");
		environment.addLine("TEST_DOXY_A=/another/pathA");
		environment.addLine("TEST_DOXY_B=/another/pathB");
		environment.addLine("TEST_DOXY_C=/another/pathC");
		environment.addLine("TEST_DOXY_D=/another/pathD");
		
		DoxygenEnvironmentVariableExpander s = new DoxygenEnvironmentVariableExpander(environment);
			
		assertEquals("nothing to see here", s.expand("nothing to see here"));
		
		assertEquals("1/my/doxygen/path", s.expand("1$(TEST_DOXY_SUBS)"));
		assertEquals("2/my/doxygen/path", s.expand("2$ (TEST_DOXY_SUBS)"));
		assertEquals("3/my/doxygen/path", s.expand("3$ ( TEST_DOXY_SUBS)"));
		assertEquals("4/my/doxygen/path", s.expand("4$ ( TEST_DOXY_SUBS )"));
		assertEquals("5/my/doxygen/path", s.expand("5$ (TEST_DOXY_SUBS )"));
		assertEquals("6/my/doxygen/path", s.expand("6$( TEST_DOXY_SUBS )"));
		assertEquals("7/my/doxygen/path", s.expand("7$(TEST_DOXY_SUBS )"));
		assertEquals(" /my/doxygen/path", s.expand(" $(TEST_DOXY_SUBS )"));
		assertEquals(" /my/doxygen/pathz ", s.expand(" $(TEST_DOXY_SUBS )z "));

		assertEquals("1/another/pathA/another/pathB/another/pathC/another/pathD",
				s.expand("1$(TEST_DOXY_A)$(TEST_DOXY_B)$(  TEST_DOXY_C)$  (TEST_DOXY_D)"));
		assertEquals(" /another/pathA/another/pathB/another/pathC/another/pathD", 
				s.expand(" $(TEST_DOXY_A)$(TEST_DOXY_B)$(  TEST_DOXY_C)$  (TEST_DOXY_D)"));
		assertEquals("3/another/pathA /another/pathB/another/pathC/another/pathD", 
				s.expand("3$(TEST_DOXY_A) $(TEST_DOXY_B)$(  TEST_DOXY_C)$  (TEST_DOXY_D)"));
		assertEquals("4/another/pathA /another/pathB /another/pathC/another/pathD   ", 
				s.expand("4$(TEST_DOXY_A) $(TEST_DOXY_B) $(  TEST_DOXY_C)$  (TEST_DOXY_D)   "));
		assertEquals("5zzzz/another/pathAx/another/pathBy/another/pathCzzzz/another/pathD", 
				s.expand("5zzzz$(TEST_DOXY_A)x$(TEST_DOXY_B)y$(  TEST_DOXY_C)zzzz$  (TEST_DOXY_D)"));
		
		assertEquals("path/my/doxygen/pathrestofpath", s.expand("path$(TEST_DOXY_SUBS)restofpath"));
		assertEquals("/my/doxygen/path", s.expand("$(TEST_DOXY_SUBS)"));
		assertEquals("path /my/doxygen/pathrestofpath", s.expand("path $(TEST_DOXY_SUBS)restofpath"));
		assertEquals("path/my/doxygen/path restofpath", s.expand("path$(TEST_DOXY_SUBS) restofpath"));
		assertEquals("path /my/doxygen/path restofpath", s.expand("path $(TEST_DOXY_SUBS) restofpath"));
		
	}
	

	
	
	
	

}
