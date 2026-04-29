package com.nanolaba.nrg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Drives the {@code --no-header} and {@code --header-text} CLI flags end-to-end against a
 * temporary {@code README.src.md}, verifying that the generated output respects the chosen
 * header strategy. Also covers the mutual-exclusion guard that rejects passing both flags.
 */
public class HeaderCustomizationCliTest extends DefaultNRGTest {

    @TempDir
    Path tempDir;

    private Path writeSrc(String body) throws IOException {
        Path src = tempDir.resolve("README.src.md");
        Files.write(src, body.getBytes(StandardCharsets.UTF_8));
        return src;
    }

    @Test
    public void testNoHeaderSuppressesGeneratedHeadComment() throws IOException {
        Path src = writeSrc("<!--@nrg.languages=en-->\n## Title\n");

        int code = NRG.run("--no-header", "-f", src.toString());

        assertEquals(0, code);
        String generated = new String(
                Files.readAllBytes(tempDir.resolve("README.md")), StandardCharsets.UTF_8);
        assertFalse(generated.contains("automatically generated"),
                "default head comment should be suppressed, got: " + generated);
        assertTrue(generated.contains("## Title"));
    }

    @Test
    public void testHeaderTextReplacesDefaultHeadComment() throws IOException {
        Path src = writeSrc("<!--@nrg.languages=en-->\n## Title\n");

        int code = NRG.run("--header-text", "<!-- Internal docs: see /wiki -->",
                "-f", src.toString());

        assertEquals(0, code);
        String generated = new String(
                Files.readAllBytes(tempDir.resolve("README.md")), StandardCharsets.UTF_8);
        assertFalse(generated.contains("automatically generated"),
                "default head comment should be replaced, got: " + generated);
        assertTrue(generated.startsWith("<!-- Internal docs: see /wiki -->"),
                "custom header should be the first line, got: " + generated);
    }

    @Test
    public void testHeaderTextEscapesProduceMultiLineHeader() throws IOException {
        Path src = writeSrc("<!--@nrg.languages=en-->\n## Title\n");

        int code = NRG.run("--header-text", "Line A\\nLine B",
                "-f", src.toString());

        assertEquals(0, code);
        String generated = new String(
                Files.readAllBytes(tempDir.resolve("README.md")), StandardCharsets.UTF_8);
        assertTrue(generated.startsWith("Line A" + RN + "Line B" + RN),
                "\\n should expand to a line separator, got: " + generated);
    }

    @Test
    public void testNoHeaderAndHeaderTextAreMutuallyExclusive() throws IOException {
        Path src = writeSrc("<!--@nrg.languages=en-->\n## Title\n");

        int code = NRG.run("--no-header", "--header-text", "anything", "-f", src.toString());

        assertEquals(1, code);
        String err = getErrAndClear();
        assertTrue(err.contains("--no-header and --header-text are mutually exclusive"),
                "expected mutual-exclusion error, got: " + err);
    }
}
