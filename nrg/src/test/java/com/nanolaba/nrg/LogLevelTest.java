package com.nanolaba.nrg;

import com.nanolaba.logging.LOG;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LogLevelTest extends DefaultNRGTest {

    @AfterEach
    public void resetLogger() {
        LOG.init();
    }

    @Test
    public void testWarnLevelSuppressesInfo() {
        NRG.applyLogLevel(NRG.LogLevel.WARN);

        LOG.info("visible info marker 123");
        LOG.warn("visible warn marker 456");
        LOG.error("visible error marker 789");

        String out = getOutAndClear();
        String err = getErrAndClear();
        String combined = out + err;

        assertFalse(combined.contains("visible info marker 123"), "INFO should be suppressed at WARN level");
        assertTrue(combined.contains("visible warn marker 456"), "WARN must remain visible");
        assertTrue(combined.contains("visible error marker 789"), "ERROR must remain visible");
    }

    @Test
    public void testDebugLevelShowsDebugAndBelow() {
        NRG.applyLogLevel(NRG.LogLevel.DEBUG);

        LOG.trace("trace marker should be hidden");
        LOG.debug("debug marker shown");
        LOG.info("info marker shown");

        String combined = getOutAndClear() + getErrAndClear();
        assertFalse(combined.contains("trace marker should be hidden"));
        assertTrue(combined.contains("debug marker shown"));
        assertTrue(combined.contains("info marker shown"));
    }

    @Test
    public void testErrorLevelShowsOnlyErrors() {
        NRG.applyLogLevel(NRG.LogLevel.ERROR);

        LOG.debug("debug skipped");
        LOG.info("info skipped");
        LOG.warn("warn skipped");
        LOG.error("error shown abc");

        String combined = getOutAndClear() + getErrAndClear();
        assertFalse(combined.contains("debug skipped"));
        assertFalse(combined.contains("info skipped"));
        assertFalse(combined.contains("warn skipped"));
        assertTrue(combined.contains("error shown abc"));
    }

    @Test
    public void testInvalidLogLevelValueReportsError() {
        NRG.main("--log-level", "verbose");

        String err = getErrAndClear();
        assertTrue(err.contains("Invalid log level"), "Expected 'Invalid log level' in stderr but got: " + err);
        assertTrue(err.contains("verbose"));
    }

    @Test
    public void testLogLevelCliOptionApplies() {
        NRG.main("--log-level", "warn", "--version");
        getOutAndClear();
        getErrAndClear();

        LOG.info("post-cli info should NOT appear");
        LOG.warn("post-cli warn should appear");
        String combined = getOutAndClear() + getErrAndClear();

        assertFalse(combined.contains("post-cli info should NOT appear"));
        assertTrue(combined.contains("post-cli warn should appear"));
    }

    @Test
    public void testParseAcceptsMixedCase() {
        assertEquals(NRG.LogLevel.WARN, NRG.LogLevel.parse("Warn"));
        assertEquals(NRG.LogLevel.DEBUG, NRG.LogLevel.parse(" DEBUG "));
        assertEquals(NRG.LogLevel.TRACE, NRG.LogLevel.parse("trace"));
    }

    @Test
    public void testParseRejectsUnknown() {
        assertNull(NRG.LogLevel.parse("verbose"));
        assertNull(NRG.LogLevel.parse(""));
        assertNull(NRG.LogLevel.parse(null));
    }
}
