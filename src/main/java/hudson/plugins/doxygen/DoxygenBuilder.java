package hudson.plugins.doxygen;

import hudson.CopyOnWrite;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;
import java.io.IOException;
import org.kohsuke.stapler.DataBoundConstructor;



/**
 * Implementation of the "Generate documentation using Doxygen" build step
 */
public class DoxygenBuilder extends Builder {
    private final String doxyfilePath;
    private final String installationName;
    private final boolean continueOnBuildFailure;
    private final boolean unstableIfWarnings;
    
    @DataBoundConstructor
    public DoxygenBuilder(String doxyfilePath, String installationName, boolean continueOnBuildFailure, boolean unstableIfWarnings) {
        this.doxyfilePath = doxyfilePath;
        this.installationName = installationName;
        this.continueOnBuildFailure = continueOnBuildFailure;
        this.unstableIfWarnings = unstableIfWarnings;
    }
    
    @SuppressWarnings("unused")
    public String getDoxyfilePath() {
        return this.doxyfilePath;
    }

    @SuppressWarnings("unused")
    public String getInstallationName() {
        return installationName;
    }

    @SuppressWarnings("unused")
    public boolean getContinueOnBuildFailure() {
        return continueOnBuildFailure;
    }

    @SuppressWarnings("unused")
    public boolean getUnstableIfWarnings() {
        return unstableIfWarnings;
    }
    
    /**
     * Finds the doxygen installation to use for this build among all installations configured in the Jenkins administration
     * @return selected Doxygen installation
     */
    public DoxygenInstallation getSelectedInstallation() {
        DescriptorImpl descriptor = (DescriptorImpl)getDescriptor();
       
        for (DoxygenInstallation i : descriptor.getInstallations()) {
            if (installationName != null && i.getName().equals(installationName))
                return i;
        }

        return null;
    }
    
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        listener.getLogger().println("Starting Doxygen documentation generation");
        DoxygenInstallation installToUse = getSelectedInstallation();

        // Raise an error if the doxygen installation isn't found
        if (installToUse == null) {
            listener.getLogger().println("There is no Doxygen installation selected. Please review the build step configuration.");
            return false;
        }
        
        // Get the doxygen executable path
        EnvVars envVariables = build.getEnvironment(listener);
        installToUse = installToUse.forNode(Computer.currentComputer().getNode(), listener);
        installToUse = installToUse.forEnvironment(envVariables);
        String pathToDoxygen = installToUse.getHome();

        // Raise an error if the doxygen executable isn't found
        FilePath doxygenPath = new FilePath(launcher.getChannel(), pathToDoxygen);
        try {
            if (!doxygenPath.exists()) {
                listener.fatalError(String.format("The path to Doxygen executable doesn't exist : \"%s\"", doxygenPath));
                return false;
            }
        } catch (IOException e) {
            listener.fatalError(String.format("Failed checking the existence of Doxygen executable : \"%s\"", doxygenPath));
            Util.displayIOException(e, listener);
            return false;
        }
        
        // Raise an error if the doxyfile isn't set
        String pathToDoxyfile = getDoxyfilePath();
        if (pathToDoxyfile == null || pathToDoxyfile.trim().length() == 0)
        {
            listener.fatalError("The Doxyfile path is empty. Please review the build step configuration.");
        }
        
        // Raise an error if the doxyfile path doesn't exists
        FilePath doxyfile = new FilePath(launcher.getChannel(), pathToDoxygen);
        try {
            if (!doxyfile.exists()) {
                listener.fatalError(String.format("The path to Doxyfile doesn't exist : \"%s\"", doxyfile));
                return false;
            }
        } catch (IOException e) {
            listener.fatalError(String.format("Failed checking the existence of the Doxyfile : \"%s\"", doxyfile));
            Util.displayIOException(e, listener);
            return false;
        }
        
        // Build the command line to run
        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add(pathToDoxygen);
        args.add(pathToDoxyfile);
        FilePath executionDirectory = build.getWorkspace();

        try {
            listener.getLogger().println(String.format("Executing the command %s from %s", args.toStringWithQuote(), executionDirectory));
            // Doxygen output parser to handle warnings and errors
            DoxygenConsoleParser outputParser = new DoxygenConsoleParser(listener.getLogger(), build.getCharset());
            // Run the doxygen command line
            int r = launcher.launch().cmds(args).envs(envVariables).stdout(outputParser).pwd(executionDirectory).join();
            // Output doxygen execution result
            listener.getLogger().println(String.format("Doxygen documentation generation ended with %s errors and %s warnings", outputParser.getNumberOfErrors(), outputParser.getNumberOfWarnings()));
            
            // Check the number of warnings
            if (unstableIfWarnings && outputParser.getNumberOfWarnings() > 0) {
                listener.getLogger().println("> Set build UNSTABLE because there are warnings.");
                build.setResult(Result.UNSTABLE);
            }
            
            // Return the result of the documentation generation
            return continueOnBuildFailure ? true : (r == 0);
        } catch (IOException e) {
            Util.displayIOException(e, listener);
            build.setResult(Result.FAILURE);
            return false;
        }
    }
    
    @Override
    public Descriptor<Builder> getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }
    
    
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @CopyOnWrite
        private volatile DoxygenInstallation[] installations = new DoxygenInstallation[0];

        public DescriptorImpl() {
            super(DoxygenBuilder.class);
            load();
        }

        public String getDisplayName() {
            return Messages.DoxygenPlugin_BuildStepName();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        public DoxygenInstallation[] getInstallations() {
            return installations;
        }

        public void setInstallations(DoxygenInstallation... antInstallations) {
            this.installations = antInstallations;
            save();
        }

        public DoxygenInstallation.DescriptorImpl getToolDescriptor() {
            return ToolInstallation.all().get(DoxygenInstallation.DescriptorImpl.class);
        }
    }
}
