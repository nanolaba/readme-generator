package com.nanolaba.nrg.core;

import com.nanolaba.nrg.DefaultNRGTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Translation-parity rules of {@code --validate}: one-language groups, incomplete
 * {@code ${en:'…', ru:'…'}} constructs and diverging skeletons, plus the
 * {@code <!--nrg.parity.ignore-->} / {@code nrg.validate.parity=false} escape hatches.
 */
class TranslationParityCheckerTest extends DefaultNRGTest {

    @TempDir
    Path tempDir;

    private List<Validator.Diagnostic> validate(String body) throws IOException {
        return validate("README.src.md", body);
    }

    private List<Validator.Diagnostic> validate(String name, String body) throws IOException {
        Path src = tempDir.resolve(name);
        Files.write(src, body.getBytes(StandardCharsets.UTF_8));
        return new Validator(src.toFile()).validate();
    }

    private static final String HEAD = "<!--@nrg.languages=en,ru-->\n";

    @Test
    public void testParallelTranslationsPass() throws IOException {
        List<Validator.Diagnostic> d = validate(HEAD +
                "## Title<!--en-->\n## Заголовок<!--ru-->\n\n" +
                "First line<!--en-->\nsecond line.<!--en-->\nОдна строка.<!--ru-->\n\n" +
                "shared text\n\n" +
                "- a<!--en-->\n- b<!--en-->\n\n- а<!--ru-->\n- б<!--ru-->\n" +
                "Title ${en:'Hello', ru:'Привет'} ${en:'', ru:''}\n");

        assertTrue(d.isEmpty(), d.toString());
    }

    @Test
    public void testGroupWithoutSecondLanguageIsAnError() throws IOException {
        List<Validator.Diagnostic> d = validate(HEAD +
                "shared\n" +
                "New paragraph<!--en-->\nthat continues.<!--en-->\n" +
                "shared again\n");

        assertEquals(1, d.size(), d.toString());
        assertTrue(d.get(0).isError());
        assertEquals(3, d.get(0).getLine());
        assertTrue(d.get(0).getMessage().startsWith("lines 3-4 exist for 'en' but not for 'ru'"), d.get(0).getMessage());
        assertTrue(d.get(0).getMessage().contains("<!--nrg.parity.ignore-->"), d.get(0).getMessage());
    }

    @Test
    public void testBlankLinesDoNotEndAGroup() throws IOException {
        // EN heading and RU heading are separated by blank lines and EN bullets: still one group.
        List<Validator.Diagnostic> d = validate(HEAD +
                "## Features<!--en-->\n\n- one<!--en-->\n\n## Возможности<!--ru-->\n\n- один<!--ru-->\n");

        assertTrue(d.isEmpty(), d.toString());
    }

    @Test
    public void testDivergingSkeletonIsAWarning() throws IOException {
        List<Validator.Diagnostic> d = validate(HEAD +
                "- one<!--en-->\n- two<!--en-->\n- один<!--ru-->\n");

        assertEquals(1, d.size(), d.toString());
        assertTrue(d.get(0).isWarning());
        assertEquals(3, d.get(0).getLine());
        assertTrue(d.get(0).getMessage().contains("'en' has a list item (line 3) where 'ru' has nothing more"),
                d.get(0).getMessage());
    }

    @Test
    public void testHeadingLevelMismatchIsReported() throws IOException {
        List<Validator.Diagnostic> d = validate(HEAD + "## Title<!--en-->\n### Заголовок<!--ru-->\n");

        assertEquals(1, d.size(), d.toString());
        assertTrue(d.get(0).getMessage().contains("level-2 heading (line 2) where 'ru' has a level-3 heading (line 3)"),
                d.get(0).getMessage());
    }

    @Test
    public void testCodeInsideTaggedFenceIsComparedByFencesOnly() throws IOException {
        List<Validator.Diagnostic> d = validate(HEAD +
                "```<!--en-->\n# comment<!--en-->\n- x<!--en-->\n```<!--en-->\n" +
                "```<!--ru-->\n# комментарий<!--ru-->\n```<!--ru-->\n");

        assertTrue(d.isEmpty(), d.toString());
    }

    @Test
    public void testIncompleteLanguageConstructIsAnError() throws IOException {
        List<Validator.Diagnostic> d = validate(HEAD + "Title ${en:'Hello'}\n");

        assertEquals(1, d.size(), d.toString());
        assertTrue(d.get(0).isError());
        assertTrue(d.get(0).getMessage().contains("no text for 'ru'"), d.get(0).getMessage());
    }

    @Test
    public void testConstructOnTaggedLineOnlyNeedsThatLanguage() throws IOException {
        List<Validator.Diagnostic> d = validate(HEAD + "A ${en:'x'}<!--en-->\nБ<!--ru-->\n");

        assertTrue(d.isEmpty(), d.toString());
    }

    @Test
    public void testUndeclaredLanguageInConstructIsAnError() throws IOException {
        List<Validator.Diagnostic> d = validate(HEAD + "Title ${en:'a', ru:'б', de:'c'}\n");

        assertEquals(1, d.size(), d.toString());
        assertTrue(d.get(0).getMessage().contains("names 'de', which is not in nrg.languages"), d.get(0).getMessage());
    }

    @Test
    public void testEscapedAndNonConstructReferencesAreIgnored() throws IOException {
        List<Validator.Diagnostic> d = validate(HEAD +
                "<!--@x=1-->\nUse \\${en:'only'} ${x} ${env.HOME:none} ${widget:date(pattern='yyyy')}\n");

        assertTrue(d.isEmpty(), d.toString());
    }

    @Test
    public void testParityIgnoreMarkerAndIgnoreBlocksAreNeutral() throws IOException {
        List<Validator.Diagnostic> d = validate(HEAD +
                "English-only note<!--en--><!--nrg.parity.ignore-->\n" +
                "<!--nrg.ignore.begin-->\nDraft<!--en-->\n<!--nrg.ignore.end-->\n" +
                "Dropped<!--en--><!--nrg.ignore-->\n");

        assertTrue(d.isEmpty(), d.toString());
    }

    @Test
    public void testParityCanBeTurnedOffByProperty() throws IOException {
        List<Validator.Diagnostic> d = validate(HEAD +
                "<!--@nrg.validate.parity=false-->\nOnly English<!--en-->\n${en:'x'}\n");

        assertTrue(d.isEmpty(), d.toString());
    }

    @Test
    public void testSingleLanguageTemplateIsNotChecked() throws IOException {
        assertTrue(validate("<!--@nrg.languages=en-->\nText<!--en-->\n").isEmpty());
    }

    @Test
    public void testImportedFragmentIsCheckedWithInheritedLanguages() throws IOException {
        Files.write(tempDir.resolve("part.src.md"), "Only English<!--en-->\n".getBytes(StandardCharsets.UTF_8));
        List<Validator.Diagnostic> d = validate(HEAD + "${widget:import(path='part.src.md')}\n");

        assertEquals(1, d.size(), d.toString());
        assertEquals("part.src.md", d.get(0).getFile().getName());
    }

    @Test
    public void testParityIgnoreMarkerIsStrippedFromOutput() {
        Generator generator = new Generator(new File("README.src.md"),
                HEAD + "Note<!--en--><!--nrg.parity.ignore-->\n");

        String en = generator.getResult("en").getContent().toString();
        assertTrue(en.contains("Note" + RN), en);
        assertFalse(en.contains("parity"), en);
    }

    @Test
    public void testParseConstructKeys() {
        assertEquals(Arrays.asList("en", "ru"),
                TranslationParityChecker.parseConstructKeys("${en:'it''s', ru : \"a\"\"b\"}", 2));
        assertNull(TranslationParityChecker.parseConstructKeys("${widget:toc}", 2));
        assertNull(TranslationParityChecker.parseConstructKeys("${en:'unterminated}", 2));
        assertNull(TranslationParityChecker.parseConstructKeys("${env.HOME:x}", 2));
    }
}
