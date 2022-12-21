package hudson.plugins.doxygen;

import hudson.Extension;
import hudson.MarkupText;
import hudson.console.ConsoleAnnotationDescriptor;
import hudson.console.ConsoleAnnotator;
import hudson.console.ConsoleNote;
import java.util.regex.Pattern;

/**
 * Annotation for Doxygen error messages
 */
public class DoxygenErrorNote  extends ConsoleNote {
    /** Pattern to identify doxygen error message */
    public static final Pattern PATTERN = Pattern.compile("^[Ee]rror[:\\s].*");
    
    public DoxygenErrorNote() {
    }

    @Override
    public ConsoleAnnotator annotate(Object context, MarkupText text, int charPos) {
        text.addMarkup(0, text.length(), "<span class=error-inline>", "</span>");
        return null;
    }

    @Extension
    public static final class DescriptorImpl extends ConsoleAnnotationDescriptor {

        public String getDisplayName() {
            return Messages.DoxygenPlugin_ErrorNotesDescription();
        }
    }
}
