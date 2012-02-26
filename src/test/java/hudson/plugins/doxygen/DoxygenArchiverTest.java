package hudson.plugins.doxygen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import hudson.AbortException;
import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.plugins.doxygen.DoxygenArchiver.DoxygenArchiverDescriptor;
import hudson.remoting.VirtualChannel;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
 
public class DoxygenArchiverTest extends AbstractWorkspaceTest {

     
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
    public void retrieveDoxygenDirectoryWithValidInput() throws Exception {
        	
        String doxygenFolderResultName = "doxygen-result";
    	FilePath doxygenDir=new FilePath(workspace,doxygenFolderResultName);
    	doxygenDir.mkdirs();
    	
     	
    	DoxygenDirectoryParser doxygenDirectoryParser =     		
    		new DoxygenDirectoryParser(DoxygenArchiverDescriptor.DOXYGEN_HTMLDIRECTORY_PUBLISHTYPE ,"",doxygenFolderResultName);
    	
        classContext.checking(new Expectations() {
            {
                ignoring(taskListener).getLogger();
                will(returnValue(new PrintStream(new ByteArrayOutputStream())));                
            }
        });
                       
        FilePath resultDoxygenDirectory = doxygenDirectoryParser.invoke(parentFile, virtualChannel);
                
        assertEquals("The computed doxygen directory is not correct", doxygenDir.toURI(),resultDoxygenDirectory.toURI()); 
        
        classContext.assertIsSatisfied();
        context.assertIsSatisfied();
    }

    @Test
    public void retrieveDoxygenDirectoryWithBlankInput() throws Exception {
        	
        String doxygenFolderResultName = "doxygen-result";
    	FilePath doxygenDir=new FilePath(workspace,doxygenFolderResultName);
    	doxygenDir.mkdirs();
     	
    	DoxygenDirectoryParser doxygenDirectoryParser =     		
    		new DoxygenDirectoryParser(DoxygenArchiverDescriptor.DOXYGEN_HTMLDIRECTORY_PUBLISHTYPE ,"","");
    	
        classContext.checking(new Expectations() {
            {
                ignoring(taskListener).getLogger();
                will(returnValue(new PrintStream(new ByteArrayOutputStream())));                
            }
        });
                   
        try{
        	doxygenDirectoryParser.invoke(parentFile, virtualChannel);
        	fail("Must throw an hudson.AbortException");
        }
        catch (IllegalArgumentException iae){
        	assertTrue(true);
        }
        
        
        classContext.assertIsSatisfied();
        context.assertIsSatisfied();
    }

    @Test
    public void retrieveDoxygenDirectoryWithWrongInput() throws Exception {
        	
        String doxygenFolderResultName = "doxygen-result";
    	FilePath doxygenDir=new FilePath(workspace,doxygenFolderResultName);
    	doxygenDir.mkdirs();
    	
     	
    	DoxygenDirectoryParser doxygenDirectoryParser =     		
    		new DoxygenDirectoryParser(DoxygenArchiverDescriptor.DOXYGEN_HTMLDIRECTORY_PUBLISHTYPE ,"","wrong");
    	
        classContext.checking(new Expectations() {
            {
                ignoring(taskListener).getLogger();
                will(returnValue(new PrintStream(new ByteArrayOutputStream())));                
            }
        });
                   
        try{
        	doxygenDirectoryParser.invoke(parentFile, virtualChannel);
        	fail("Must throw an hudson.AbortException");
        }
        catch (AbortException iae){
        	assertTrue(true);
        }
        
        
        classContext.assertIsSatisfied();
        context.assertIsSatisfied();
    }
    
    
    private String readAsString(String resourceName) throws IOException {
        String contentString = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(resourceName)));
        String line = reader.readLine();
        while (line != null) {
        	contentString += line + "\n";
            line = reader.readLine();
        }
        reader.close();

        return contentString;
    }

    
    
    @Test
    public void retrieveDoxygenFromLoadFileWithValidInput() throws Exception {
        	
        
    	//Create the doxyfile with content in the temporary workspace
    	FileOutputStream fos = new FileOutputStream(new File(new FilePath(workspace,"Doxyfile").toURI()));
    	fos.write(readAsString("Doxyfile").getBytes());
    	fos.close();
    	
    	//Create the generated doxygen directory
    	String commandDoxygenGeneratedDirectoryName="html-out/html";
    	FilePath doxygenDir=new FilePath(workspace,commandDoxygenGeneratedDirectoryName);
    	doxygenDir.mkdirs();
    	
    	
    	DoxygenDirectoryParser doxygenDirectoryParser =     		
    		new DoxygenDirectoryParser(DoxygenArchiverDescriptor.DOXYGEN_DOXYFILE_PUBLISHTYPE ,"Doxyfile","");
    	
        classContext.checking(new Expectations() {
            {
                ignoring(taskListener).getLogger();
                will(returnValue(new PrintStream(new ByteArrayOutputStream())));                
            }
        });
                   
        FilePath resultDoxygenDirectory = doxygenDirectoryParser.invoke(parentFile, virtualChannel);
        assertEquals("The computed doxygen directory is not correct", doxygenDir.toURI(),resultDoxygenDirectory.toURI()); 
        
        
        classContext.assertIsSatisfied();
        context.assertIsSatisfied();
    }    
    
    @Test
    public void pathIsRelativeWhenNoParent() throws Exception {
    	// Arrange
    	DoxygenDirectoryParser parser = new DoxygenDirectoryParser(DoxygenArchiverDescriptor.DOXYGEN_DOXYFILE_PUBLISHTYPE, "Doxyfile", "");
    	
    	// Act
    	// A final computed directory like "html" has no parent.
    	Boolean absolute = parser.isDirectoryAbsolute(workspace, "html");
    	
    	// Assert
    	Assert.assertFalse("When no parent is in the path, it is not absolute.", absolute);
    }
}
