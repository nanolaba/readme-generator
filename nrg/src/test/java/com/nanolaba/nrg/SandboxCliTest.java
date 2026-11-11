package com.nanolaba.nrg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code --no-env}, {@code --env-allow}, {@code --offline} and {@code --sandbox}: switches the
 * caller sets for templates it does not trust, which no template property can lift.
 */
public class SandboxCliTest extends DefaultNRGTest {

    @TempDir
    Path tempDir;

    /** Any variable that is set in the test JVM, so that "not visible" is distinguishable from "unset". */
    private static String someSetVariable() {
        for (Map.Entry<String, String> e : System.getenv().entrySet()) {
            if (e.getKey().matches("[A-Za-z_][A-Za-z0-9_]*") && !e.getValue().isEmpty() && !e.getValue().contains("}")) {
                return e.getKey();
            }
        }
        throw new IllegalStateException("no environment variable to test with");
    }

    private String render(String body, String... flags) throws IOException {
        Path src = tempDir.resolve("README.src.md");
        Files.write(src, body.getBytes(StandardCharsets.UTF_8));
        String[] args = new String[flags.length + 4];
        System.arraycopy(flags, 0, args, 0, flags.length);
        args[flags.length] = "--stdout";
        args[flags.length + 1] = "--no-header";
        args[flags.length + 2] = "-f";
        args[flags.length + 3] = src.toString();
        NRG.run(args);
        return getOutAndClear();
    }

    @Test
    public void testNoEnvHidesEveryVariableAndKeepsFallbacks() throws IOException {
        String name = someSetVariable();
        String body = "<!--@copied=${env." + name + "}-->\n[${env." + name + "}] [${copied}] [${env." + name + ":fallback}]\n";

        assertFalse(render(body).contains("[] [] [fallback]"), "visible without restrictions");
        String restricted = render(body, "--no-env", "--log-level", "warn");
        assertTrue(restricted.contains("[] [] [fallback]"), "hidden in lines and in property values: " + restricted);
        assertTrue(restricted.contains("not available under --no-env"), "the warning says why: " + restricted);
    }

    @Test
    public void testEnvAllowListsNamesAndPrefixes() throws IOException {
        String name = someSetVariable();
        String value = System.getenv(name);
        String body = "[${env." + name + "}]\n";

        assertTrue(render(body, "--env-allow", name).contains("[" + value + "]"));
        assertTrue(render(body, "--env-allow", name.substring(0, 1) + "*").contains("[" + value + "]"));
        assertTrue(render(body, "--env-allow", "SOMETHING_ELSE").contains("[]"));
    }

    @Test
    public void testRestrictedEnvMatching() {
        Function<String, String> env = NRG.restrictedEnv(" CI , GITHUB_* ");
        String name = someSetVariable();
        assertNull(NRG.restrictedEnv(null).apply(name));
        assertNull(env.apply("CIX"));
        assertEquals(System.getenv("CI"), env.apply("CI"));
    }

    @Test
    public void testSandboxRefusesRemoteImportsEvenWhenTheTemplateAllowsThem() throws IOException {
        Path src = tempDir.resolve("README.src.md");
        Files.write(src, ("<!--@nrg.allowRemoteImports=true-->\n"
                + "${widget:import(url='https://example.com/x.md', run-generator='false')}\n").getBytes(StandardCharsets.UTF_8));

        assertEquals(NRG.EXIT_TEMPLATE_ERROR, NRG.run("--sandbox", "--format", "json", "-f", src.toString()));
        assertTrue(getOutAndClear().contains("disabled by --offline / --sandbox"));
    }

    @Test
    public void testSandboxRejectsAllowExec() {
        assertEquals(NRG.EXIT_USAGE, NRG.run("--sandbox", "--allow-exec", "-f", "README.src.md"));
        assertTrue(getErrAndClear().contains("--sandbox cannot be combined with --allow-exec"));
    }
}
