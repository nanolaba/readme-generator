package com.nanolaba.nrg.core;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProjectRootTest {

    @Test
    void detectsNearestAncestorWithGitDirectory(@TempDir Path tmp) throws IOException {
        Files.createDirectories(tmp.resolve("repo/.git"));
        Files.createDirectories(tmp.resolve("repo/docs/deep"));

        File root = ProjectRoot.detect(tmp.resolve("repo/docs/deep/README.src.md").toFile());

        assertEquals(tmp.resolve("repo").toFile().getCanonicalFile(), root);
    }

    @Test
    void gitFileOfAWorktreeCountsToo(@TempDir Path tmp) throws IOException {
        Files.createDirectories(tmp.resolve("wt/docs"));
        Files.write(tmp.resolve("wt/.git"), "gitdir: elsewhere".getBytes());

        File root = ProjectRoot.detect(tmp.resolve("wt/docs/README.src.md").toFile());

        assertEquals(tmp.resolve("wt").toFile().getCanonicalFile(), root);
    }

    @Test
    void fallsBackToTheSourceDirectoryWithoutGit(@TempDir Path tmp) throws IOException {
        Files.createDirectories(tmp.resolve("plain/docs"));

        File root = ProjectRoot.detect(tmp.resolve("plain/docs/README.src.md").toFile());

        assertEquals(tmp.resolve("plain/docs").toFile().getCanonicalFile(), root);
    }

    @Test
    void containsResolvesDotDotBeforeComparing(@TempDir Path tmp) throws IOException {
        File root = Files.createDirectories(tmp.resolve("repo")).toFile();
        Files.createDirectories(tmp.resolve("repo-sibling"));

        assertTrue(ProjectRoot.contains(root, new File(root, "a/b.md")));
        assertTrue(ProjectRoot.contains(root, new File(root, "a/../b.md")));
        assertTrue(ProjectRoot.contains(root, root));
        assertFalse(ProjectRoot.contains(root, new File(root, "../outside.md")));
        assertFalse(ProjectRoot.contains(root, new File(root, "a/../../outside.md")));
        // A sibling whose name merely starts with the root's name is not inside the root.
        assertFalse(ProjectRoot.contains(root, tmp.resolve("repo-sibling/x.md").toFile()));
    }

    /** Creates a symbolic link, or skips the test where the platform refuses (Windows without the privilege). */
    private static void symlink(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            Assumptions.assumeTrue(false, "symbolic links are not available here: " + e);
        }
    }

    @Test
    void symbolicLinkInsideTheRootDoesNotWidenIt(@TempDir Path tmp) throws IOException {
        File root = Files.createDirectories(tmp.resolve("repo")).toFile();
        Path outside = Files.createDirectories(tmp.resolve("outside"));
        Files.write(outside.resolve("secret.txt"), "x".getBytes());
        symlink(tmp.resolve("repo/dir-link"), outside);
        symlink(tmp.resolve("repo/file-link.md"), outside.resolve("secret.txt"));

        assertFalse(ProjectRoot.contains(root, new File(root, "dir-link/secret.txt")));
        assertFalse(ProjectRoot.contains(root, new File(root, "dir-link/not-yet-created.md")));
        assertFalse(ProjectRoot.contains(root, new File(root, "file-link.md")));
    }

    @Test
    void dotDotAfterALinkIsResolvedAgainstTheLinkTarget(@TempDir Path tmp) throws IOException {
        // repo/link -> outside/deep, so repo/link/../secret.txt is outside/secret.txt for the
        // operating system, although lexically it looks like repo/secret.txt.
        File root = Files.createDirectories(tmp.resolve("repo")).toFile();
        Path deep = Files.createDirectories(tmp.resolve("outside/deep"));
        symlink(tmp.resolve("repo/link"), deep);

        assertFalse(ProjectRoot.contains(root, new File(root, "link/../secret.txt")));
    }

    @Test
    void danglingLinkIsFollowedToItsTarget(@TempDir Path tmp) throws IOException {
        // Writing through a dangling link creates the target, so the target is what counts.
        File root = Files.createDirectories(tmp.resolve("repo")).toFile();
        symlink(tmp.resolve("repo/out.md"), tmp.resolve("outside-not-created.md"));
        symlink(tmp.resolve("repo/in.md"), tmp.resolve("repo/inside-not-created.md"));

        assertFalse(ProjectRoot.contains(root, new File(root, "out.md")));
        assertTrue(ProjectRoot.contains(root, new File(root, "in.md")));
    }

    @Test
    void windowsJunctionInsideTheRootDoesNotWidenIt(@TempDir Path tmp) throws Exception {
        // Junctions need no privilege on Windows, unlike symbolic links, and
        // File.getCanonicalFile() does not follow them there.
        Assumptions.assumeTrue(System.getProperty("os.name").toLowerCase().startsWith("windows"));
        File root = Files.createDirectories(tmp.resolve("repo")).toFile();
        Path deep = Files.createDirectories(tmp.resolve("outside/deep"));
        Path junction = tmp.resolve("repo/junction");
        Process mklink = new ProcessBuilder("cmd", "/c", "mklink", "/J", junction.toString(), deep.toString())
                .redirectErrorStream(true).start();
        Assumptions.assumeTrue(mklink.waitFor() == 0, "mklink /J failed");
        try {
            assertFalse(ProjectRoot.contains(root, new File(root, "junction/secret.txt")));
            assertFalse(ProjectRoot.contains(root, new File(root, "junction/../secret.txt")));
            assertTrue(ProjectRoot.contains(root, new File(root, "plain/secret.txt")));
        } finally {
            // Removes the junction itself; @TempDir cleanup cannot delete one.
            Files.delete(junction);
        }
    }

    @Test
    void linkThatStaysInsideTheRootIsFine(@TempDir Path tmp) throws IOException {
        File root = Files.createDirectories(tmp.resolve("repo")).toFile();
        Path real = Files.createDirectories(tmp.resolve("repo/real"));
        symlink(tmp.resolve("repo/alias"), real);

        assertTrue(ProjectRoot.contains(root, new File(root, "alias/x.md")));
    }

    @Test
    void requireNamesThePathAndTheWayOut(@TempDir Path tmp) throws IOException {
        File root = Files.createDirectories(tmp.resolve("repo")).toFile();
        File inside = new File(root, "x.md");

        assertSame(inside, ProjectRoot.require(root, inside, "import path"));

        ProjectRoot.OutsideProjectRootException e = assertThrows(ProjectRoot.OutsideProjectRootException.class,
                () -> ProjectRoot.require(root, new File(root, "../secret.txt"), "import path"));
        assertTrue(e.getMessage().startsWith("import path is outside the project root"), e.getMessage());
        assertTrue(e.getMessage().contains("secret.txt"), e.getMessage());
        assertTrue(e.getMessage().contains("--root"), e.getMessage());
    }
}
