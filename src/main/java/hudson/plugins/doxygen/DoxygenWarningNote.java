package hudson.plugins.doxygen;

import hudson.Extension;
import hudson.MarkupText;
import hudson.console.ConsoleAnnotationDescriptor;
import hudson.console.ConsoleAnnotator;
import hudson.console.ConsoleNote;
import java.util.regex.Pattern;

/**
 * Annotation for Doxygen warning messages
 */
public class DoxygenWarningNote  extends ConsoleNote {
    /** Pattern to identify doxygen warning message */
    public static Pattern PATTERN = Pattern.compile("^.*[Ww]arning:\\s.*");
    
    public DoxygenWarningNote() {
    }

    @Override
    public ConsoleAnnotator annotate(Object context, MarkupText text, int charPos) {
        text.addMarkup(0, text.length(), "<span class=warning-inline>", "</span>");
        return null;
    }

    @Extension
    public static final class DescriptorImpl extends ConsoleAnnotationDescriptor {

        public String getDisplayName() {
            return Messages.DoxygenPlugin_WarningNotesDescription();
        }
    }
}
