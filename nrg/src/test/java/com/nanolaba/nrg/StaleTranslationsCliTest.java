package com.nanolaba.nrg;

import com.nanolaba.nrg.core.json.MinimalJsonParser;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code --stale-translations}: the CLI over a real git repository, plus the handling of
 * fragments added since the revision and of an unreadable revision.
 */
public class StaleTranslationsCliTest extends DefaultNRGTest {

    @TempDir
    Path tempDir;

    /** Only errors are logged, so stdout holds nothing but what the command under test prints. */
    @BeforeEach
    public void logErrorsOnly() {
        NRG.applyLogLevel(NRG.LogLevel.ERROR);
    }

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

    private static final String OLD_BODY = "<!--@nrg.languages=en,ru-->\nOne<!--en-->\nОдин<!--ru-->\nx\n"
            + "Speed: ${en:'Fast', ru:'Быстро'}\n";
    private static final String NEW_BODY = "<!--@nrg.languages=en,ru-->\nOne<!--en-->\nTwo<!--en-->\nThree<!--en-->\n"
            + "Один<!--ru-->\nx\nSpeed: ${en:'Fast and safe', ru:'Быстро'}\n";

    private int staleTranslations(Path src, String oldBody, RunReport.Format format) throws IOException {
        return new ManifestCommand(StandardCharsets.UTF_8, null, tempDir.toFile())
                .load(Collections.singletonList(src.toFile()))
                .printStaleTranslations("base", format, (file, ref) -> oldBody);
    }

    @Test
    public void testTextReportShowsOldAndNewTextOfEveryFinding() throws IOException {
        Path src = write("README.src.md", NEW_BODY);

        assertEquals(NRG.EXIT_DRIFT, staleTranslations(src, OLD_BODY, RunReport.Format.TEXT));
        assertEquals(Arrays.asList(
                "README.src.md:2-5: 'en' changed since base, 'ru' did not",
                "    en was: (nothing)",
                "    en now: Two",
                "            Three",
                "README.src.md:7: 'en' changed since base, 'ru' did not (in a ${...} language construct)",
                "    en was: Fast",
                "    en now: Fast and safe"), Arrays.asList(getOutAndClear().trim().split("\\R")));
        assertEquals("Update the other languages, or mark an intentional one-sided edit with <!--nrg.translation.ok-->",
                getErrAndClear().trim());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testJsonReportListsEveryFinding() throws IOException {
        Path src = write("README.src.md", NEW_BODY);

        assertEquals(NRG.EXIT_DRIFT, staleTranslations(src, OLD_BODY, RunReport.Format.JSON));
        Map<String, Object> report = (Map<String, Object>) MinimalJsonParser.parse(getOutAndClear());
        assertEquals("stale-translations", report.get("mode"));
        assertEquals("base", report.get("base"));
        List<Map<String, Object>> findings = (List<Map<String, Object>>) report.get("findings");
        assertEquals(2, findings.size());
        assertEquals("block", findings.get(0).get("kind"));
        assertEquals(5L, ((Number) findings.get(0).get("lastLine")).longValue());
        assertEquals(Collections.singletonMap("en", ""), findings.get(0).get("old"));
        assertEquals(Collections.singletonMap("en", "Two\nThree"), findings.get(0).get("new"));
        assertEquals("construct", findings.get(1).get("kind"));
        assertEquals(Collections.singletonList("ru"), findings.get(1).get("unchanged"));
        assertEquals(Collections.singletonMap("en", "Fast"), findings.get(1).get("old"));
        assertEquals("", getErrAndClear(), "the hint is for people reading the text report");
    }

    @Test
    public void testNothingStale() throws IOException {
        Path src = write("README.src.md", OLD_BODY);

        assertEquals(NRG.EXIT_OK, staleTranslations(src, OLD_BODY, RunReport.Format.TEXT));
        assertEquals("", getOutAndClear() + getErrAndClear());
        assertEquals(NRG.EXIT_OK, staleTranslations(src, OLD_BODY, RunReport.Format.JSON));
        assertEquals("{\"version\": " + RunReport.str(NRG.getVersion().trim())
                + ", \"mode\": \"stale-translations\", \"base\": \"base\", \"findings\": []}", getOutAndClear().trim());
    }
}
