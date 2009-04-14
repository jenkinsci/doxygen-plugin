package hudson.plugins.doxygen;

import hudson.AbortException;
import hudson.FilePath;
import hudson.plugins.doxygen.DoxygenArchiver.DoxygenArchiverDescriptor;
import hudson.remoting.VirtualChannel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class DoxygenDirectoryParser implements FilePath.FileCallable<FilePath>, Serializable{


	private static final long serialVersionUID = 1L;
	
    private transient Map<String, String> doxyfileInfos = new HashMap<String, String>();
	
	private static final String DOXYGEN_KEY_OUTPUT_DIRECTORY =  "OUTPUT_DIRECTORY";
    private static final String DOXYGEN_KEY_GENERATE_HTML    =  "GENERATE_HTML";
    private static final String DOXYGEN_KEY_HTML_OUTPUT      =  "HTML_OUTPUT";
    private static final String DOXYGEN_DEFAULT_HTML_OUTPUT  =  "html";
    
    private static final String DOXYGEN_VALUE_YES            =  "YES";
   
	
    private PrintStream logger;
    private String publishType;
    private String doxygenHtmlDirectory;
    private String doxyfilePath;
    
    public DoxygenDirectoryParser(PrintStream logger, String publishType, String doxyfilePath, String doxygenHtmlDirectory){
    	this.logger=logger;
    	this.publishType=publishType;
    	this.doxyfilePath=doxyfilePath;
    	this.doxygenHtmlDirectory=doxygenHtmlDirectory;
    }
    
    public FilePath invoke(java.io.File workspace, VirtualChannel channel) throws IOException {
    	    	
    	try{
    		return (DoxygenArchiverDescriptor.DOXYGEN_HTMLDIRECTORY_PUBLISHTYPE).equals(publishType)
    			?retrieveDoxygenDirectoryFromHudsonConfiguration(doxygenHtmlDirectory, new FilePath(workspace),logger)
    			:retrieveDoxygenDirectoryFromDoxyfile(doxyfilePath,new FilePath(workspace),logger);
    	}
    	catch (InterruptedException ie){
    		throw new AbortException(ie.getMessage());
    	}    	
    }
    
    
    /**
     * Determine if Doxygen generate HTML reports 
     */
    private boolean isDoxygenGenerateHtml(){    	
    	if (doxyfileInfos==null)
    		return false;
    	
    	String generatedHtmlKeyVal = doxyfileInfos.get(DOXYGEN_KEY_GENERATE_HTML);
    	
    	// If the 'GENERATE_HTML Key is not present, by default the HTML generated documentation is actived.
    	if (generatedHtmlKeyVal==null){
    		return true;
    	}
    	
    	return DOXYGEN_VALUE_YES.equalsIgnoreCase(generatedHtmlKeyVal);
    }
    
    /**
     * Retrieve the generated doxygen HTML directory from Hudson configuration given by the user
     */
	private FilePath retrieveDoxygenDirectoryFromHudsonConfiguration(String doxygenHtmlDirectory, FilePath base, PrintStream logger) throws InterruptedException,IOException {
		
		FilePath doxygenGeneratedDir = null;
		
		logger.println("Using the Doxygen HTML directory specified by the configuration.");    	
		
		if (doxygenHtmlDirectory==null){
			throw new IllegalArgumentException("Error on the given doxygen html directory.");
		}

		if (doxygenHtmlDirectory.trim().length()==0){
			throw new IllegalArgumentException("Error on the given doxygen html directory.");
		}

		doxygenGeneratedDir =  new FilePath(base,doxygenHtmlDirectory); 
		if (!doxygenGeneratedDir.exists()){
	        throw new AbortException("The directory '"+ doxygenGeneratedDir + "' doesn't exist.");
		}
		return doxygenGeneratedDir;		
	}    
    
    /**
     * Gets the directory where the Doxygen is generated for the given build.
     */
    private  FilePath getDoxygenGeneratedDir(FilePath base, PrintStream logger) {
        
    	if (doxyfileInfos==null)
    		return null;
    	    	
    	String outputDirectory = doxyfileInfos.get(DOXYGEN_KEY_OUTPUT_DIRECTORY);    	
    	String doxyGenDir = null;
    	if (outputDirectory!= null && outputDirectory.trim().length() != 0){
    		doxyGenDir = outputDirectory;    		
    	}
    	
    	String outputHTML      = doxyfileInfos.get(DOXYGEN_KEY_HTML_OUTPUT);
    	if (outputHTML== null || outputHTML.trim().length() == 0){
    		outputHTML = "html";
    		logger.println("The "+DOXYGEN_KEY_HTML_OUTPUT+" tag is not present or is left blank." + DOXYGEN_DEFAULT_HTML_OUTPUT+ " will be used as the default path.");
    	}
    	else {
    		doxyGenDir = (doxyGenDir!=null)?(doxyGenDir+ File.separator + outputHTML):outputHTML;
    		return new FilePath(base, doxyGenDir);
    	}    	
    	
    	return null;
    }  
    
    /**
     * Load the Doxyfile Doxygen file in memory
     */
    private void loadDoxyFile(FilePath doxyfilePath, PrintStream logger) 
    throws FileNotFoundException, IOException, InterruptedException{
    
    	logger.println("The Doxyfile path is '"+doxyfilePath.toURI()+"'.");
    	
    	final String separator = "=";
		InputStream ips=new FileInputStream(new File(doxyfilePath.toURI())); 
		InputStreamReader ipsr=new InputStreamReader(ips);
		BufferedReader br=new BufferedReader(ipsr);
		String line=null;
		if (doxyfileInfos==null){
			doxyfileInfos=new HashMap<String, String>();
		}
		while ((line=br.readLine())!=null){
			if (line.indexOf(separator)!=-1){
				String[] elements = line.split(separator);
				doxyfileInfos.put(elements[0].trim(), elements[1].trim());
			}
		}
		br.close(); 
		ipsr.close();
		ips.close();
    }    
	
    /**
     * Retrieve the generated doxygen HTML directory from Doxyfile
     */
	private FilePath retrieveDoxygenDirectoryFromDoxyfile(String doxyfilePath, FilePath base, PrintStream logger)
	throws FileNotFoundException, IOException, InterruptedException {
		
		FilePath doxygenGeneratedDir;
		
		logger.println("Using the Doxyfile information.");
		
		//Load the Doxyfile
		loadDoxyFile(base.child(doxyfilePath),logger);
		
		//Process if the generate htnl tag is set to 'YES'
		if (isDoxygenGenerateHtml()){
			
			//Retrieve the generated doxygen directory from the build
			doxygenGeneratedDir = getDoxygenGeneratedDir(base,logger);                
			if (!doxygenGeneratedDir.exists()){
		        throw new AbortException("The directory '"+ doxygenGeneratedDir + "' doesn't exist.");
			}
		}
		else {			
			//The GENERATE_HTML tag is not set to 'YES'
			throw new AbortException("The tag "+DOXYGEN_KEY_GENERATE_HTML+" is not set to '" + DOXYGEN_VALUE_YES+ "'. The Doxygen plugin publishes only HTML documentations.");
		}
		
		return doxygenGeneratedDir;
	}    
    
}
