package com.nanolaba.nrg.core;

import com.nanolaba.nrg.DefaultNRGTest;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NpmSubstitutionTest extends DefaultNRGTest {

    private static Generator make(String body, NpmReader npm) {
        return new Generator(new File("README.src.md"), body, null,
                null, null, npm, null);
    }

    private static NpmReader fixed(String... pairs) {
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
    public void testTopLevelVersion() {
        Generator g = make("Version: ${npm.version}\n", fixed("version", "1.2.3"));
        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("Version: 1.2.3"), body);
    }

    @Test
    public void testTopLevelName() {
        Generator g = make("Name: ${npm.name}\n", fixed("name", "@scope/pkg"));
        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("Name: @scope/pkg"), body);
    }

    @Test
    public void testNestedDependencyPath() {
        Generator g = make("Lodash: ${npm.dependencies.lodash}\n",
                fixed("dependencies.lodash", "^4.17.21"));
        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("Lodash: ^4.17.21"), body);
    }

    @Test
    public void testMissingValueWithoutDefaultRendersEmpty() {
        Generator g = make("X[${npm.missing}]Y\n", fixed());
        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("X[]Y"), body);
    }

    @Test
    public void testMissingValueLogsWarnOncePerPath() {
        Generator g = make("${npm.a}${npm.a}${npm.b}\n", fixed());
        g.getResult("en").getContent().toString();

        String logs = getOutAndClear() + getErrAndClear();
        long aCount = Arrays.stream(logs.split("\\R")).filter(l -> l.contains("'a'")).count();
        long bCount = Arrays.stream(logs.split("\\R")).filter(l -> l.contains("'b'")).count();
        assertEquals(1L, aCount, "a should warn once: " + logs);
        assertEquals(1L, bCount, "b should warn once: " + logs);
    }

    @Test
    public void testDefaultUsedWhenMissing() {
        Generator g = make("${npm.version:0.0.0-unknown}\n", fixed());
        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("0.0.0-unknown"), body);
    }

    @Test
    public void testValueBeatsDefault() {
        Generator g = make("${npm.version:fallback}\n", fixed("version", "1.0"));
        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("1.0"), body);
        assertFalse(body.contains("fallback"), body);
    }

    @Test
    public void testNpmUsableInPropertyDeclaration() {
        Generator g = make(
                "<!--@v=${npm.version}-->\n" +
                        "Version: ${v}\n",
                fixed("version", "9.9"));
        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("Version: 9.9"), body);
    }

    @Test
    public void testNpmUsableInsideWidgetParams() {
        Generator g = make(
                "${widget:badge(type='custom', label='ver', message='${npm.version}', color='blue')}\n",
                fixed("version", "2.5"));
        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("ver-2.5-blue"), body);
    }

    @Test
    public void testBackslashEscapeSuppressesNpmSubstitution() {
        Generator g = make("\\${npm.version} and ${npm.version}\n", fixed("version", "1.0"));
        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("${npm.version} and 1.0"), body);
    }
}
