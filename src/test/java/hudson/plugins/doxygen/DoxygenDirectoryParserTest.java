package hudson.plugins.doxygen;

import static org.junit.Assert.assertEquals;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.plugins.doxygen.DoxygenArchiver.DoxygenArchiverDescriptor;
import hudson.remoting.VirtualChannel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Logger;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class DoxygenDirectoryParserTest extends AbstractWorkspaceTest {

    // TODO: move parsing-related tests from DoxygenArchiverTest to here.

    private Mockery context;
    private Mockery classContext;
    private BuildListener taskListener;
    private VirtualChannel virtualChannel;

    @Before
    public void setUp() throws Exception {
        super.createWorkspace();
        
        context = new Mockery();
        classContext = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        
        taskListener = context.mock(BuildListener.class);
        virtualChannel = context.mock(VirtualChannel.class);
    }

    @After
    public void tearDown() throws Exception {
        super.deleteWorkspace();
    }
	
    
    @Test
    public void retrieveDoxygenFromLoadFileWithVarSubst() throws Exception {
        	
        Logger LOGGER = Logger.getLogger(DoxygenDirectoryParserTest.class.getName());

    	String doxyFileName = "Doxyfile-env-expand";
    	
    	String expectedOutputDirectory = "exp-html-out";
    	String expectedHtmlDirectory = "exp-html";
    	
        
    	//Create the doxyfile with content in the temporary workspace
    	FileOutputStream fos = new FileOutputStream(new File(new FilePath(workspace,doxyFileName).toURI()));
    	fos.write(readAsString(doxyFileName).getBytes());
    	fos.close();
    	
    	//Create the generated doxygen directory
    	String commandDoxygenGeneratedDirectoryName=expectedOutputDirectory+"/"+expectedHtmlDirectory;
    	FilePath doxygenDir=new FilePath(workspace,commandDoxygenGeneratedDirectoryName);
    	doxygenDir.mkdirs();
    	
    	EnvVars env = new EnvVars();
    	
    	
    	env.addLine("EXP_OUTPUT_DIRECTORY=" + expectedOutputDirectory);
    	env.addLine("EXP_HTML_OUTPUT=" + expectedHtmlDirectory);
    	
    	
    	DoxygenDirectoryParser doxygenDirectoryParser =    
    			new DoxygenDirectoryParser(DoxygenArchiverDescriptor.DOXYGEN_DOXYFILE_PUBLISHTYPE, doxyFileName, "", "", env);
   	
        classContext.checking(new Expectations() {
            {
                ignoring(taskListener).getLogger();
                will(returnValue(new PrintStream(new ByteArrayOutputStream())));                
            }
        });
        
        LOGGER.info("parser=" + doxygenDirectoryParser);
                   
        FilePath resultDoxygenDirectory = doxygenDirectoryParser.invoke(parentFile, virtualChannel);
        
        LOGGER.info("resultDoxygenDirectory=" + resultDoxygenDirectory);
        LOGGER.info("doxygenDir=" + doxygenDir);
        
        assertEquals("The substituted doxygen directory is not correct", doxygenDir.toURI(),resultDoxygenDirectory.toURI()); 
        
        
        classContext.assertIsSatisfied();
        context.assertIsSatisfied();
    }       
}
