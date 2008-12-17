package hudson.plugins.doxygen;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.maven.AbstractMavenProject;
import hudson.model.AbstractBuild;
import hudson.model.AbstractItem;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.DirectoryBrowserSupport;
import hudson.model.Project;
import hudson.model.ProminentProjectAction;
import hudson.model.Result;
import hudson.model.Run;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * 
 * @author Gregory Boissinot
 * @version 1.0 Initial Version
 */
public class DoxygenArchiver extends Publisher {

    /**
     * Path to the Doxyfile file.
     */    
    private final String doxyfilePath;    

    /**
     * If true, retain doxygen for all the successful builds.
     */
    private final boolean keepAll;
    
    
    public final static Descriptor<Publisher> DESCRIPTOR = new DoxygenArchiverDescriptor();

    private static final String DOXYGEN_KEY_OUTPUT_DIRECTORY =  "OUTPUT_DIRECTORY";
    private static final String DOXYGEN_KEY_GENERATE_HTML    =  "GENERATE_HTML";
    private static final String DOXYGEN_KEY_HTML_OUTPUT      =  "HTML_OUTPUT";
    private static final String DOXYGEN_VALUE_YES            =  "YES";
   

    private Map<String, String> doxyfileInfos = new HashMap<String, String>();
	
    
	public String getDoxyfilePath(){
		return doxyfilePath;
	}	
	
    public boolean isKeepAll() {
        return keepAll;
    }
	
    
    public static final class DoxygenArchiverDescriptor extends BuildStepDescriptor<Publisher>{

        public DoxygenArchiverDescriptor() {
            super(DoxygenArchiver.class);
        }

        @Override
        public String getDisplayName() {
            return "Publish Doxygen";
        }

        @Override
        public Publisher newInstance(StaplerRequest req) throws FormException {
            Publisher p = new DoxygenArchiver(
                    req.getParameter("doxygen.doxyfilePath"),
                    req.getParameter("doxygen.keepall")!=null);
            return p;
        }

        @Override
        public String getHelpFile() {
            return "/plugin/doxygen/help.html";
        }
        
        
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            // Only generate Doxygen Archiver for free-style projects
            return !AbstractMavenProject.class.isAssignableFrom(jobType);
        }      
    }

    private DoxygenArchiver(final String doxyfilePath, boolean keepAll) {
    	this.doxyfilePath = doxyfilePath.trim();
    	this.keepAll= keepAll;
    }

    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
    }

    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        return true;
    }
        

    
    /**
     * Load the Doxyfile Doxygen file in memory
     */
    private void loadDoxyFile(FilePath doxyfilePath) 
    throws FileNotFoundException, IOException, InterruptedException{
    
    	final String separator = "=";
		InputStream ips=new FileInputStream(new File(doxyfilePath.toURI())); 
		InputStreamReader ipsr=new InputStreamReader(ips);
		BufferedReader br=new BufferedReader(ipsr);
		String line;
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
     * Determine if Doxygen generate HTML reports 
     */
    private boolean isDoxygenGenerateHtml(){
    	
    	String valGenerateHtml = doxyfileInfos.get(DOXYGEN_KEY_GENERATE_HTML);
    	
    	if (valGenerateHtml!=null)
    		return valGenerateHtml.equalsIgnoreCase(DOXYGEN_VALUE_YES);
    	
    	return false;	
    }
    
    /**
     * Gets the directory where the Doxygen is generated for the given build.
     */
    private  FilePath getDoxygenGeneratedDir(AbstractBuild<?,?> build) {
        
    	String outputHTML      = doxyfileInfos.get(DOXYGEN_KEY_HTML_OUTPUT);
    	String outputDirectory = doxyfileInfos.get(DOXYGEN_KEY_OUTPUT_DIRECTORY);
    	
    	String doxyGenDir = null;
    	if (outputDirectory!= null && outputDirectory.trim().length() != 0){
    		doxyGenDir = outputDirectory;    		
    	}
    	    	   
    	if (outputHTML!= null && outputHTML.trim().length() != 0){
    		doxyGenDir = (doxyGenDir!=null)?(doxyGenDir+ File.separator + outputHTML):outputHTML;
    		return build.getParent().getModuleRoot().child(doxyGenDir);
    	}    	
    	
    	return null;
    }        
    
    /**
     * Gets the directory where the Doxygen is stored for the given project.
     */
    private static File getDoxygenDir(AbstractItem project) {
        return new File(project.getRootDir(),"doxygen/html");
    }

    /**
     * Gets the directory where the Doxygen is stored for the given build.
     */
    private static File getDoxygenDir(Run run) {
        return new File(run.getRootDir(),"doxygen/html");
    }    
    
    @SuppressWarnings("unchecked")
    @Override
    public boolean perform(AbstractBuild<?,?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
    	    
    	//Load the Doxyfile
    	loadDoxyFile(build.getParent().getWorkspace().child(doxyfilePath));
    	
    	//Process if the generate htnl tag is set to 'YES'
    	if (isDoxygenGenerateHtml()){
    		
    		//Retrieve the generate doxygen directory from the build
            FilePath doxygenGenerateDir = getDoxygenGeneratedDir(build);
            
            //Determine the stored doxygen directory
            FilePath target = new FilePath(keepAll ? getDoxygenDir(build) : getDoxygenDir(build.getProject()));    	

            try {
                if (doxygenGenerateDir.copyRecursiveTo("**/*",target)==0) {
                    if(build.getResult().isBetterOrEqualTo(Result.UNSTABLE)) {
                        // If the build failed, don't complain that there was no javadoc.
                        // The build probably didn't even get to the point where it produces javadoc. 
                    }
                    build.setResult(Result.FAILURE);
                    return true;
                }
            } catch (IOException e) {
                Util.displayIOException(e,listener);
                e.printStackTrace(listener.fatalError("error"));
                build.setResult(Result.FAILURE);
                return true;
            }
            
            // add build action, if doxygen is recorded for each build
            if(keepAll)                
            	build.addAction(new DoxygenBuildAction(build));		
    	}

		return true;
	}


    public Descriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

    public Action getProjectAction(Project project) {
        return new DoxygenAction(project);
    }



 
    protected static abstract class BaseDoxygenAction implements Action {
        public String getUrlName() {
            return "doxygen";
        }

        public String getDisplayName() {
        	return "DoxyGen HTML";
        }

        public String getIconFileName() {
            if(dir().exists())
                return "help.gif";
            else
                // hide it since we don't have doxygen yet.
                return null;
        }

        public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, InterruptedException {
            new DirectoryBrowserSupport(this, getTitle())
                .serveFile(req, rsp, new FilePath(dir()), "help.gif", false);
        }

        protected abstract String getTitle();

        protected abstract File dir();
    }

    public static class DoxygenAction extends BaseDoxygenAction implements ProminentProjectAction {
        private final AbstractItem project;

        public DoxygenAction(AbstractItem project) {
            this.project = project;
        }

        protected File dir() {

            if (project instanceof AbstractProject) {
                AbstractProject abstractProject = (AbstractProject) project;

                Run run = abstractProject.getLastSuccessfulBuild();
                if (run != null) {
                    File doxygenDir = getDoxygenDir(run);

                    if (doxygenDir.exists())
                        return doxygenDir;
                }
            }

            return getDoxygenDir(project);
        }

        protected String getTitle() {
            return project.getDisplayName()+" doxygen";
        }
    }
    
    public static class DoxygenBuildAction extends BaseDoxygenAction {
    	private final AbstractBuild<?,?> build;
    	
    	public DoxygenBuildAction(AbstractBuild<?,?> build) {
    	    this.build = build;
    	}

        protected String getTitle() {
            return build.getDisplayName()+" doxygen/html";
        }

        protected File dir() {
            return new File(build.getRootDir(),"doxygen/html");
        }
    }    
        
}
