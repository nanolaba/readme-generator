package com.nanolaba.nrg.core.freeze;

import com.nanolaba.nrg.core.Generator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GeneratorFreezeTest {

    @Test
    void roundTripPreservesExternalToolEdits(@TempDir Path tmp) throws Exception {
        File source = tmp.resolve("README.src.md").toFile();
        String tpl = String.join("\n",
                "<!--@nrg.languages=en-->",
                "# Title",
                "",
                "<!--nrg.freeze id=\"contrib\"-->",
                "placeholder",
                "<!--/nrg.freeze-->",
                "");
        Files.write(source.toPath(), tpl.getBytes(StandardCharsets.UTF_8));

        // First generation — bootstrap.
        Generator g1 = new Generator(source, StandardCharsets.UTF_8);
        String firstEn = g1.getResult("en").getContent().toString();
        assertTrue(firstEn.contains("placeholder"));

        // Simulate external tool: write the bootstrapped file, then mutate inside the freeze.
        File diskOut = tmp.resolve("README.md").toFile();
        Files.write(diskOut.toPath(), firstEn.getBytes(StandardCharsets.UTF_8));
        String mutated = firstEn.replace("placeholder", "<table>contributors</table>");
        Files.write(diskOut.toPath(), mutated.getBytes(StandardCharsets.UTF_8));

        // Second generation — should preserve the mutation.
        Generator g2 = new Generator(source, StandardCharsets.UTF_8);
        String secondEn = g2.getResult("en").getContent().toString();
        assertTrue(secondEn.contains("<table>contributors</table>"),
                "round-trip: external mutation must survive regeneration");
        assertFalse(secondEn.contains("placeholder"),
                "placeholder must be displaced");
    }

    @Test
    void importedFreezeResolvedAtRoot(@TempDir Path tmp) throws Exception {
        File partial = tmp.resolve("partial.src.md").toFile();
        Files.write(partial.toPath(), String.join("\n",
                "<!--nrg.freeze id=\"contrib\"-->",
                "placeholder",
                "<!--/nrg.freeze-->",
                "").getBytes(StandardCharsets.UTF_8));

        File source = tmp.resolve("README.src.md").toFile();
        String tpl = String.join("\n",
                "<!--@nrg.languages=en-->",
                "# Title",
                "${widget:import(path='partial.src.md')}",
                "");
        Files.write(source.toPath(), tpl.getBytes(StandardCharsets.UTF_8));

        File diskOut = tmp.resolve("README.md").toFile();
        Files.write(diskOut.toPath(), String.join("\n",
                "<!--nrg.freeze id=\"contrib\"-->",
                "imported-disk-content",
                "<!--/nrg.freeze-->",
                "").getBytes(StandardCharsets.UTF_8));

        Generator g = new Generator(source, StandardCharsets.UTF_8);
        String out = g.getResult("en").getContent().toString();
        assertTrue(out.contains("imported-disk-content"),
                "freeze in imported file should be resolved at root");
    }
}
