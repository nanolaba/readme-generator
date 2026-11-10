package com.nanolaba.nrg;

import com.nanolaba.nrg.core.Generator;
import com.nanolaba.nrg.core.ProjectRoot;
import com.nanolaba.nrg.core.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A template must not be able to read anything outside the project root. The layout used
 * by every test: {@code <tmp>/outside} holds the files a template has no business reading,
 * {@code <tmp>/repo} is the project (no {@code .git}, so the root is the source directory).
 */
class ProjectRootBoundaryTest extends DefaultNRGTest {

    @TempDir
    Path tmp;

    private Path repo;

    @BeforeEach
    void layout() throws IOException {
        repo = Files.createDirectories(tmp.resolve("repo"));
        Path outside = Files.createDirectories(tmp.resolve("outside"));
        write(outside.resolve("secret.txt"), "TOP-SECRET");
        write(outside.resolve("gradle.properties"), "signing.password=GRADLE-SECRET\n");
        write(outside.resolve("package.json"), "{\"version\":\"NPM-SECRET\"}");
        write(outside.resolve("pom.xml"), "<project><version>POM-SECRET</version></project>");
    }

    private static void write(Path file, String content) throws IOException {
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }

    private String render(String body) throws IOException {
        Path src = repo.resolve("README.src.md");
        write(src, body);
        return new Generator(src.toFile(), StandardCharsets.UTF_8).getResult("en").getContent().toString();
    }

    private void assertRefused(String body, String what) {
        ProjectRoot.OutsideProjectRootException e =
                assertThrows(ProjectRoot.OutsideProjectRootException.class, () -> render(body));
        assertTrue(e.getMessage().startsWith(what + " is outside the project root"), e.getMessage());
    }

    @Test
    void importCannotLeaveTheRoot() {
        assertRefused("${widget:import(path='../outside/secret.txt')}\n", "import path");
    }

    @Test
    void importInsideAnImportedFileCannotLeaveTheRootEither() throws IOException {
        write(repo.resolve("child.src.md"), "${widget:import(path='../outside/secret.txt')}\n");
        assertRefused("${widget:import(path='child.src.md')}\n", "import path");
    }

    @Test
    void importIsCheckedInTheTableOfContentsPassToo() {
        assertRefused("${widget:toc}\n## A\n${widget:import(path='../outside/secret.txt')}\n", "import path");
    }

    @Test
    void fileTreeCannotLeaveTheRoot() {
        assertRefused("${widget:fileTree(path='../outside')}\n", "fileTree path");
        assertRefused("${widget:fileTree(path='" + tmp.resolve("outside").toString().replace('\\', '/') + "')}\n",
                "fileTree path");
    }

    @Test
    void descriptorPathsCannotLeaveTheRoot() {
        assertRefused("<!--@nrg.pom.path=../outside/pom.xml-->\n${pom.version}\n", "nrg.pom.path");
        assertRefused("<!--@nrg.npm.path=../outside/package.json-->\n${npm.version}\n", "nrg.npm.path");
        assertRefused("<!--@nrg.gradle.path=../outside-->\n${gradle.signing.password}\n", "nrg.gradle.path");
    }

    @Test
    void pathsInsideTheRootKeepWorking() throws IOException {
        Files.createDirectories(repo.resolve("docs/sub"));
        write(repo.resolve("docs/part.md"), "PART");
        write(repo.resolve("docs/sub/up.src.md"), "${widget:import(path='../part.md')}");
        write(repo.resolve("docs/pom.xml"), "<project><version>9.9</version></project>");

        String out = render("<!--@nrg.pom.path=docs/pom.xml-->\n" +
                "${widget:import(path='docs/sub/up.src.md')}\n" +
                "v=${pom.version}\n" +
                "${widget:fileTree(path='docs', depth='1')}\n");

        assertTrue(out.contains("PART"), out);
        assertTrue(out.contains("v=9.9"), out);
        assertTrue(out.contains("part.md"), out);
    }

    @Test
    void gitRepositoryRootIsTheBoundary() throws IOException {
        // With .git one level above the source directory, "../" inside the repository is fine,
        // while leaving the repository is still refused.
        Files.createDirectories(tmp.resolve("repo/.git"));
        Path docs = Files.createDirectories(repo.resolve("docs"));
        write(repo.resolve("shared.md"), "SHARED");
        Path src = docs.resolve("README.src.md");
        write(src, "${widget:import(path='../shared.md')}\n");
        assertTrue(new Generator(src.toFile(), StandardCharsets.UTF_8).getResult("en").getContent().toString()
                .contains("SHARED"));

        write(src, "${widget:import(path='../../outside/secret.txt')}\n");
        assertThrows(ProjectRoot.OutsideProjectRootException.class,
                () -> new Generator(src.toFile(), StandardCharsets.UTF_8).getResult("en"));
    }

    @Test
    void cliRootOptionWidensTheBoundary() throws IOException {
        Path src = repo.resolve("README.src.md");
        write(src, "[${widget:import(path='../outside/secret.txt')}]\n");

        assertEquals(0, NRG.run("-f", src.toString(), "--stdout", "--root", tmp.toString()));
        assertTrue(getOutAndClear().contains("[TOP-SECRET"));
    }

    @Test
    void validateReportsEscapingImportWithoutFollowingIt() throws IOException {
        Path src = repo.resolve("README.src.md");
        write(src, "${widget:import(path='../outside/secret.txt')}\n" +
                "${widget:import(path='../outside/missing.txt')}\n");

        List<Validator.Diagnostic> diagnostics = new Validator(src.toFile()).validate();

        assertEquals(2, diagnostics.size(), diagnostics.toString());
        for (Validator.Diagnostic d : diagnostics) {
            // Same message whether or not the target exists: no existence oracle.
            assertTrue(d.toString().contains("import path is outside the project root"), d.toString());
        }
        assertTrue(new Validator(src.toFile(), tmp.toFile()).validate().stream()
                .noneMatch(d -> d.toString().contains("outside the project root")));
    }
}
