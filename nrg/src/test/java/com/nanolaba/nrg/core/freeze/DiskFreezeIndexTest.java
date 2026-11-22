package com.nanolaba.nrg.core.freeze;

import com.nanolaba.logging.MemoryLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import static com.nanolaba.logging.LogEntry.LogEntryLevel.WARN;
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

    @Test
    void malformedAndDuplicateBlocksAreWarnedAbout(@TempDir Path tmp) throws Exception {
        File f = tmp.resolve("README.md").toFile();
        Files.write(f.toPath(), String.join("\n",
                "<!--nrg.freeze id=\"x\"-->", "first", "<!--/nrg.freeze-->",
                "<!--nrg.freeze id=\"x\"-->", "second", "<!--/nrg.freeze-->",
                "").getBytes(StandardCharsets.UTF_8));
        try (MemoryLogger log = MemoryLogger.capture()) {
            DiskFreezeIndex.read(f);
            assertEquals(Arrays.asList(
                            "freeze: ERROR: README.md:4: duplicate freeze id 'x' at lines 1 and 4",
                            "freeze id='x' in README.md: duplicate occurrences; using first, ignoring rest"),
                    log.getMessages());
        }
    }

    @Test
    void emptyBlockGivesEmptyContent(@TempDir Path tmp) throws Exception {
        File f = tmp.resolve("README.md").toFile();
        Files.write(f.toPath(), "<!--nrg.freeze id=\"x\"-->\n<!--/nrg.freeze-->\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(Optional.of(""), DiskFreezeIndex.read(f).lookup("x"));
    }

    @Test
    void unreadableFileGivesEmptyIndexWithWarn(@TempDir Path tmp) {
        // The file vanishes between the isFile() check and the read: the read fails with an
        // IOException, which must degrade to "nothing frozen" rather than abort the run.
        File vanished = new File(tmp.toFile(), "README.md") {
            @Override
            public boolean isFile() {
                return true;
            }
        };
        try (MemoryLogger log = MemoryLogger.capture()) {
            DiskFreezeIndex idx = DiskFreezeIndex.read(vanished);
            assertFalse(idx.lookup("x").isPresent());
            assertTrue(log.contains(WARN, "Could not read " + vanished.getAbsolutePath() + " for freeze lookup: "),
                    log.getMessages().toString());
        }
    }
}
