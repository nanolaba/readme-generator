package com.nanolaba.nrg;

import org.apache.commons.io.output.TeeOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public abstract class DefaultNRGTest {

    public static final String RN = System.lineSeparator();

    protected final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    protected final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    protected final PrintStream originalOut = System.out;
    protected final PrintStream originalErr = System.err;

    @BeforeEach
    public void setUpStreams() {
        System.setOut(new PrintStream(new TeeOutputStream(originalOut, outContent)));
        System.setErr(new PrintStream(new TeeOutputStream(originalErr, errContent)));
    }

    @AfterEach
    public void restoreStreams() {
        outContent.reset();
        errContent.reset();
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    protected String getOutAndClear() {
        String res = outContent.toString();
        outContent.reset();
        return res;
    }

    protected String getErrAndClear() {
        String res = errContent.toString();
        errContent.reset();
        return res;
    }
}
