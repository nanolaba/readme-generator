package com.nanolaba.nrg;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code --stale-translations}: the CLI over a real git repository, plus the handling of
 * fragments added since the revision and of an unreadable revision.
 */
public class StaleTranslationsCliTest extends DefaultNRGTest {

    @TempDir
    Path tempDir;

    private Path write(String name, String body) throws IOException {
        Path p = tempDir.resolve(name);
        Files.createDirectories(p.getParent());
        Files.write(p, body.getBytes(StandardCharsets.UTF_8));
        return p;
    }

    private boolean git(String... args) throws IOException, InterruptedException {
        String[] command = new String[args.length + 5];
        command[0] = "git";
        command[1] = "-c";
        command[2] = "user.name=test";
        command[3] = "-c";
        command[4] = "user.email=test@example.com";
        System.arraycopy(args, 0, command, 5, args.length);
        try {
            Process p = new ProcessBuilder(command).directory(tempDir.toFile()).redirectErrorStream(true).start();
            p.getInputStream().close();
            return p.waitFor() == 0;
        } catch (IOException e) {
            return false;
        }
    }

    @Test
    public void testOneSidedEditAgainstAGitRevision() throws Exception {
        Assumptions.assumeTrue(git("init", "-q", "."), "git is not available");
        Path src = write("README.src.md", "<!--@nrg.languages=en,ru-->\nFast<!--en-->\nБыстро<!--ru-->\n");
        assertTrue(git("add", "."));
        assertTrue(git("commit", "-q", "-m", "init"));

        write("README.src.md", "<!--@nrg.languages=en,ru-->\nFast and safe<!--en-->\nБыстро<!--ru-->\n");
        assertEquals(NRG.EXIT_DRIFT, NRG.run("--stale-translations", "HEAD", "-f", src.toString()));
        String out = getOutAndClear();
        assertTrue(out.contains("README.src.md:2-3: 'en' changed since HEAD, 'ru' did not"), out);
        assertTrue(out.contains("en was: Fast") && out.contains("en now: Fast and safe"), out);

        write("README.src.md", "<!--@nrg.languages=en,ru-->\nFast and safe<!--en-->\nБыстро и надёжно<!--ru-->\n");
        assertEquals(NRG.EXIT_OK, NRG.run("--stale-translations", "HEAD", "-f", src.toString()));

        assertEquals(NRG.EXIT_USAGE, NRG.run("--stale-translations", "no-such-revision", "-f", src.toString()));
        assertTrue(getErrAndClear().contains("cannot read"));
    }

    @Test
    public void testFragmentsAddedSinceTheRevisionAreSkipped() throws IOException {
        Path fragment = write("docs/new.src.md", "Only English<!--en-->\nТолько русский<!--ru-->\n");
        Path src = write("README.src.md", "<!--@nrg.languages=en,ru-->\nA<!--en-->\nА<!--ru-->\n"
                + "${widget:import(path='docs/new.src.md')}\n");
        Map<File, String> old = new HashMap<>();
        old.put(src.toFile().getAbsoluteFile().toPath().normalize().toFile(), "<!--@nrg.languages=en,ru-->\nB<!--en-->\nА<!--ru-->\n");

        int code = new ManifestCommand(StandardCharsets.UTF_8, null, tempDir.toFile())
                .load(Collections.singletonList(src.toFile()))
                .printStaleTranslations("base", RunReport.Format.JSON, (file, ref) -> old.get(file));

        assertEquals(NRG.EXIT_DRIFT, code);
        String all = getOutAndClear();
        String out = all.substring(all.indexOf("{\"version\""));
        assertTrue(out.contains("\"kind\": \"block\"") && out.contains("\"changed\": [\"en\"]"), out);
        assertFalse(out.contains(fragment.getFileName().toString()), "a new fragment has no previous version: " + out);
    }
}
