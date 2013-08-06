package hudson.plugins.doxygen;

import hudson.AbortException;
import hudson.FilePath;
import hudson.plugins.doxygen.DoxygenArchiver.DoxygenArchiverDescriptor;
import hudson.remoting.VirtualChannel;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DoxygenDirectoryParser implements FilePath.FileCallable<FilePath>, Serializable {


    private static final long serialVersionUID = 1L;

    private static final Pattern DRIVE_PATTERN = Pattern.compile("[A-Za-z]:\\\\.+");

    private static final Logger LOGGER = Logger.getLogger(DoxygenDirectoryParser.class.getName());

    private transient Map<String, String> doxyfileInfos = new HashMap<String, String>();

    private static final String DOXYGEN_KEY_OUTPUT_DIRECTORY = "OUTPUT_DIRECTORY";
    private static final String DOXYGEN_KEY_GENERATE_HTML = "GENERATE_HTML";
    private static final String DOXYGEN_KEY_HTML_OUTPUT = "HTML_OUTPUT";
    private static final String DOXYGEN_DEFAULT_HTML_OUTPUT = "html";

    private static final String DOXYGEN_VALUE_YES = "YES";

    private String publishType;
    private String doxygenHtmlDirectory;
    private String doxyfilePath;
    private String folderWhereYouRunDoxygen;

    public DoxygenDirectoryParser(String publishType, String doxyfilePath, String doxygenHtmlDirectory) {
        this.publishType = publishType;
        this.doxyfilePath = doxyfilePath;
        this.doxygenHtmlDirectory = doxygenHtmlDirectory;
        this.folderWhereYouRunDoxygen = null;
    }

    public DoxygenDirectoryParser(String publishType, String doxyfilePath, String doxygenHtmlDirectory, String folderWhereYouRunDoxygen) {
        this.publishType = publishType;
        this.doxyfilePath = doxyfilePath;
        this.doxygenHtmlDirectory = doxygenHtmlDirectory;
        this.folderWhereYouRunDoxygen = folderWhereYouRunDoxygen;
    }

    public FilePath invoke(java.io.File workspace, VirtualChannel channel) throws IOException {
        try {
            return (DoxygenArchiverDescriptor.DOXYGEN_HTMLDIRECTORY_PUBLISHTYPE).equals(publishType)
                    ? retrieveDoxygenDirectoryFromHudsonConfiguration(doxygenHtmlDirectory, new FilePath(workspace))
                    : retrieveDoxygenDirectoryFromDoxyfile(doxyfilePath, new FilePath(workspace));
        } catch (InterruptedException ie) {
            throw new AbortException(ie.getMessage());
        }
    }


    /**
     * Determine if Doxygen generate HTML reports
     */
    private boolean isDoxygenGenerateHtml() {
        if (doxyfileInfos == null)
            return false;

        String generatedHtmlKeyVal = doxyfileInfos.get(DOXYGEN_KEY_GENERATE_HTML);

        // If the 'GENERATE_HTML Key is not present, by default the HTML generated documentation is actived.
        if (generatedHtmlKeyVal == null) {
            return true;
        }

        return DOXYGEN_VALUE_YES.equalsIgnoreCase(generatedHtmlKeyVal);
    }

    /**
     * Retrieve the generated doxygen HTML directory from Hudson configuration given by the user
     */
    private FilePath retrieveDoxygenDirectoryFromHudsonConfiguration(String doxygenHtmlDirectory, FilePath base) throws InterruptedException, IOException {

        FilePath doxygenGeneratedDir = null;

        LOGGER.log(Level.INFO, "Using the Doxygen HTML directory specified by the configuration.");

        if (doxygenHtmlDirectory == null) {
            throw new IllegalArgumentException("Error on the given doxygen html directory.");
        }

        if (doxygenHtmlDirectory.trim().length() == 0) {
            throw new IllegalArgumentException("Error on the given doxygen html directory.");
        }

        doxygenGeneratedDir = new FilePath(base, doxygenHtmlDirectory);
        if (!doxygenGeneratedDir.exists()) {
            throw new AbortException("The directory '" + doxygenGeneratedDir + "' doesn't exist.");
        }
        return doxygenGeneratedDir;
    }

    /**
     * Gets the directory where the Doxygen is generated for the given build.
     */
    private FilePath getDoxygenGeneratedDir(FilePath base) throws IOException, InterruptedException {

        if (doxyfileInfos == null)
            return null;

        FilePath result = base;
        if ((this.folderWhereYouRunDoxygen != null) && (!this.folderWhereYouRunDoxygen.trim().isEmpty())) {
            result = result.child(this.folderWhereYouRunDoxygen);
        }
        final String outputDirectory = doxyfileInfos.get(DOXYGEN_KEY_OUTPUT_DIRECTORY);
        if ((outputDirectory != null) && (!outputDirectory.trim().isEmpty())) {
            result = result.child(outputDirectory);
        }

        //Concat html directory
        String outputHTML = doxyfileInfos.get(DOXYGEN_KEY_HTML_OUTPUT);
        if (outputHTML == null || outputHTML.trim().isEmpty()) {
            outputHTML = DOXYGEN_DEFAULT_HTML_OUTPUT;
            LOGGER.log(Level.INFO, "The " + DOXYGEN_KEY_HTML_OUTPUT + " tag is not present or is left blank." + DOXYGEN_DEFAULT_HTML_OUTPUT + " will be used as the default path.");
        }
        result = result.child(outputHTML);

        LOGGER.info("Created filepath with the following path:"+result.getRemote());
        if (!result.exists()) {
            LOGGER.info("Computed doxygen generated dir does not exist. Returning null");
            return null;
        }

        return result;
    }

    /**
     * Load the Doxyfile Doxygen file in memory
     */
    private void loadDoxyFile(FilePath doxyfilePath)
            throws IOException, InterruptedException {

        LOGGER.log(Level.INFO, "The Doxyfile path is '" + doxyfilePath.toURI() + "'.");

        final String separator = "=";
        InputStream ips = new FileInputStream(new File(doxyfilePath.toURI()));
        InputStreamReader ipsr = new InputStreamReader(ips);
        BufferedReader br = new BufferedReader(ipsr);
        String line = null;

        List<String> doxyfileDirectories = new ArrayList<String>();
        if (doxyfileInfos == null) {
            doxyfileInfos = new HashMap<String, String>();
        }
        while ((line = br.readLine()) != null) {

            if (doxyfileLineIsAComment(line)) {
                // Prevent that a comment containing a separator get's interpreted somehow
                continue;
            }

            String[] elements = line.split(separator);
            if (elements.length == 1) {
                // Either there is no separator in the line or there is nothing behind the separator (i.e. there's no value to the key)
                continue;
            }
            // split space-separated list, preserving quoted strings
            List<String> rhs = new ArrayList<String>();
            Matcher m = Pattern.compile("((?:[^\"]\\S*)|\"(?:.*?[^\\\\])\")\\s*").matcher(elements[1].trim());
            while (m.find())
            {
                // Replace delimiting quotes and escaped quotes
                String s = m.group(1).replace("\\\"", "\"").replaceAll("^\"(.*)\"$", "$1");
                // Discard continuation character
                if (!s.equals("\\"))
                    rhs.add(s);
            }

            if (elements[0].startsWith("@INCLUDE_PATH")) {
                Collections.addAll(doxyfileDirectories, rhs.toArray(new String[0]));
            }
            else {
                if (rhs.isEmpty()) {
                    rhs.add("");
                }
                if (elements[0].startsWith("@INCLUDE")) {
                    processIncludeFile(doxyfileDirectories, doxyfilePath.getParent(), rhs.get(0));
                }
                else {
                    doxyfileInfos.put(elements[0].trim(), rhs.get(0));
                }
            }

        }
        br.close();
        ipsr.close();
        ips.close();
    }


    private boolean doxyfileLineIsAComment(String line) {
        return line.trim().startsWith("#");
    }

    private static boolean isAbsolute(String rel) {
        return rel.startsWith("/") || DRIVE_PATTERN.matcher(rel).matches();
    }


    private void processIncludeFileWithNoIncludedDirectories(FilePath parentFile, String includedFile)
            throws IOException, InterruptedException {

        FilePath includedFilePath = isAbsolute(includedFile) ? new FilePath(new File(includedFile)) : new FilePath(parentFile, includedFile);
        if (!includedFilePath.exists()) {
            throw new AbortException("Doxyfile is incorrect. Included file '" + includedFile + "' doesn't exist.");
        }

        //Call again loadDoxyFile with the the included doxygen file path
        loadDoxyFile(includedFilePath);
    }

    private void processIncludeFileWithIncludedDirectories(List<String> doxyfileDirectories, FilePath parentFile, String includedFile)
            throws IOException, InterruptedException {

        FilePath includedFilePath = null;
        boolean findIncludedFileInDirectories = false;

        //Determine if the included doxygen file is absolute
        if (isAbsolute(includedFile)) {
            //Call again loadDoxyFile with the the included doxygen file path
            loadDoxyFile(new FilePath(new File(includedFile)));
            return;
        }

        // else, test if the included doxygen file is at the parent root
        if ((includedFilePath = new FilePath(parentFile, includedFile)).exists()) {
            //Call again loadDoxyFile with the the included doxygen file path
            loadDoxyFile(includedFilePath);
            return;
        }


        // else, iterate on each included directory for retrieve the included doxygen file
        for (String doxyfileDirectory : doxyfileDirectories) {

            //Retrieve the filepath for the included directory (it can be absolute)
            FilePath directoryFilePath = isAbsolute(doxyfileDirectory) ? new FilePath(new File(doxyfileDirectory)) : new FilePath(parentFile, doxyfileDirectory);

            //If the current included directory doesn't exist, continue, no errors
            if (!directoryFilePath.exists()) {
                continue;
            }

            //Retrieve the filepath for the included doxygen file
            includedFilePath = new FilePath(directoryFilePath, includedFile);

            //At this point, if the computed included file doesn't exist, perhaps, it's included in the next directory in directories list
            if (!includedFilePath.exists()) {
                continue;
            }

            //Call again loadDoxyFile with the the included doxygen file path
            loadDoxyFile(includedFilePath);
            findIncludedFileInDirectories = true;
            break;
        }

        //At this point, the included doxygen file path is not determined
        //Never happen, check by the doxygen tool
        if (!findIncludedFileInDirectories) {
            throw new AbortException("Doxyfile is incorrect. Included file '" + includedFile + "' doesn't exist.");
        }
    }


    private void processIncludeFile(List<String> doxyfileDirectories, FilePath parentFile, String includedFile)
            throws IOException, InterruptedException {

        //We haven't any @INCLUDE_PATH
        if (doxyfileDirectories == null || doxyfileDirectories.isEmpty()) {
            processIncludeFileWithNoIncludedDirectories(parentFile, includedFile);
        }

        //We have some  @INCLUDE_PATH
        else {
            processIncludeFileWithIncludedDirectories(doxyfileDirectories, parentFile, includedFile);
        }
    }

    /**
     * Retrieve the generated doxygen HTML directory from Doxyfile
     */
    private FilePath retrieveDoxygenDirectoryFromDoxyfile(String doxyfilePath, FilePath base)
            throws IOException, InterruptedException {

        FilePath doxygenGeneratedDir;

        LOGGER.log(Level.INFO, "Using the Doxyfile information.");

        //Load the Doxyfile
        loadDoxyFile(base.child(doxyfilePath));

        //Process if the generate htnl tag is set to 'YES'
        if (isDoxygenGenerateHtml()) {

            //Retrieve the generated doxygen directory from the build
            doxygenGeneratedDir = getDoxygenGeneratedDir(base);

            if (doxygenGeneratedDir == null || !doxygenGeneratedDir.exists()) {
                throw new AbortException("The output directory doesn't exist.");
            }
        } else {
            //The GENERATE_HTML tag is not set to 'YES'
            throw new AbortException("The tag " + DOXYGEN_KEY_GENERATE_HTML + " is not set to '" + DOXYGEN_VALUE_YES + "'. The Doxygen plugin publishes only HTML documentation.");
        }

        return doxygenGeneratedDir;
    }

}
