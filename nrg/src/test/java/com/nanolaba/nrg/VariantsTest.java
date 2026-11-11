package com.nanolaba.nrg;

import com.nanolaba.nrg.core.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Output variants ({@code nrg.variants}): one template generated once per variant and per
 * language — for instance {@code AGENTS.md}, {@code CLAUDE.md} and a Copilot file from one
 * source — with {@code <!--variant:…-->} line selection, {@code ${nrg.variant}}, file name
 * placeholders, collision detection and the CLI / validator / manifest support.
 */
public class VariantsTest extends DefaultNRGTest {

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

    private Path agents() throws IOException {
        write("docs/build.src.md", "Build with mvn.\nClaude build note.<!--variant:claude-->\n");
        return write("AGENTS.src.md", "<!--@nrg.variants=agents,claude,copilot-->\n"
                + "<!--@nrg.fileNamePattern=<VARIANT>.md-->\n"
                + "<!--@nrg.variant.copilot.file=.github/copilot-instructions.md-->\n"
                + "<!--@nrg.noHeader=true-->\n"
                + "# Rules for ${nrg.variant}\n"
                + "Shared.\n"
                + "Claude and Copilot.<!--variant:claude,copilot-->\n"
                + "Not Copilot.<!--variant:!copilot-->\n"
                + "${widget:import(path='docs/build.src.md')}\n"
                + "${widget:if(cond='${nrg.variant} == agents')}\n"
                + "Generic agents only.\n"
                + "${widget:endIf}\n");
    }

    @Test
    public void testEachVariantGetsItsFileAndItsLines() throws IOException {
        Path src = agents();

        assertEquals(NRG.EXIT_OK, NRG.run("-f", src.toString()));
        String agents = read("AGENTS.md");
        String claude = read("CLAUDE.md");
        String copilot = read(".github/copilot-instructions.md");

        assertTrue(agents.startsWith("# Rules for agents"), agents);
        assertTrue(agents.contains("Not Copilot.") && agents.contains("Generic agents only."), agents);
        assertFalse(agents.contains("Claude and Copilot.") || agents.contains("Claude build note."), agents);

        assertTrue(claude.contains("Claude and Copilot.") && claude.contains("Not Copilot."), claude);
        assertTrue(claude.contains("Claude build note."), "variant markers work in imported fragments: " + claude);
        assertFalse(claude.contains("Generic agents only."), claude);

        assertTrue(copilot.contains("Claude and Copilot."), copilot);
        assertFalse(copilot.contains("Not Copilot."), copilot);
        assertFalse(copilot.contains("<!--variant"), "markers are stripped from the output: " + copilot);
    }

    @Test
    public void testDefaultNamesCombineVariantAndLanguage() throws IOException {
        Path src = write("README.src.md", "<!--@nrg.languages=en,ru-->\n<!--@nrg.variants=user,dev-->\n"
                + "${widget:languages}\nHello<!--en-->\nПривет<!--ru-->\nDev notes<!--variant:dev-->\n");

        assertEquals(NRG.EXIT_OK, NRG.run("-f", src.toString()));
        assertTrue(Files.exists(tempDir.resolve("README.user.md")));
        assertTrue(Files.exists(tempDir.resolve("README.user.ru.md")));
        assertTrue(read("README.dev.ru.md").contains("Dev notes"));
        assertFalse(read("README.user.ru.md").contains("Dev notes"));
        assertTrue(read("README.dev.md").contains("(README.dev.ru.md)"),
                "the language switcher stays inside the variant: " + read("README.dev.md"));
    }

    @Test
    public void testVariantsWritingTheSameFileAreAnError() throws IOException {
        Path src = write("README.src.md", "<!--@nrg.variants=a,b-->\n<!--@nrg.fileNamePattern=OUT.md-->\nx\n");

        assertEquals(NRG.EXIT_TEMPLATE_ERROR, NRG.run("-f", src.toString()));
        String err = getErrAndClear();
        assertTrue(err.contains("variants 'a' and 'b' write the same file"), err);
    }

    @Test
    public void testStdoutVariantFilter() throws IOException {
        Path src = agents();

        assertEquals(NRG.EXIT_OK, NRG.run("--stdout", "--variant", "claude", "-f", src.toString()));
        String out = getOutAndClear();
        assertTrue(out.contains("# Rules for claude"), out);
        assertFalse(out.contains("Rules for agents"), out);
        assertFalse(out.contains("=== "), "a single output needs no separator: " + out);
    }

    @Test
    public void testValidatorKnowsVariants() throws IOException {
        Path ok = agents();
        assertTrue(new Validator(ok.toFile()).validate().isEmpty(), new Validator(ok.toFile()).validate().toString());

        Path typo = write("T.src.md", "<!--@nrg.variants=claude,gemini-->\nx<!--variant:claud-->\n");
        List<Validator.Diagnostic> d = new Validator(typo.toFile()).validate();
        assertEquals(1, d.size(), d.toString());
        assertEquals("undeclared-variant", d.get(0).getCode());
        assertEquals("did you mean 'claude'?", d.get(0).getHint());

        Path none = write("N.src.md", "x<!--variant:claude-->\n");
        assertEquals("undeclared-variant", new Validator(none.toFile()).validate().get(0).getCode());
    }

    @Test
    public void testManifestAndIsGeneratedCoverEveryVariant() throws IOException {
        Path src = agents();

        assertEquals(NRG.EXIT_OK, NRG.run("--manifest", "-f", src.toString()));
        String out = getOutAndClear();
        assertTrue(out.contains("(variant agents)") && out.contains("(variant copilot)"), out);
        assertEquals(NRG.EXIT_OK, NRG.run("--is-generated", tempDir.resolve(".github/copilot-instructions.md").toString(),
                "-f", src.toString()));
    }

    @Test
    public void testTableOfContentsFollowsTheVariant() throws IOException {
        Path src = write("README.src.md", "<!--@nrg.variants=a,b-->\n${widget:toc}\n## Common\n## Only A<!--variant:a-->\n");

        assertEquals(NRG.EXIT_OK, NRG.run("-f", src.toString()));
        assertTrue(read("README.a.md").contains("[Only A]"), read("README.a.md"));
        assertFalse(read("README.b.md").contains("Only A"), read("README.b.md"));
    }
}
