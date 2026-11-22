package com.nanolaba.nrg.core;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        symlinks(link.getParent(), Collections.singletonMap(link.getFileName().toString(), target));
    }

    /**
     * Creates the symbolic links {@code dir/name -> target}, or skips the test where the platform
     * refuses. JDK 8 cannot create one on Windows without the privilege even in Developer Mode,
     * where {@code mklink} can, so Windows falls back to a single {@code mklink} batch.
     */
    private static void symlinks(Path dir, Map<String, Path> links) {
        try {
            for (Map.Entry<String, Path> link : links.entrySet()) {
                Files.createSymbolicLink(dir.resolve(link.getKey()), link.getValue());
            }
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            Assumptions.assumeTrue(isWindows() && mklink(dir, links), "symbolic links are not available here: " + e);
        }
    }

    private static boolean mklink(Path dir, Map<String, Path> links) {
        List<String> command = new ArrayList<>(Arrays.asList("cmd", "/c"));
        for (Map.Entry<String, Path> link : links.entrySet()) {
            if (Files.exists(dir.resolve(link.getKey()), LinkOption.NOFOLLOW_LINKS)) {
                continue;
            }
            if (command.size() > 2) {
                command.add("&&");
            }
            command.add("mklink");
            if (Files.isDirectory(dir.resolve(link.getValue()))) {
                command.add("/D");
            }
            command.add(link.getKey());
            command.add(link.getValue().toString());
        }
        try {
            Process mklink = new ProcessBuilder(command).directory(dir.toFile()).redirectErrorStream(true).start();
            IOUtils.toString(mklink.getInputStream(), Charset.defaultCharset());
            return mklink.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows");
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
    void linkCycleFailsClosed(@TempDir Path tmp) throws IOException {
        // The file system cannot say where a cycle leads, so the path is never "inside".
        File root = Files.createDirectories(tmp.resolve("repo")).toFile();
        symlink(tmp.resolve("repo/a"), tmp.resolve("repo/b"));
        symlink(tmp.resolve("repo/b"), tmp.resolve("repo/a"));

        File a = new File(root, "a");
        assertFalse(ProjectRoot.contains(root, a));
        assertFalse(ProjectRoot.contains(root, new File(root, "a/x.md")));
        ProjectRoot.OutsideProjectRootException e = assertThrows(ProjectRoot.OutsideProjectRootException.class,
                () -> ProjectRoot.require(root, a, "import path"));
        // Nothing to resolve it to, so the message names the path as written.
        assertEquals(a.getAbsolutePath(), ProjectRoot.describe(a));
        assertTrue(e.getMessage().contains(a.getAbsolutePath()), e.getMessage());
    }

    @Test
    void unresolvableSourceDirectoryIsKeptAsWritten(@TempDir Path tmp) throws IOException {
        // A root that cannot be resolved contains nothing, so every later check fails closed.
        Files.createDirectories(tmp.resolve("repo/.git"));
        symlink(tmp.resolve("repo/a"), tmp.resolve("repo/b"));
        symlink(tmp.resolve("repo/b"), tmp.resolve("repo/a"));
        File source = tmp.resolve("repo/a/README.src.md").toFile();

        File root = ProjectRoot.detect(source);

        assertEquals(source.getAbsoluteFile().getParentFile(), root);
        assertFalse(ProjectRoot.contains(root, source));
    }

    @Test
    void danglingLinksAreFollowedUpToTheHopLimit(@TempDir Path tmp) throws IOException {
        File root = Files.createDirectories(tmp.resolve("repo")).toFile();
        Map<String, Path> links = new LinkedHashMap<>();
        addDanglingChain(links, "short", ProjectRoot.MAX_LINK_HOPS);
        addDanglingChain(links, "long", ProjectRoot.MAX_LINK_HOPS + 1);
        symlinks(root.toPath(), links);

        assertTrue(ProjectRoot.contains(root, new File(root, "short0")));
        assertFalse(ProjectRoot.contains(root, new File(root, "long0")));
    }

    /** {@code prefix0 -> prefix1 -> … -> prefix<count-1> -> prefix-target.md}, the target never created. */
    private static void addDanglingChain(Map<String, Path> links, String prefix, int count) {
        for (int i = 0; i < count; i++) {
            links.put(prefix + i, Paths.get(i + 1 < count ? prefix + (i + 1) : prefix + "-target.md"));
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
