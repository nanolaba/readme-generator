package com.nanolaba.nrg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code --agent-rules}: the instruction block computed from the templates, written into an
 * {@code AGENTS.md} between its markers without touching the rest, and verified with
 * {@code --check}.
 */
public class AgentRulesCliTest extends DefaultNRGTest {

    @TempDir
    Path tempDir;

    private Path write(String name, String body) throws IOException {
        Path p = tempDir.resolve(name);
        Files.createDirectories(p.getParent());
        Files.write(p, body.getBytes(StandardCharsets.UTF_8));
        return p;
    }

    private String read(String name) throws IOException {
        return new String(Files.readAllBytes(tempDir.resolve(name)), StandardCharsets.UTF_8);
    }

    private Path template() throws IOException {
        write("docs/part.src.md", "Part<!--en-->\nЧасть<!--ru-->\n");
        write("Example.java", "class Example {}\n");
        return write("README.src.md", "<!--@nrg.languages=en,ru-->\n" +
                "${widget:import(path='docs/part.src.md')}\n" +
                "${widget:import(path='Example.java', wrap='true')}\n");
    }

    private ManifestCommand command(Path src) throws IOException {
        return new ManifestCommand(StandardCharsets.UTF_8, null, tempDir.toFile())
                .load(Collections.singletonList(src.toFile()));
    }

    @Test
    public void testBlockNamesGeneratedFilesTemplatesLanguagesAndCommands() throws IOException {
        Path src = template();

        assertEquals(NRG.EXIT_OK, command(src).agentRules(null, false));
        String out = getOutAndClear();
        int begin = out.indexOf(ManifestCommand.RULES_BEGIN);
        assertTrue(begin >= 0, out);
        String block = out.substring(begin);
        assertTrue(block.trim().endsWith(ManifestCommand.RULES_END), block);
        assertTrue(block.contains("`README.md`, `README.ru.md` (from `README.src.md`)"), block);
        assertTrue(block.contains("the fragments they import (`docs/*.src.md`)"), block);
        assertTrue(block.contains("`Example.java`"), "embedded non-template inputs are listed: " + block);
        assertTrue(block.contains("Languages: `en`, `ru`"), block);
        assertTrue(block.contains("`nrg --validate README.src.md && nrg README.src.md`"), block);
        assertFalse(block.contains("generation date"), block);
    }

    @Test
    public void testWrapperIsPreferredWhenPresent() throws IOException {
        Path src = template();
        write("nrgw", "#!/bin/sh\n");
        write(".nrg/wrapper.properties", "version=1.3\n");

        command(src).agentRules(null, false);
        assertTrue(getOutAndClear().contains("`./nrgw --validate README.src.md && ./nrgw README.src.md`"));
    }

    @Test
    public void testWriteReplacesOnlyTheBlockAndCheckVerifiesIt() throws IOException {
        Path src = template();
        write("AGENTS.md", "# Rules\n\nKeep this.\n");

        assertEquals(NRG.EXIT_OK, command(src).agentRules(tempDir.resolve("AGENTS.md").toFile(), false));
        String first = read("AGENTS.md");
        assertTrue(first.startsWith("# Rules\n\nKeep this.\n\n" + ManifestCommand.RULES_BEGIN), first);
        assertEquals(NRG.EXIT_OK, command(src).agentRules(tempDir.resolve("AGENTS.md").toFile(), true),
                "a freshly written block is up to date");

        write("AGENTS.md", first.replace("Keep this.", "Keep this, edited.") + "\nTrailing notes.\n");
        write("docs/second.src.md", "Second<!--en-->\nВторой<!--ru-->\n");
        Path changed = write("README.src.md", "<!--@nrg.languages=en,ru-->\n${widget:import(path='docs/second.src.md')}\n");
        getOutAndClear();
        assertEquals(NRG.EXIT_DRIFT, command(changed).agentRules(tempDir.resolve("AGENTS.md").toFile(), true));
        assertTrue(getErrAndClear().contains("Update with: nrg --agent-rules --write"));

        assertEquals(NRG.EXIT_OK, command(changed).agentRules(tempDir.resolve("AGENTS.md").toFile(), false));
        String second = read("AGENTS.md");
        assertTrue(second.contains("Keep this, edited."), second);
        assertTrue(second.contains("Trailing notes."), second);
        assertFalse(second.contains("Example.java"), "the old block is replaced, not appended: " + second);
        assertEquals(1, second.split("nrg:agent-rules:begin", -1).length - 1, second);
    }

    @Test
    public void testSingleLanguageAndDateDependentTemplate() throws IOException {
        Path src = write("README.src.md", "Updated ${widget:date}\n");

        command(src).agentRules(null, false);
        String block = getOutAndClear();
        assertFalse(block.contains("Languages:"), block);
        assertTrue(block.contains("CI should gate on `--validate`"), block);
    }

    @Test
    public void testCliOptionRules() throws IOException {
        Path src = template();
        assertEquals(NRG.EXIT_USAGE, NRG.run("--write", "AGENTS.md", "-f", src.toString()));
        assertEquals(NRG.EXIT_USAGE, NRG.run("--agent-rules", "--check", "-f", src.toString()));
        assertEquals(NRG.EXIT_OK, NRG.run("--agent-rules", "-f", src.toString()));
        assertTrue(getOutAndClear().contains(ManifestCommand.RULES_BEGIN));
    }
}
