package com.nanolaba.nrg.core.freeze;

import com.nanolaba.nrg.core.Generator;
import com.nanolaba.nrg.core.GeneratorConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FreezeBlockProcessorTest {

    @Test
    void bootstrapKeepsTemplatePlaceholderWhenDiskAbsent(@TempDir Path tmp) throws Exception {
        File source = tmp.resolve("README.src.md").toFile();
        Files.write(source.toPath(), "<!--@nrg.languages=en-->\n".getBytes(StandardCharsets.UTF_8));
        Generator g = new Generator(source, "<!--@nrg.languages=en-->\n", null);
        GeneratorConfig cfg = g.getConfig();

        String rendered = String.join("\n",
                "intro",
                "<!--nrg.freeze id=\"x\"-->",
                "placeholder",
                "<!--/nrg.freeze-->",
                "outro",
                "");
        String resolved = FreezeBlockProcessor.resolve(rendered, cfg, "en");

        assertTrue(resolved.contains("placeholder"), "bootstrap should retain placeholder when no disk file");
        assertTrue(resolved.contains("<!--nrg.freeze id=\"x\"-->"));
        assertTrue(resolved.contains("<!--/nrg.freeze-->"));
    }

    @Test
    void splicesContentFromDisk(@TempDir Path tmp) throws Exception {
        File source = tmp.resolve("README.src.md").toFile();
        Files.write(source.toPath(), "<!--@nrg.languages=en-->\n".getBytes(StandardCharsets.UTF_8));

        File diskOutput = tmp.resolve("README.md").toFile();
        Files.write(diskOutput.toPath(), String.join("\n",
                "<!--nrg.freeze id=\"x\"-->",
                "<table>aliсe</table>",
                "<!--/nrg.freeze-->",
                "").getBytes(StandardCharsets.UTF_8));

        Generator g = new Generator(source, "<!--@nrg.languages=en-->\n", null);
        GeneratorConfig cfg = g.getConfig();

        String rendered = String.join("\n",
                "<!--nrg.freeze id=\"x\"-->",
                "placeholder",
                "<!--/nrg.freeze-->",
                "");
        String resolved = FreezeBlockProcessor.resolve(rendered, cfg, "en");

        assertTrue(resolved.contains("<table>aliсe</table>"), "should splice on-disk content");
        assertFalse(resolved.contains("placeholder"), "placeholder should be replaced");
        assertTrue(resolved.contains("<!--nrg.freeze id=\"x\"-->"), "open marker preserved");
        assertTrue(resolved.contains("<!--/nrg.freeze-->"), "close marker preserved");
    }

    @Test
    void sourceLangPullsFromOtherLanguageFile(@TempDir Path tmp) throws Exception {
        File source = tmp.resolve("README.src.md").toFile();
        String tpl = "<!--@nrg.languages=en,ru-->\n<!--@nrg.defaultLanguage=en-->\n";
        Files.write(source.toPath(), tpl.getBytes(StandardCharsets.UTF_8));

        // Disk: README.md has the table, README.ru.md does not exist.
        File enDisk = tmp.resolve("README.md").toFile();
        Files.write(enDisk.toPath(), String.join("\n",
                "<!--nrg.freeze id=\"x\"-->",
                "<table>EN</table>",
                "<!--/nrg.freeze-->",
                "").getBytes(StandardCharsets.UTF_8));

        Generator g = new Generator(source, tpl, null);
        GeneratorConfig cfg = g.getConfig();

        String rendered = String.join("\n",
                "<!--nrg.freeze id=\"x\" source-lang=\"en\"-->",
                "placeholder",
                "<!--/nrg.freeze-->",
                "");

        String resolvedRu = FreezeBlockProcessor.resolve(rendered, cfg, "ru");
        assertTrue(resolvedRu.contains("<table>EN</table>"),
                "ru output should pull from en disk file via source-lang");
    }
}
