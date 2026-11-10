package com.nanolaba.nrg.core;

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
