package hudson.plugins.doxygen;

import org.junit.Test;

import junit.framework.Assert;

public class DoxygenDirectoryParserTest {

    @Test
    public void testIsAbsolute() throws Exception {
        DoxygenDirectoryParser parser = new DoxygenDirectoryParser("", "", "");        
        // testData array contains arrays of test data where index 0 is the path
        // and index 1 is the expectedResult of isAbsolute
        // This test data will currently on work in a Unix environment
        System.out.println(System.getProperty("user.dir"));
        Object[][] testData = new Object[][] {
                { "doc", false },
                { "/usr", true },
                { "abcd123", false }, // nonexistent relative dir
                { "/foo/bar", true }, // nonexistent absolute dir
                { "/etc/passwd", true },      
                { "pom.xml", false }, // since tests are run from the trunk dir
                { "src", false }, // a dir in the trunk dir
        };
        
        for(Object[] testPair : testData ) {
            Boolean actual = parser.isDirectoryAbsolute((String)testPair[0]);
            Assert.assertEquals("For path:" + testPair[0], (Boolean)testPair[1], actual);
        }
    }
}
