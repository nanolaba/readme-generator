package com.nanolaba.nrg;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public abstract class ConsoleOutputSupportTest {

    protected final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    protected final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    protected final PrintStream originalOut = System.out;
    protected final PrintStream originalErr = System.err;

    @BeforeEach
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
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
