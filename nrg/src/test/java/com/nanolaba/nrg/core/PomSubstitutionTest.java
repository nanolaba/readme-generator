package com.nanolaba.nrg.core;

import com.nanolaba.nrg.DefaultNRGTest;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PomSubstitutionTest extends DefaultNRGTest {

    private static Generator make(String body, PomReader pom) {
        return make(body, pom, null);
    }

    private static Generator make(String body, PomReader pom, Map<String, String> env) {
        return new Generator(new File("README.src.md"), body, null,
                env == null ? null : env::get, pom);
    }

    private static PomReader fixed(String... pairs) {
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
        Generator g = make("Version: ${pom.version}\n", fixed("version", "1.2.3"));

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("Version: 1.2.3"), body);
    }

    @Test
    public void testCoordinatesViaPomNamespace() {
        Generator g = make("${pom.groupId}:${pom.artifactId}:${pom.version}\n",
                fixed("groupId", "com.nanolaba", "artifactId", "readme-generator", "version", "0.4-SNAPSHOT"));

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("com.nanolaba:readme-generator:0.4-SNAPSHOT"), body);
    }

    @Test
    public void testNestedPath() {
        Generator g = make("Repo: ${pom.scm.url}\n",
                fixed("scm.url", "https://github.com/nanolaba/readme-generator"));

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("Repo: https://github.com/nanolaba/readme-generator"), body);
    }

    @Test
    public void testPropertiesLookup() {
        Generator g = make("Java: ${pom.properties.java.version}\n",
                fixed("properties.java.version", "8"));

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("Java: 8"), body);
    }

    @Test
    public void testParentVersion() {
        Generator g = make("Parent: ${pom.parent.version}\n",
                fixed("parent.version", "0.3"));

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("Parent: 0.3"), body);
    }

    @Test
    public void testMissingValueWithoutDefaultRendersEmpty() {
        Generator g = make("X[${pom.missing}]Y\n", fixed());

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("X[]Y"), body);
    }

    @Test
    public void testMissingValueLogsErrorOncePerPath() {
        Generator g = make("${pom.a}${pom.a}${pom.b}\n", fixed());
        g.getResult("en").getContent().toString();

        String logs = getOutAndClear() + getErrAndClear();
        long aCount = Arrays.stream(logs.split("\\R")).filter(l -> l.contains("'a'")).count();
        long bCount = Arrays.stream(logs.split("\\R")).filter(l -> l.contains("'b'")).count();
        assertEquals(1L, aCount, "a should warn once: " + logs);
        assertEquals(1L, bCount, "b should warn once: " + logs);
    }

    @Test
    public void testDefaultUsedWhenMissing() {
        Generator g = make("${pom.version:0.0.0-unknown}\n", fixed());

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("0.0.0-unknown"), body);
    }

    @Test
    public void testValueBeatsDefault() {
        Generator g = make("${pom.version:fallback}\n", fixed("version", "1.0"));

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("1.0"), body);
        assertFalse(body.contains("fallback"), body);
    }

    @Test
    public void testPomUsableInPropertyDeclaration() {
        Generator g = make(
                "<!--@v=${pom.version}-->\n" +
                        "Version: ${v}\n",
                fixed("version", "9.9"));

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("Version: 9.9"), body);
    }

    @Test
    public void testPomUsableInsideWidgetParams() {
        Generator g = make(
                "${widget:badge(type='custom', label='ver', message='${pom.version}', color='blue')}\n",
                fixed("version", "2.5"));

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("ver-2.5-blue"), body);
    }

    @Test
    public void testBackslashEscapeSuppressesPomSubstitution() {
        Generator g = make("\\${pom.version} and ${pom.version}\n", fixed("version", "1.0"));

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("${pom.version} and 1.0"), body);
    }

    @Test
    public void testPomInternalPropertyInterpolation() {
        // The POM value itself contains a ${revision} reference resolvable from properties.
        PomReader pr = path -> {
            if ("version".equals(path)) {
                return java.util.Optional.of("${revision}");
            }
            if ("properties.revision".equals(path)) {
                return java.util.Optional.of("4.2");
            }
            return java.util.Optional.empty();
        };
        Generator g = make("V=${pom.version}\n", pr);

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("V=4.2"), body);
    }

    @Test
    public void testPomInternalProjectReferenceResolves() {
        PomReader pr = path -> {
            if ("version".equals(path)) {
                return java.util.Optional.of("${project.parent.version}");
            }
            if ("parent.version".equals(path)) {
                return java.util.Optional.of("3.0");
            }
            return java.util.Optional.empty();
        };
        Generator g = make("V=${pom.version}\n", pr);

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("V=3.0"), body);
    }

    @Test
    public void testPomInternalEnvReference() {
        PomReader pr = path -> "version".equals(path)
                ? java.util.Optional.of("${env.RELEASE_VERSION:0.0.0}")
                : java.util.Optional.empty();
        Map<String, String> env = new HashMap<>();
        env.put("RELEASE_VERSION", "9.9.9");
        Generator g = make("V=${pom.version}\n", pr, env);

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("V=9.9.9"), body);
    }

    @Test
    public void testPomInternalEnvFallbackUsedWhenUnset() {
        PomReader pr = path -> "version".equals(path)
                ? java.util.Optional.of("${env.RELEASE_VERSION:0.0.0}")
                : java.util.Optional.empty();
        Generator g = make("V=${pom.version}\n", pr, new HashMap<>());

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("V=0.0.0"), body);
    }

    @Test
    public void testParentInheritanceForGroupId() {
        // No top-level groupId, falls back to parent.groupId.
        PomReader pr = path -> {
            if ("parent.groupId".equals(path)) return java.util.Optional.of("com.nanolaba");
            return java.util.Optional.empty();
        };
        Generator g = make("G=${pom.groupId}\n", pr);

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("G=com.nanolaba"), body);
    }

    @Test
    public void testParentInheritanceForVersion() {
        PomReader pr = path -> {
            if ("parent.version".equals(path)) return java.util.Optional.of("0.3");
            return java.util.Optional.empty();
        };
        Generator g = make("V=${pom.version}\n", pr);

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("V=0.3"), body);
    }

    @Test
    public void testLocalValueWinsOverParentInheritance() {
        PomReader pr = path -> {
            if ("groupId".equals(path)) return java.util.Optional.of("local.id");
            if ("parent.groupId".equals(path)) return java.util.Optional.of("parent.id");
            return java.util.Optional.empty();
        };
        Generator g = make("G=${pom.groupId}\n", pr);

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("G=local.id"), body);
    }

    @Test
    public void testNoParentInheritanceForArbitraryFields() {
        // description does NOT inherit from parent (only groupId/version/name do)
        PomReader pr = path -> {
            if ("parent.description".equals(path)) return java.util.Optional.of("inherited");
            return java.util.Optional.empty();
        };
        Generator g = make("[${pom.description}]\n", pr);

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("[]"), body);
    }
}
