package hudson.plugins.doxygen;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixRun;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.ProminentProjectAction;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractItem;
import hudson.model.AbstractProject;
import hudson.model.DirectoryBrowserSupport;
import hudson.model.Hudson;
import hudson.model.Label;
import hudson.model.Run;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * 
 * @author Gregory Boissinot
 */
public class DoxygenArchiver extends Recorder implements Serializable,MatrixAggregatable  {

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = Logger.getLogger(DoxygenArchiver.class.getName());
	
	@Extension
	public static final DoxygenArchiverDescriptor DESCRIPTOR = new DoxygenArchiverDescriptor();

	/**
	 * Path to the Doxyfile file.
	 */
	private final String doxyfilePath;

	/**
	 * If true, retain doxygen for all the successful builds.
	 */
	private final boolean keepAll;

	/**
	 * The publishing type : with the doxyfile or directly the html directory
	 */ 
	private transient String publishType;
	
	
	public String runOnChild;
	
	public String folderWhereYouRunDoxygen;

	/**
	 * The doxygen html directory
	 */
	private transient String doxygenHtmlDirectory;

	public String getDoxyfilePath() {
		return doxyfilePath;
	}

	public boolean isKeepAll() {
		return keepAll;
	}

    @Deprecated
	public String getPublishType() {
		return publishType;
	}

    @Deprecated
	public String getDoxygenHtmlDirectory() {
		return doxygenHtmlDirectory;
	}

	public static final class DoxygenArchiverDescriptor extends
			BuildStepDescriptor<Publisher> {

		public static final String DOXYGEN_DOXYFILE_PUBLISHTYPE = "DoxyFile";
		public static final String DOXYGEN_HTMLDIRECTORY_PUBLISHTYPE = "HtmlDirectory";
		public static final String DEFAULT_DOXYGEN_PUBLISHTYPE = DOXYGEN_DOXYFILE_PUBLISHTYPE;

		public DoxygenArchiverDescriptor() {
			super(DoxygenArchiver.class);
		}

		@Override
		public String getDisplayName() {
			return "Publish Doxygen";
		}

		@Override
		public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
			return req.bindJSON(DoxygenArchiver.class, formData);
		}
        
        public FormValidation doCheckDoxyfilePath(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            FilePath ws = project.getSomeWorkspace();
            return ws!=null ? ws.validateFileMask(value, true) : FormValidation.ok();
        }

		@Override
		public String getHelpFile() {
			return "/plugin/doxygen/help.html";
		}

		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}
		
		public boolean isMatrixProject(AbstractProject<?, ?> project) {
			return project instanceof MatrixProject;
		}
				
	}

	@DataBoundConstructor
	public DoxygenArchiver(String doxyfilePath, boolean keepAll,String runOnChild,String folderWhereYouRunDoxygen) {
		this.doxyfilePath = doxyfilePath.trim();
		this.keepAll = keepAll;
		this.runOnChild = (null != runOnChild)?runOnChild.trim():null;
		this.folderWhereYouRunDoxygen = folderWhereYouRunDoxygen;
	}

	@Override
	public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
		return true;
	}

	/**
	 * Gets the directory where the Doxygen is stored for the given project.
	 */
	private static File getDoxygenDir(AbstractItem project) {
		return new File(project.getRootDir(), "doxygen/html");
	}

	/**
	 * Gets the directory where the Doxygen is stored for the given build.
	 */
	private static File getDoxygenDir(Run run) {
		return new File(run.getRootDir(), "doxygen/html");
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {

		// Check if this project is a matrix project .. then build will be taken from some child and not from here 
		if (! (build.getProject() instanceof MatrixConfiguration)){
			return _perform(build,launcher,listener);
		}
		return true;
	}

	
	private boolean _perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener){
		
		if ((build.getResult().equals(Result.SUCCESS))
				|| (build.getResult().equals(Result.UNSTABLE))) {

			listener.getLogger().println("Publishing Doxygen HTML results.");
			
			try {

				EnvVars environment = build.getEnvironment(listener);
				
				DoxygenDirectoryParser parser = new DoxygenDirectoryParser(
						publishType, doxyfilePath, doxygenHtmlDirectory,folderWhereYouRunDoxygen, environment, listener);
				
				
				
				// If we are matrix project then we will take from the node as we passed,
				// otherwise from current build 
				FilePath doxygenGeneratedDir = null;
				if ((getDescriptor().isMatrixProject(build.getProject())) && (null != runOnChild)){
					MatrixBuild _thebuild = (MatrixBuild)build;					
					Label childLabel = Hudson.getInstance().getLabel(runOnChild);
					
					// If label is an instance label it is easy to locate the computer .. but if label is 
					// a group label, we need to find some slave that runs the build and that it is assigned to 
					// that label
					for (MatrixRun run : _thebuild.getRuns()){
						// Check if this run runs on the node that is assigned this label 
						if (run.getBuiltOn().getAssignedLabels().contains(childLabel)){														
							doxygenGeneratedDir = run.getWorkspace().act(parser);
                            listener.getLogger().println("Selected node is " + run.getBuiltOn().getDisplayName());
							break;
						}
					} 	
					// If we got here and did not assign the directory .. it means that build does not run on this node, or group of nodes 
					if (null == doxygenGeneratedDir){
						LOGGER.log(Level.CONFIG,"Project " + build.getProject().getDisplayName() + " is not build on any node that is assigned label " + runOnChild);
						throw new AbortException("Build does not run on any node with label" + runOnChild);
					}
				}else{
					doxygenGeneratedDir = build.getWorkspace().act(parser);
				}


				listener.getLogger().println(
						"The determined Doxygen directory is '" + doxygenGeneratedDir + "'.");

				// Determine the future stored doxygen directory
				FilePath target = new FilePath(keepAll ? getDoxygenDir(build)
						: getDoxygenDir(build.getProject()));

				if (doxygenGeneratedDir.copyRecursiveTo("**/*", target) == 0) {
					if (build.getResult().isBetterOrEqualTo(Result.UNSTABLE)) {
						// If the build failed, don't complain that there was no
						// javadoc.
						// The build probably didn't even get to the point where
						// it produces javadoc.
					}

					listener.getLogger().println(
							"Failure to copy the generated doxygen html documentation at '"
									+ doxygenHtmlDirectory + "' to '" + target
									+ "'");

					build.setResult(Result.FAILURE);
					return true;
				}

				// add build action, if doxygen is recorded for each build
				if (keepAll)
					build.addAction(new DoxygenBuildAction(build));

			} catch (Exception e) {
				e.printStackTrace(listener.fatalError("error"));
				build.setResult(Result.FAILURE);
				return true;
			}

			listener.getLogger().println("End publishing Doxygen HTML results.");
		}
		else{
			listener.getLogger().println("Build failed. Publishing Doxygen skipped.");
		}
		return true;
	}
	
	
	
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.STEP;
	}

	@Override
	public DoxygenArchiverDescriptor getDescriptor() {
		return DESCRIPTOR;
	}

	@Override
	public Action getProjectAction(AbstractProject<?, ?> project) {
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
			if (dir().exists())
				return "help.gif";
			else
				// hide it since we don't have doxygen yet.
				return null;
		}


        public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            DirectoryBrowserSupport dbs = new DirectoryBrowserSupport(this, new FilePath(this.dir()), this.getTitle(), "graph.gif", false);
            dbs.generateResponse(req, rsp, this);
        }
        


		protected abstract String getTitle();

		protected abstract File dir();
	}

	public static class DoxygenAction extends BaseDoxygenAction implements
			ProminentProjectAction {
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
			return project.getDisplayName() + " doxygen";
		}
	}

	public static class DoxygenBuildAction extends BaseDoxygenAction {
		private final AbstractBuild<?, ?> build;

		public DoxygenBuildAction(AbstractBuild<?, ?> build) {
			this.build = build;
		}

		protected String getTitle() {
			return build.getDisplayName() + " doxygen/html";
		}

		protected File dir() {
			return new File(build.getRootDir(), "doxygen/html");
		}
	}

	public MatrixAggregator createAggregator(MatrixBuild build,
			Launcher launcher, BuildListener listener) {
		
		return new MatrixAggregator(build,launcher,listener) {
			
			 public boolean endBuild() throws InterruptedException, IOException {
				 return DoxygenArchiver.this._perform(build, launcher, listener);
			 }
		};
	}

}
