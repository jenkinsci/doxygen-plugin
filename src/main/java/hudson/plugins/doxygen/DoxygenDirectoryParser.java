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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DoxygenDirectoryParser implements FilePath.FileCallable<FilePath>, Serializable{


	private static final long serialVersionUID = 1L;
	
    private static final Logger LOGGER = Logger.getLogger(DoxygenDirectoryParser.class.getName());
	
    private transient Map<String, String> doxyfileInfos = new HashMap<String, String>();
	
	private static final String DOXYGEN_KEY_OUTPUT_DIRECTORY =  "OUTPUT_DIRECTORY";
    private static final String DOXYGEN_KEY_GENERATE_HTML    =  "GENERATE_HTML";
    private static final String DOXYGEN_KEY_HTML_OUTPUT      =  "HTML_OUTPUT";
    private static final String DOXYGEN_DEFAULT_HTML_OUTPUT  =  "html";
    
    private static final String DOXYGEN_VALUE_YES            =  "YES";
   
    private String publishType;
    private String doxygenHtmlDirectory;
    private String doxyfilePath;
    
    public DoxygenDirectoryParser(String publishType, String doxyfilePath, String doxygenHtmlDirectory){
    	this.publishType=publishType;
    	this.doxyfilePath=doxyfilePath;
    	this.doxygenHtmlDirectory=doxygenHtmlDirectory;
    }
    
    public FilePath invoke(java.io.File workspace, VirtualChannel channel) throws IOException {
    	    	
    	try{
    		return (DoxygenArchiverDescriptor.DOXYGEN_HTMLDIRECTORY_PUBLISHTYPE).equals(publishType)
    			?retrieveDoxygenDirectoryFromHudsonConfiguration(doxygenHtmlDirectory, new FilePath(workspace))
    			:retrieveDoxygenDirectoryFromDoxyfile(doxyfilePath,new FilePath(workspace));
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
	private FilePath retrieveDoxygenDirectoryFromHudsonConfiguration(String doxygenHtmlDirectory, FilePath base) throws InterruptedException,IOException {
		
		FilePath doxygenGeneratedDir = null;
		
		LOGGER.log(Level.INFO,"Using the Doxygen HTML directory specified by the configuration.");    	
		
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
    private  FilePath getDoxygenGeneratedDir(FilePath base) {
        
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
    		LOGGER.log(Level.INFO,"The "+DOXYGEN_KEY_HTML_OUTPUT+" tag is not present or is left blank." + DOXYGEN_DEFAULT_HTML_OUTPUT+ " will be used as the default path.");
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
    private void loadDoxyFile(FilePath doxyfilePath) 
    throws FileNotFoundException, IOException, InterruptedException{
    
    	LOGGER.log(Level.INFO,"The Doxyfile path is '"+doxyfilePath.toURI()+"'.");
    	
    	final String separator = "=";
		InputStream ips=new FileInputStream(new File(doxyfilePath.toURI())); 
		InputStreamReader ipsr=new InputStreamReader(ips);
		BufferedReader br=new BufferedReader(ipsr);
		String line=null;
		
		List<String> doxyfileDirectories = new ArrayList<String>();
		if (doxyfileInfos==null){
			doxyfileInfos=new HashMap<String, String>();
		}
		while ((line=br.readLine())!=null){
			if (line.indexOf(separator)!=-1){
				String[] elements = line.split(separator);

				if (elements[0].startsWith("@INCLUDE_PATH")){
					 Collections.addAll(doxyfileDirectories,(elements[1].split(" ")));
				}
				else if (elements[0].startsWith("@INCLUDE")){
					processIncludeFile(doxyfileDirectories, doxyfilePath.getParent(), elements[1].trim());
				}
				else{
					doxyfileInfos.put(elements[0].trim(), elements[1].trim());
				}
			}

			
		}
		br.close(); 
		ipsr.close();
		ips.close();
    }    
	
    private void processIncludeFile(List<String> doxyfileDirectories, FilePath parentFile, String includeStr) 
    throws FileNotFoundException, IOException, InterruptedException{

    	boolean find = false;
    	
    	if (doxyfileDirectories==null || doxyfileDirectories.isEmpty()){
        	FilePath includedFilePath = new FilePath(parentFile,includeStr);
        	if (!includedFilePath.exists()){
        		throw new AbortException("Doxyfile is incorrect. Included file '" + includeStr + "' doesn't exist.");
        	}
        	
        	loadDoxyFile(includedFilePath);
    	}
    	else{
    		for (String doxyfileDirectory : doxyfileDirectories){
    			
    			FilePath directoryFilePath=new FilePath(parentFile,doxyfileDirectory);
    			if (!directoryFilePath.exists()){
    				continue;
    			}
    			FilePath includedFilePath= new FilePath(directoryFilePath,includeStr);
    			if (!includedFilePath.exists()){
    				continue;
    			}
    			else {
    				loadDoxyFile(includedFilePath);	
    				find = true;
    				break;
    			}
    		}
    		if (!find){
            	FilePath includedFilePath = new FilePath(parentFile,includeStr);
            	if (!includedFilePath.exists()){
            		throw new AbortException("Doxyfile is incorrect. Included file '" + includeStr + "' doesn't exist.");
            	}
            	
            	loadDoxyFile(includedFilePath);
    		}
    	}
    	

	}

	/**
     * Retrieve the generated doxygen HTML directory from Doxyfile
     */
	private FilePath retrieveDoxygenDirectoryFromDoxyfile(String doxyfilePath, FilePath base)
	throws FileNotFoundException, IOException, InterruptedException {
		
		FilePath doxygenGeneratedDir;
		
		LOGGER.log(Level.INFO,"Using the Doxyfile information.");
		
		//Load the Doxyfile
		loadDoxyFile(base.child(doxyfilePath));
		
		//Process if the generate htnl tag is set to 'YES'
		if (isDoxygenGenerateHtml()){
			
			//Retrieve the generated doxygen directory from the build
			doxygenGeneratedDir = getDoxygenGeneratedDir(base);                
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
