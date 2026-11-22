package com.nanolaba.nrg;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Everything NRG prints on stdout is UTF-8, like the files it writes, whatever the encoding of
 * {@code System.out}. Each run gets a {@code System.out} that can encode nothing but ASCII, as JDK 8
 * to 17 set it up under {@code LANG=C}: text printed through it would turn every non-ASCII
 * character into {@code ?}.
 */
public class Utf8StdoutTest extends DefaultNRGTest {

    private static final String TEXT = "More ✓ 中文 Ж.";

    @TempDir
    Path tempDir;

    private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    private PrintStream previous;

    @BeforeEach
    public void asciiStdout() throws IOException {
        previous = System.out;
        System.setOut(new PrintStream(stdout, true, "US-ASCII"));
    }

    @AfterEach
    public void restoreStdout() {
        System.setOut(previous);
    }

    private String stdout() {
        String text = new String(stdout.toByteArray(), StandardCharsets.UTF_8);
        stdout.reset();
        return text;
    }

    private Path template(String name) throws IOException {
        Path src = tempDir.resolve(name);
        Files.write(src, ("<!--@nrg.languages=en-->\n# Title\n" + TEXT + "\n").getBytes(StandardCharsets.UTF_8));
        return src;
    }

    @Test
    public void testStdoutPrintsTheBytesOfTheFile() throws IOException {
        Path src = template("S.src.md");

        assertEquals(NRG.EXIT_OK, NRG.run("--no-header", "--stdout", "-f", src.toString()));
        byte[] printed = stdout.toByteArray();
        assertEquals(NRG.EXIT_OK, NRG.run("--no-header", "--log-level", "warn", "-f", src.toString()));

        assertEquals(new String(Files.readAllBytes(tempDir.resolve("S.md")), StandardCharsets.UTF_8),
                new String(printed, StandardCharsets.UTF_8));
    }

    @Test
    public void testStdoutWithSeparatorsIsUtf8() throws IOException {
        Path src = template("Ж.src.md");

        assertEquals(NRG.EXIT_OK, NRG.run("--stdout", "--language", "en", src.toString(), template("S.src.md").toString()));
        String out = stdout();
        assertTrue(out.contains("=== Ж.md ==="), out);
        assertTrue(out.contains(TEXT), out);
    }

    @Test
    public void testReportsAreUtf8() throws IOException {
        Path src = template("Ж.src.md");

        assertEquals(NRG.EXIT_OK, NRG.run("--format", "json", "--validate", src.toString()));
        assertTrue(stdout().contains("Ж.src.md\""));
        Files.write(src, "<!--@nrg.languages=en-->\n${widget:doesNotExist}\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(NRG.EXIT_TEMPLATE_ERROR, NRG.run("--format", "github", "--validate", src.toString()));
        assertTrue(stdout().contains("Ж.src.md"));
    }

    @Test
    public void testQueriesAreUtf8() throws IOException {
        Path src = template("Ж.src.md");

        assertEquals(NRG.EXIT_OK, NRG.run("--manifest", src.toString()));
        assertTrue(stdout().contains("Ж.md (en)"));
        assertEquals(NRG.EXIT_OK, NRG.run("--affected", src.toString(), "-f", src.toString()));
        assertTrue(stdout().contains("Ж.src.md"));
    }

    @Test
    public void testSyntaxCardIsUtf8() {
        assertEquals(NRG.EXIT_OK, NRG.run("--help", "syntax"));
        assertTrue(stdout().contains("Привет"), "the card's ru:'Привет' example");
    }

    @Test
    public void testPostEditHookOutputIsUtf8() throws IOException {
        Path src = template("Ж.src.md");
        String json = "{\"cwd\": " + RunReport.str(tempDir.toString()) + ", \"tool_name\": \"Edit\", "
                + "\"tool_input\": {\"file_path\": " + RunReport.str(src.toString()) + "}}";

        assertEquals(NRG.EXIT_OK, new ClaudeHook(StandardCharsets.UTF_8, Collections.emptyList(), name -> null)
                .run("post-edit", new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))));
        assertTrue(stdout().contains("Ж.md"));
    }
}
