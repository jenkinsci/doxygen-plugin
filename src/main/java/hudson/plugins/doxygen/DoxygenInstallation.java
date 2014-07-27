package hudson.plugins.doxygen;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentSpecific;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import java.io.IOException;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Represents an installation of doxygen
 * These installations can be managed through Jenkins administration
 */
public final class DoxygenInstallation extends ToolInstallation implements NodeSpecific<DoxygenInstallation>, EnvironmentSpecific<DoxygenInstallation>  {
    
    @DataBoundConstructor
    public DoxygenInstallation(String name, String home) {
        super(name, home, null);
    }
    
    public DoxygenInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new DoxygenInstallation(getName(), translateFor(node, log));
    }
    
    public DoxygenInstallation forEnvironment(EnvVars environment) {
        return new DoxygenInstallation(getName(), environment.expand(getHome()));
    } 
    
    
    @Extension
    public static class DescriptorImpl extends ToolDescriptor<DoxygenInstallation> {

        public String getDisplayName() {
            return Messages.DoxygenPlugin_InstallationName();
        }

        @Override
        public DoxygenInstallation[] getInstallations() {
            return Hudson.getInstance().getDescriptorByType(DoxygenBuilder.DescriptorImpl.class).getInstallations();
        }

        @Override
        public void setInstallations(DoxygenInstallation... installations) {
            Hudson.getInstance().getDescriptorByType(DoxygenBuilder.DescriptorImpl.class).setInstallations(installations);
        }

    }
}
