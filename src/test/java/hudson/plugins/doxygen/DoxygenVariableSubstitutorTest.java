package hudson.plugins.doxygen;

import static org.junit.Assert.assertEquals;


import org.junit.Test;


public class DoxygenVariableSubstitutorTest {

	@Test
	public void test() {
			
		DoxygenVariableSubstitutor s = new DoxygenVariableSubstitutor();
			
		assertEquals(s.substitute("$(SUBS)"), "$SUBS");
		assertEquals(s.substitute("$ (SUBS)"), "$SUBS");
		assertEquals(s.substitute("$ ( SUBS)"), "$SUBS");
		assertEquals(s.substitute("$ ( SUBS )"), "$SUBS");
		assertEquals(s.substitute("$ (SUBS )"), "$SUBS");
		assertEquals(s.substitute("$( SUBS )"), "$SUBS");
		assertEquals(s.substitute("$(SUBS )"), "$SUBS");

		assertEquals(s.substitute("$(a)$(b)$(  c)$  (q)"), "$a$b$c$q");
		assertEquals(s.substitute(" $(a)$(b)$(  c)$  (q)"), " $a$b$c$q");
		assertEquals(s.substitute("$(a) $(b)$(  c)$  (q)"), "$a $b$c$q");
		assertEquals(s.substitute("$(a) $(b) $(  c)$  (q)   "), "$a $b $c$q   ");
		assertEquals(s.substitute("zzzz$(a)x$(b)y$(  c)zzzz$  (q)"), "zzzz$ax$by$czzzz$q");
		
		assertEquals(s.substitute("path$(SUBS)restofpath"), "path$SUBSrestofpath");
		assertEquals(s.substitute("$(SUBS)"), "$SUBS");
		assertEquals(s.substitute("path $(SUBS)restofpath"), "path $SUBSrestofpath");
		assertEquals(s.substitute("path$(SUBS) restofpath"), "path$SUBS restofpath");
		assertEquals(s.substitute("path $(SUBS) restofpath"), "path $SUBS restofpath");
	}

}
