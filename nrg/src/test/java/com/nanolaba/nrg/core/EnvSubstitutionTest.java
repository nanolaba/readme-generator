package com.nanolaba.nrg.core;

import com.nanolaba.logging.MemoryLogger;
import com.nanolaba.nrg.DefaultNRGTest;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.nanolaba.logging.LogEntry.LogEntryLevel.WARN;
import static org.junit.jupiter.api.Assertions.*;

class EnvSubstitutionTest extends DefaultNRGTest {

    private static Generator make(String body, Map<String, String> env) {
        return new Generator(new File("README.src.md"), body, null, env::get);
    }

    @Test
    public void testEnvVarSubstitutedInBody() {
        Map<String, String> env = new HashMap<>();
        env.put("BUILD_NUMBER", "42");
        Generator g = make("Build: ${env.BUILD_NUMBER}\n", env);

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("Build: 42"), body);
    }

    @Test
    public void testMissingEnvWithoutDefaultRendersEmpty() {
        Generator g = make("Build: [${env.MISSING}]\n", new HashMap<>());

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("Build: []"), body);
    }

    @Test
    public void testMissingEnvLogsWarning() {
        Generator g = make("X${env.MISSING}Y\n", new HashMap<>());
        g.getResult("en").getContent().toString();

        String logs = getOutAndClear() + getErrAndClear();
        assertTrue(logs.contains("MISSING"), logs);
    }

    @Test
    public void testMissingEnvWarningSaysWhetherTheCallerRestrictedTheEnvironment() {
        Generator plain = make("[${env.PLAIN}]\n", new HashMap<>());
        assertFalse(plain.getConfig().isEnvRestricted());
        try (MemoryLogger log = MemoryLogger.capture()) {
            plain.getResult("en");
            log.assertLogged(WARN, "Environment variable 'PLAIN' is not set; rendering empty");
        }

        Generator flagged = make("[${env.FLAGGED}]\n", new HashMap<>());
        flagged.getConfig().setEnvRestricted(true);
        assertTrue(flagged.getConfig().isEnvRestricted());
        try (MemoryLogger log = MemoryLogger.capture()) {
            flagged.getResult("en");
            log.assertLogged(WARN, "Environment variable 'FLAGGED' is not available under --no-env / --env-allow");
        }

        GeneratorConfig.RestrictedEnvProvider hiding = name -> null;
        Generator hidden = new Generator(new File("README.src.md"), "[${env.HIDDEN}]\n", null, hiding);
        assertTrue(hidden.getConfig().isEnvRestricted(), "a restricting provider is recognised by its type");
        try (MemoryLogger log = MemoryLogger.capture()) {
            hidden.getResult("en");
            log.assertLogged(WARN, "Environment variable 'HIDDEN' is not available under --no-env / --env-allow");
        }
    }

    @Test
    public void testMissingEnvWarnsOncePerName() {
        Generator g = make("${env.M1}${env.M1}${env.M2}${env.M1}", new HashMap<>());
        g.getResult("en").getContent().toString();

        String logs = getOutAndClear() + getErrAndClear();
        long m1Count = Arrays.stream(logs.split("\\R")).filter(l -> l.contains("'M1'")).count();
        long m2Count = Arrays.stream(logs.split("\\R")).filter(l -> l.contains("'M2'")).count();
        assertEquals(1L, m1Count, "M1 should warn once: " + logs);
        assertEquals(1L, m2Count, "M2 should warn once: " + logs);
    }

    @Test
    public void testDefaultUsedWhenEnvMissing() {
        Generator g = make("URL: ${env.RELEASE_URL:https://example.com}\n", new HashMap<>());

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("URL: https://example.com"), body);
    }

    @Test
    public void testEnvBeatsDefaultWhenSet() {
        Map<String, String> env = new HashMap<>();
        env.put("RELEASE_URL", "https://real.example.com");
        Generator g = make("URL: ${env.RELEASE_URL:https://default.example.com}\n", env);

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("URL: https://real.example.com"), body);
        assertFalse(body.contains("default.example.com"), body);
    }

    @Test
    public void testEmptyEnvValueTreatedAsSet() {
        Map<String, String> env = new HashMap<>();
        env.put("EMPTY", "");
        Generator g = make("[${env.EMPTY:fallback}]\n", env);

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("[]"), body);
        assertFalse(body.contains("fallback"), body);
    }

    @Test
    public void testEnvUsableInPropertyDeclaration() {
        Map<String, String> env = new HashMap<>();
        env.put("BUILD", "99");
        Generator g = make(
                "<!--@buildNumber=${env.BUILD}-->\n" +
                        "Build: ${buildNumber}\n", env);

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("Build: 99"), body);
    }

    @Test
    public void testEnvUsableInsideWidgetParams() {
        Map<String, String> env = new HashMap<>();
        env.put("DATE_FMT", "yyyy");
        Generator g = make("Year: ${widget:date(pattern='${env.DATE_FMT}')}\n", env);

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.matches("(?s).*Year: \\d{4}.*"), body);
    }

    @Test
    public void testBackslashEscapeSuppressesEnvSubstitution() {
        Map<String, String> env = new HashMap<>();
        env.put("X", "VALUE");
        Generator g = make("\\${env.X} and ${env.X}\n", env);

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("${env.X} and VALUE"), body);
    }

    @Test
    public void testNonPosixIdentifierFallsThroughToPropertyResolver() {
        Generator g = make(
                "<!--@app.version=1.0-->\n" +
                        "Version: ${app.version}\n", new HashMap<>());

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("Version: 1.0"), body);
    }

    @Test
    public void testDefaultMaySpanArbitraryCharsExceptCloseBrace() {
        Generator g = make("URL: ${env.X:https://a.b/c?q=1&z=2}\n", new HashMap<>());

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("URL: https://a.b/c?q=1&z=2"), body);
    }

    @Test
    public void testDefaultEnvProviderReadsRealEnv() {
        Generator g = new Generator(new File("README.src.md"), "${env.PATH:UNSET}\n");

        String body = g.getResult("en").getContent().toString();
        assertFalse(body.contains("UNSET"), "PATH must be set on any test machine: " + body);
    }
}
