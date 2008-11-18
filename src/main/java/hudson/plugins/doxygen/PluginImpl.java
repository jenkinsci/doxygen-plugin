package hudson.plugins.doxygen;

import java.io.IOException;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.Plugin;
import hudson.tasks.Publisher;
import hudson.util.FormFieldValidator;

/**
 * @author Gregory BOISSINOT
 */
public class PluginImpl extends Plugin {


    /**
     * Registers Doxygen publisher.
     */
    @Override
    public void start() throws Exception {
        Publisher.PUBLISHERS.add(DoxygenArchiver.DESCRIPTOR);        
        super.start();
    }
    
    public void doCheckDoxygenFile(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        new FormFieldValidator.WorkspaceFilePath(req,rsp, true, true).process();
    }     


}
