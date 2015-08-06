package hudson.plugins.doxygen;

import hudson.FilePath;
import hudson.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public abstract class AbstractWorkspaceTest {

    protected File parentFile;
    protected FilePath workspace;

    public void createWorkspace() throws Exception {
        parentFile = Util.createTempDir();
        workspace = new FilePath(parentFile);
        if (workspace.exists()) {
            workspace.deleteRecursive();
        }
        workspace.mkdirs();
    }

    public void deleteWorkspace() throws Exception {
        workspace.deleteRecursive();
    }
    
    // Helper
    protected String readAsString(String resourceName) throws IOException {
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

    
    
}
