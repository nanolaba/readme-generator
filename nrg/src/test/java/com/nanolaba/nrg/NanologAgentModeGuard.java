package com.nanolaba.nrg;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Puts nanolog's {@code nanolog.agent} system property back after every test, to the value the
 * test JVM started with.
 * <p>
 * {@link NRG#main} sets it to {@code off} for the whole JVM, as it should for a command-line run,
 * and many CLI tests call {@code main}. Left set, the next {@code LOG.init()} reads agent mode as
 * off, and nanolog then closes its run log and drops the run summary for the rest of the JVM, so
 * every later test would run without agent mode. The guard runs right after the test method,
 * before the {@code @AfterEach} methods that reset the logger with {@code LOG.init()}.
 */
public class NanologAgentModeGuard implements AfterTestExecutionCallback {

    static final String PROPERTY = "nanolog.agent";

    // Read when JUnit first creates the guard, before any test that uses it has run NRG.main.
    private static final String INITIAL = System.getProperty(PROPERTY);

    @Override
    public void afterTestExecution(ExtensionContext context) {
        if (INITIAL == null) {
            System.clearProperty(PROPERTY);
        } else {
            System.setProperty(PROPERTY, INITIAL);
        }
    }
}
