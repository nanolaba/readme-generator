package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.DefaultNRGTest;
import com.nanolaba.nrg.core.Generator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileTreeWidgetTest extends DefaultNRGTest {

    @TempDir
    Path tempDir;

    private void mkdirs(String... paths) throws IOException {
        for (String p : paths) Files.createDirectories(tempDir.resolve(p));
    }

    private void touch(String... paths) throws IOException {
        for (String p : paths) {
            Path f = tempDir.resolve(p);
            Files.createDirectories(f.getParent());
            Files.write(f, new byte[0]);
        }
    }

    private String render(String body) {
        File src = tempDir.resolve("README.src.md").toFile();
        Generator g = new Generator(src, body, Collections.singletonList(new FileTreeWidget()));
        return g.getResult("en").getContent().toString();
    }

    @Test
    public void testFlatDirectoryRendersBoxDrawing() throws IOException {
        touch("src/a.txt", "src/b.txt");
        String out = render("${widget:fileTree(path='src', depth='1', codeblock='false')}\n");

        assertTrue(out.contains("src"), out);
        assertTrue(out.contains("├── a.txt"), out);
        assertTrue(out.contains("└── b.txt"), out);
    }

    @Test
    public void testOrderingIsAlphabeticalDirsFirst() throws IOException {
        mkdirs("root/zdir", "root/adir");
        touch("root/m.txt", "root/a.txt", "root/zdir/x.txt", "root/adir/y.txt");
        String out = render("${widget:fileTree(path='root', depth='2', codeblock='false')}\n");

        // Directories before files, each alphabetical.
        int adir = out.indexOf("adir");
        int zdir = out.indexOf("zdir");
        int aFile = out.indexOf("a.txt");
        int mFile = out.indexOf("m.txt");
        assertTrue(adir > 0 && zdir > 0 && aFile > 0 && mFile > 0, out);
        assertTrue(adir < zdir, "adir before zdir: " + out);
        assertTrue(zdir < aFile, "directories before files: " + out);
        assertTrue(aFile < mFile, "a.txt before m.txt: " + out);
    }

    @Test
    public void testDepthLimitsRecursion() throws IOException {
        touch("root/a.txt", "root/sub/b.txt", "root/sub/sub2/c.txt");
        String out = render("${widget:fileTree(path='root', depth='1', codeblock='false')}\n");

        assertTrue(out.contains("a.txt"), out);
        assertTrue(out.contains("sub"), out);
        assertFalse(out.contains("b.txt"), "depth=1 must not list contents of sub: " + out);
        assertFalse(out.contains("c.txt"), out);
    }

    @Test
    public void testDepthTwoShowsTwoLevels() throws IOException {
        touch("root/a.txt", "root/sub/b.txt", "root/sub/sub2/c.txt");
        String out = render("${widget:fileTree(path='root', depth='2', codeblock='false')}\n");

        assertTrue(out.contains("a.txt"), out);
        assertTrue(out.contains("b.txt"), out);
        assertFalse(out.contains("c.txt"), "depth=2 must not list contents of sub2: " + out);
    }

    @Test
    public void testExcludeByName() throws IOException {
        touch("root/a.txt", "root/b.class", "root/c.txt");
        String out = render("${widget:fileTree(path='root', depth='1', exclude='*.class', codeblock='false')}\n");

        assertTrue(out.contains("a.txt"), out);
        assertTrue(out.contains("c.txt"), out);
        assertFalse(out.contains("b.class"), out);
    }

    @Test
    public void testExcludeMultipleGlobs() throws IOException {
        mkdirs("root/target", "root/.idea", "root/src");
        touch("root/target/x.jar", "root/.idea/x.xml", "root/src/x.java", "root/.git/HEAD");
        String out = render("${widget:fileTree(path='root', depth='2', exclude='target,.idea,.git', codeblock='false')}\n");

        assertTrue(out.contains("src"), out);
        assertFalse(out.contains("target"), out);
        assertFalse(out.contains(".idea"), out);
        assertFalse(out.contains(".git"), out);
    }

    @Test
    public void testDirsOnlySuppressesFiles() throws IOException {
        mkdirs("root/sub1", "root/sub2");
        touch("root/a.txt", "root/sub1/b.txt");
        String out = render("${widget:fileTree(path='root', depth='2', dirsOnly='true', codeblock='false')}\n");

        assertTrue(out.contains("sub1"), out);
        assertTrue(out.contains("sub2"), out);
        assertFalse(out.contains("a.txt"), out);
        assertFalse(out.contains("b.txt"), out);
    }

    @Test
    public void testCodeblockTrueByDefaultWraps() throws IOException {
        touch("root/a.txt");
        String out = render("${widget:fileTree(path='root', depth='1')}\n");

        String ls = System.lineSeparator();
        assertTrue(out.contains("```" + ls), "expected code fence: " + out);
        assertTrue(out.contains("a.txt"), out);
    }

    @Test
    public void testMissingPathLogsErrorAndEmitsNothing() {
        String out = render("before\n${widget:fileTree(path='nonexistent', depth='1')}\nafter\n");

        assertTrue(out.contains("before"));
        assertTrue(out.contains("after"));
        assertFalse(out.contains("├──"), out);
        assertTrue(getErrAndClear().contains("fileTree widget"));
    }

    @Test
    public void testMissingPathParameterLogsError() {
        render("${widget:fileTree(depth='1')}\n");
        assertTrue(getErrAndClear().contains("missing required 'path'"));
    }

    @Test
    public void testRelativePathResolvedAgainstSourceFile() throws IOException {
        // tempDir/README.src.md resolves "child/" to tempDir/child
        touch("child/file.md");
        String out = render("${widget:fileTree(path='child', depth='1', codeblock='false')}\n");
        assertTrue(out.contains("file.md"), out);
    }

    @Test
    public void testInvalidDepthLogsError() throws IOException {
        touch("root/a.txt");
        render("${widget:fileTree(path='root', depth='abc')}\n");
        assertTrue(getErrAndClear().contains("invalid 'depth'"));
    }

    @Test
    public void testPathPatternExcludesByRelativePath() throws IOException {
        touch("root/sub/keep.txt", "root/sub/drop.txt");
        String out = render("${widget:fileTree(path='root', depth='2', exclude='sub/drop.txt', codeblock='false')}\n");

        assertTrue(out.contains("keep.txt"), out);
        assertFalse(out.contains("drop.txt"), out);
    }
}
