package com.nanolaba.nrg.core;

import com.nanolaba.nrg.DefaultNRGTest;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GradleSubstitutionTest extends DefaultNRGTest {

    private static Generator make(String body, GradleReader gradle) {
        return new Generator(new File("README.src.md"), body, null,
                null, null, null, gradle);
    }

    private static GradleReader fixed(String... pairs) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put(pairs[i], pairs[i + 1]);
        }
        return path -> {
            String v = m.get(path);
            return v == null ? java.util.Optional.empty() : java.util.Optional.of(v);
        };
    }

    @Test
    public void testVersion() {
        Generator g = make("V=${gradle.version}\n", fixed("version", "1.2.0"));
        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("V=1.2.0"), body);
    }

    @Test
    public void testGroup() {
        Generator g = make("G=${gradle.group}\n", fixed("group", "com.example"));
        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("G=com.example"), body);
    }

    @Test
    public void testArbitraryKeyFromProperties() {
        Generator g = make("X=${gradle.kotlin.version}\n",
                fixed("kotlin.version", "1.9.22"));
        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("X=1.9.22"), body);
    }

    @Test
    public void testMissingValueRendersEmpty() {
        Generator g = make("X[${gradle.missing}]Y\n", fixed());
        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("X[]Y"), body);
    }

    @Test
    public void testMissingValueLogsWarnOncePerPath() {
        Generator g = make("${gradle.a}${gradle.a}${gradle.b}\n", fixed());
        g.getResult("en").getContent().toString();

        String logs = getOutAndClear() + getErrAndClear();
        long aCount = Arrays.stream(logs.split("\\R")).filter(l -> l.contains("'a'")).count();
        long bCount = Arrays.stream(logs.split("\\R")).filter(l -> l.contains("'b'")).count();
        assertEquals(1L, aCount, "a should warn once: " + logs);
        assertEquals(1L, bCount, "b should warn once: " + logs);
    }

    @Test
    public void testDefaultUsedWhenMissing() {
        Generator g = make("${gradle.version:0.0.0-unknown}\n", fixed());
        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("0.0.0-unknown"), body);
    }

    @Test
    public void testValueBeatsDefault() {
        Generator g = make("${gradle.version:fallback}\n", fixed("version", "1.0"));
        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("1.0"), body);
        assertFalse(body.contains("fallback"), body);
    }

    @Test
    public void testGradleUsableInPropertyDeclaration() {
        Generator g = make(
                "<!--@v=${gradle.version}-->\n" +
                        "V=${v}\n",
                fixed("version", "9.9"));
        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("V=9.9"), body);
    }

    @Test
    public void testBackslashEscapeSuppressesGradleSubstitution() {
        Generator g = make("\\${gradle.version} and ${gradle.version}\n", fixed("version", "1.0"));
        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("${gradle.version} and 1.0"), body);
    }
}
