package com.nanolaba.nrg;

import com.nanolaba.nrg.core.Generator;
import com.nanolaba.nrg.core.TemplateManifest;
import com.nanolaba.nrg.core.json.MinimalJsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link TemplateManifest} and the {@code --manifest} / {@code --is-generated} /
 * {@code --affected} queries built on it.
 */
public class ManifestCliTest extends DefaultNRGTest {

    @TempDir
    Path tempDir;

    private Path write(String name, String body) throws IOException {
        Path p = tempDir.resolve(name);
        Files.createDirectories(p.getParent());
        Files.write(p, body.getBytes(StandardCharsets.UTF_8));
        return p;
    }

    private Path project() throws IOException {
        Files.createDirectories(tempDir.resolve(".git"));
        write("pom.xml", "<project><version>1.0</version></project>");
        write("docs/part.src.md", "Part\n${widget:import(path='nested.src.md')}\n");
        write("docs/nested.src.md", "${widget:fileTree(path='../src')}\n${widget:exec(cmd='git log')}\n");
        write("docs/snippet.sh", "echo ${HOME}\n");
        Files.createDirectories(tempDir.resolve("src"));
        return write("README.src.md", "<!--@nrg.languages=en,ru-->\n<!--@nrg.pom.path=pom.xml-->\n" +
                "Version ${pom.version}\n" +
                "${widget:import(path='docs/part.src.md')}\n" +
                "${widget:import(path='docs/snippet.sh', run-generator='false')}\n" +
                "\\${widget:import(path='escaped.src.md')}\n" +
                "${widget:import(url='https://example.com/x.md')} ${env.TOKEN} ${widget:date}\n");
    }

    private static Path n(Path p) {
        return p.toAbsolutePath().normalize();
    }

    @Test
    public void testManifestListsOutputsInputsAndDynamicSources() throws IOException {
        Path src = project();
        TemplateManifest m = TemplateManifest.of(new Generator(src.toFile(), StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        assertEquals(n(tempDir.resolve("README.md")).toFile(), m.getOutputs().get("en"));
        assertEquals(n(tempDir.resolve("README.ru.md")).toFile(), m.getOutputs().get("ru"));
        assertTrue(m.readsInput(src.toFile()));
        assertTrue(m.readsInput(tempDir.resolve("docs/part.src.md").toFile()));
        assertTrue(m.readsInput(tempDir.resolve("docs/nested.src.md").toFile()), "imports are followed transitively");
        assertTrue(m.readsInput(tempDir.resolve("docs/snippet.sh").toFile()), "plain-text imports are inputs too");
        assertTrue(m.readsInput(tempDir.resolve("src").toFile()), "fileTree directories are inputs");
        assertTrue(m.readsInput(tempDir.resolve("pom.xml").toFile()), "descriptors behind ${pom.*} are inputs");
        assertFalse(m.readsInput(tempDir.resolve("escaped.src.md").toFile()), "escaped widgets are not");
        assertTrue(m.getDynamic().contains("url:https://example.com/x.md"), m.getDynamic().toString());
        assertTrue(m.getDynamic().contains("exec:git log"), m.getDynamic().toString());
        assertTrue(m.getDynamic().contains("env:TOKEN"), m.getDynamic().toString());
        assertTrue(m.getDynamic().contains("date"), m.getDynamic().toString());
    }

    @Test
    public void testManifestJson() throws IOException {
        Path src = project();

        assertEquals(NRG.EXIT_OK, NRG.run("--manifest", "--format", "json", "-f", src.toString()));
        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) MinimalJsonParser.parse(getOutAndClear());
        assertEquals("manifest", report.get("mode"));
        @SuppressWarnings("unchecked")
        Map<String, Object> template = ((List<Map<String, Object>>) report.get("templates")).get(0);
        assertTrue(((String) template.get("source")).endsWith("README.src.md"));
        assertEquals(2, ((List<?>) template.get("outputs")).size());
        assertTrue(((List<?>) template.get("inputs")).size() >= 6, template.toString());
    }

    @Test
    public void testIsGenerated() throws IOException {
        Path src = project();

        assertEquals(NRG.EXIT_OK, NRG.run("--is-generated", tempDir.resolve("README.ru.md").toString(), "-f", src.toString()));
        assertTrue(getOutAndClear().trim().endsWith("README.src.md"));
        assertEquals(1, NRG.run("--is-generated", tempDir.resolve("docs/part.src.md").toString(), "-f", src.toString()));
        assertEquals("", getOutAndClear().trim());
    }

    @Test
    public void testAffected() throws IOException {
        Path src = project();

        assertEquals(NRG.EXIT_OK, NRG.run("--affected", tempDir.resolve("docs/nested.src.md").toString(), "-f", src.toString()));
        assertTrue(getOutAndClear().trim().endsWith("README.src.md"));
        NRG.run("--affected", tempDir.resolve("src/Main.java").toString(), "-f", src.toString());
        assertTrue(getOutAndClear().trim().endsWith("README.src.md"), "a file inside a fileTree directory counts");
        NRG.run("--affected", tempDir.resolve("unrelated.txt").toString(), "-f", src.toString());
        assertEquals("", getOutAndClear().trim());
    }

    @Test
    public void testDiscoveryFindsRootTemplatesOnly() throws IOException {
        project();
        write("CONTRIBUTING.src.md", "Contributing\n");
        write("node_modules/pkg/README.src.md", "ignored\n");
        write(".hidden/README.src.md", "ignored\n");

        ManifestCommand command = new ManifestCommand(StandardCharsets.UTF_8, null, tempDir.toFile())
                .load(Collections.emptyList());
        assertEquals(NRG.EXIT_OK, command.printManifest(RunReport.Format.TEXT));
        List<String> lines = Arrays.asList(getOutAndClear().split("\\R"));
        assertTrue(lines.contains("README.src.md"), lines.toString());
        assertTrue(lines.contains("CONTRIBUTING.src.md"), lines.toString());
        assertFalse(lines.contains("docs/part.src.md"), "an imported fragment is not a root: " + lines);
        assertTrue(lines.contains("  input   docs/part.src.md"), lines.toString());
        assertTrue(lines.stream().noneMatch(l -> l.contains("node_modules") || l.contains(".hidden")), lines.toString());
    }

    @Test
    public void testLocate() throws IOException {
        write("docs/part.src.md", "Part line\n");
        Path src = write("README.src.md", "<!--@nrg.languages=en,ru-->\nHello<!--en-->\nПривет<!--ru-->\n"
                + "${widget:import(path='docs/part.src.md')}\n");
        String ru = tempDir.resolve("README.ru.md").toString();

        assertEquals(NRG.EXIT_OK, NRG.run("--locate", ru + ":3-4", "-f", src.toString()));
        String[] out = getOutAndClear().trim().split("\\R");
        assertTrue(out[0].contains("README.ru.md:3: ") && out[0].endsWith("/README.src.md:3"), out[0]);
        assertTrue(out[1].contains("part.src.md:1 <- imported at") && out[1].contains("README.src.md:4"), out[1]);

        assertEquals(NRG.EXIT_OK, NRG.run("--locate", ru + ":1", "--format", "json", "-f", src.toString()));
        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) MinimalJsonParser.parse(getOutAndClear());
        assertEquals("locate", report.get("mode"));
        assertEquals("ru", report.get("language"));
        @SuppressWarnings("unchecked")
        Map<String, Object> line = ((List<Map<String, Object>>) report.get("lines")).get(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> origin = (Map<String, Object>) line.get("origin");
        assertEquals("head comment", origin.get("note"));
        assertTrue(((String) line.get("text")).startsWith("<!--"), line.toString());
    }

    @Test
    public void testLocateErrors() throws IOException {
        Path src = write("README.src.md", "Hello\n");
        String md = tempDir.resolve("README.md").toString();

        assertEquals(NRG.EXIT_USAGE, NRG.run("--locate", md + ":999", "-f", src.toString()));
        assertTrue(getErrAndClear().contains("does not exist"));
        assertEquals(NRG.EXIT_USAGE, NRG.run("--locate", md + ":x", "-f", src.toString()));
        assertEquals(1, NRG.run("--locate", tempDir.resolve("other.md") + ":1", "-f", src.toString()));
        assertTrue(getErrAndClear().contains("is not generated"));
    }

    @Test
    public void testQueryModesAreExclusive() throws IOException {
        Path src = project();
        assertEquals(NRG.EXIT_USAGE, NRG.run("--manifest", "--check", "-f", src.toString()));
        assertEquals(NRG.EXIT_USAGE, NRG.run("--manifest", "--is-generated", "x", "-f", src.toString()));
    }
}
