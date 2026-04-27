package com.nanolaba.nrg.core.freeze;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DiskFreezeIndexTest {

    @Test
    void missingFileGivesEmptyIndex(@TempDir Path tmp) {
        File missing = tmp.resolve("README.md").toFile();
        DiskFreezeIndex idx = DiskFreezeIndex.read(missing);
        assertFalse(idx.lookup("any").isPresent());
    }

    @Test
    void readsFreezeContentVerbatim(@TempDir Path tmp) throws Exception {
        File f = tmp.resolve("README.md").toFile();
        String body = String.join("\n",
                "before",
                "<!--nrg.freeze id=\"contrib\"-->",
                "<table>",
                "  <tr><td>alice</td></tr>",
                "</table>",
                "<!--/nrg.freeze-->",
                "after",
                "");
        Files.write(f.toPath(), body.getBytes(StandardCharsets.UTF_8));
        DiskFreezeIndex idx = DiskFreezeIndex.read(f);
        Optional<String> got = idx.lookup("contrib");
        assertTrue(got.isPresent());
        assertEquals("<table>\n  <tr><td>alice</td></tr>\n</table>\n", got.get());
    }

    @Test
    void malformedBlockIsSkippedWithWarn(@TempDir Path tmp) throws Exception {
        File f = tmp.resolve("README.md").toFile();
        String body = "<!--nrg.freeze id=\"x\"-->\nhello\n"; // missing close
        Files.write(f.toPath(), body.getBytes(StandardCharsets.UTF_8));
        DiskFreezeIndex idx = DiskFreezeIndex.read(f);
        assertFalse(idx.lookup("x").isPresent());
    }

    @Test
    void duplicateIdKeepsFirst(@TempDir Path tmp) throws Exception {
        File f = tmp.resolve("README.md").toFile();
        String body = String.join("\n",
                "<!--nrg.freeze id=\"x\"-->", "first", "<!--/nrg.freeze-->",
                "<!--nrg.freeze id=\"x\"-->", "second", "<!--/nrg.freeze-->",
                "");
        Files.write(f.toPath(), body.getBytes(StandardCharsets.UTF_8));
        DiskFreezeIndex idx = DiskFreezeIndex.read(f);
        assertEquals(Optional.of("first\n"), idx.lookup("x"));
    }
}
