package com.nanolaba.nrg;

import com.nanolaba.logging.LOG;
import org.apache.commons.io.output.TeeOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@ExtendWith(NanologAgentModeGuard.class)
public abstract class DefaultNRGTest {

    public static final String RN = System.lineSeparator();

    protected final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    protected final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    protected final PrintStream originalOut = System.out;
    protected final PrintStream originalErr = System.err;

    /**
     * Captures both streams in UTF-8, the encoding of what NRG prints on stdout, so that the log
     * lines written to the same streams decode the same way whatever the platform charset is.
     */
    @BeforeEach
    public void setUpStreams() throws UnsupportedEncodingException {
        LOG.init();
        NRG.exitOnFailure = false;
        System.setOut(new PrintStream(new TeeOutputStream(originalOut, outContent), true, "UTF-8"));
        System.setErr(new PrintStream(new TeeOutputStream(originalErr, errContent), true, "UTF-8"));
    }

    @AfterEach
    public void restoreStreams() {
        outContent.reset();
        errContent.reset();
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    protected String getOutAndClear() {
        String res = new String(outContent.toByteArray(), StandardCharsets.UTF_8);
        outContent.reset();
        return res;
    }

    protected String getErrAndClear() {
        String res = new String(errContent.toByteArray(), StandardCharsets.UTF_8);
        errContent.reset();
        return res;
    }
}
