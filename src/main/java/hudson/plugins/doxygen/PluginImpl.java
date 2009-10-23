package hudson.plugins.doxygen;

import java.io.IOException;

import hudson.FilePath;
import hudson.Plugin;
import hudson.model.AbstractProject;
import hudson.util.FormValidation;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;

/**
 * @author Gregory BOISSINOT
 */
public class PluginImpl extends Plugin {

    public FormValidation doCheckDoxygenFile(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
        FilePath ws = project.getSomeWorkspace();
        return ws!=null ? ws.validateFileMask(value, true) : FormValidation.ok();
    }     


}
