package hudson.plugins.doxygen;

import hudson.console.LineTransformationOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.regex.Matcher;

/**
 * Parser for the Doxygen output
 */
public class DoxygenConsoleParser extends LineTransformationOutputStream {
    private final OutputStream out;
    private final Charset charset;
    
    private int numberOfWarnings = 0;
    private int numberOfErrors = 0;
    
    public DoxygenConsoleParser(OutputStream out, Charset charset) {
        this.out = out;
        this.charset = charset;
    }
    
    public int getNumberOfWarnings() {
        return numberOfWarnings;
    }
    
    public int getNumberOfErrors() {
        return numberOfErrors;
    }
    
    @Override
    protected void eol(byte[] b, int len) throws IOException {
        String line = charset.decode(ByteBuffer.wrap(b, 0, len)).toString();
        
        // trim off CR/LF from the end
        line = trimEOL(line);
        
        // Error messages handler
        Matcher m = DoxygenErrorNote.PATTERN.matcher(line);
        if (m.matches()) { // Match the number of warnings
            new DoxygenErrorNote().encodeTo(out);
            this.numberOfErrors++;
        }
        
        // Warning messages handler
        m = DoxygenWarningNote.PATTERN.matcher(line);
        if (m.matches()) { // Match the number of warnings
            new DoxygenWarningNote().encodeTo(out);
            this.numberOfWarnings++;
        }
        
        out.write(b, 0, len);
    }
    
    @Override
    public void close() throws IOException {
        super.close();
        out.close();
    }
}
