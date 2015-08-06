package hudson.plugins.doxygen;

import static org.junit.Assert.assertEquals;


import org.junit.Test;


public class DoxygenVariableSubstitutorTest {

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
		DoxygenVariableSubstitutor s = new DoxygenVariableSubstitutor();
			
		assertEquals("nothing to see here", s.substitute("nothing to see here"));
		
		assertEquals("1/my/doxygen/path", s.substitute("1$(TEST_DOXY_SUBS)"));
		assertEquals("2/my/doxygen/path", s.substitute("2$ (TEST_DOXY_SUBS)"));
		assertEquals("3/my/doxygen/path", s.substitute("3$ ( TEST_DOXY_SUBS)"));
		assertEquals("4/my/doxygen/path", s.substitute("4$ ( TEST_DOXY_SUBS )"));
		assertEquals("5/my/doxygen/path", s.substitute("5$ (TEST_DOXY_SUBS )"));
		assertEquals("6/my/doxygen/path", s.substitute("6$( TEST_DOXY_SUBS )"));
		assertEquals("7/my/doxygen/path", s.substitute("7$(TEST_DOXY_SUBS )"));
		assertEquals(" /my/doxygen/path", s.substitute(" $(TEST_DOXY_SUBS )"));
		assertEquals(" /my/doxygen/pathz ", s.substitute(" $(TEST_DOXY_SUBS )z "));

		assertEquals("1/another/pathA/another/pathB/another/pathC/another/pathD",
				s.substitute("1$(TEST_DOXY_A)$(TEST_DOXY_B)$(  TEST_DOXY_C)$  (TEST_DOXY_D)"));
		assertEquals(" /another/pathA/another/pathB/another/pathC/another/pathD", 
				s.substitute(" $(TEST_DOXY_A)$(TEST_DOXY_B)$(  TEST_DOXY_C)$  (TEST_DOXY_D)"));
		assertEquals("3/another/pathA /another/pathB/another/pathC/another/pathD", 
				s.substitute("3$(TEST_DOXY_A) $(TEST_DOXY_B)$(  TEST_DOXY_C)$  (TEST_DOXY_D)"));
		assertEquals("4/another/pathA /another/pathB /another/pathC/another/pathD   ", 
				s.substitute("4$(TEST_DOXY_A) $(TEST_DOXY_B) $(  TEST_DOXY_C)$  (TEST_DOXY_D)   "));
		assertEquals("5zzzz/another/pathAx/another/pathBy/another/pathCzzzz/another/pathD", 
				s.substitute("5zzzz$(TEST_DOXY_A)x$(TEST_DOXY_B)y$(  TEST_DOXY_C)zzzz$  (TEST_DOXY_D)"));
		
		assertEquals("path/my/doxygen/pathrestofpath", s.substitute("path$(TEST_DOXY_SUBS)restofpath"));
		assertEquals("/my/doxygen/path", s.substitute("$(TEST_DOXY_SUBS)"));
		assertEquals("path /my/doxygen/pathrestofpath", s.substitute("path $(TEST_DOXY_SUBS)restofpath"));
		assertEquals("path/my/doxygen/path restofpath", s.substitute("path$(TEST_DOXY_SUBS) restofpath"));
		assertEquals("path /my/doxygen/path restofpath", s.substitute("path $(TEST_DOXY_SUBS) restofpath"));
		
	}
	

	
	
	
	

}
