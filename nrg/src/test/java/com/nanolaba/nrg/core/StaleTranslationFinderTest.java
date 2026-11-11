package com.nanolaba.nrg.core;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link StaleTranslationFinder}: one-sided edits of translation groups and of
 * {@code ${en:'…', ru:'…'}} constructs between two versions of a template.
 */
class StaleTranslationFinderTest {

    private static final List<String> EN_RU = Arrays.asList("en", "ru");
    private static final File FILE = new File("README.src.md");

    private static List<StaleTranslationFinder.Finding> find(String before, String after) {
        return StaleTranslationFinder.find(FILE, before, after, EN_RU);
    }

    @Test
    public void testOneSidedEditOfAGroupIsReported() {
        List<StaleTranslationFinder.Finding> f = find(
                "Intro\n\nFast<!--en-->\nБыстро<!--ru-->\n\nShared\n",
                "Intro\n\nFast and safe<!--en-->\nБыстро<!--ru-->\n\nShared\n");

        assertEquals(1, f.size());
        assertEquals("block", f.get(0).getKind());
        assertEquals(3, f.get(0).getFirstLine());
        assertEquals(4, f.get(0).getLastLine());
        assertEquals(Arrays.asList("en"), Arrays.asList(f.get(0).getChanged().toArray()));
        assertEquals(Arrays.asList("ru"), Arrays.asList(f.get(0).getUnchanged().toArray()));
        assertEquals("Fast", f.get(0).getOldText().get("en"));
        assertEquals("Fast and safe", f.get(0).getNewText().get("en"));
    }

    @Test
    public void testEditingBothLanguagesIsFine() {
        assertTrue(find("Fast<!--en-->\nБыстро<!--ru-->\n", "Faster<!--en-->\nБыстрее<!--ru-->\n").isEmpty());
    }

    @Test
    public void testAddingAndRemovingLinesCount() {
        List<StaleTranslationFinder.Finding> added = find(
                "One<!--en-->\nОдин<!--ru-->\n",
                "One<!--en-->\nTwo<!--en-->\nОдин<!--ru-->\n");
        assertEquals(1, added.size(), "an added English line without a Russian one");
        assertEquals("Two", added.get(0).getNewText().get("en"));

        List<StaleTranslationFinder.Finding> removed = find(
                "One<!--en-->\nОдин<!--ru-->\nДва<!--ru-->\n",
                "One<!--en-->\nОдин<!--ru-->\n");
        assertEquals(1, removed.size(), "a removed Russian line");
        assertEquals(Arrays.asList("ru"), Arrays.asList(removed.get(0).getChanged().toArray()));
        assertEquals("Два", removed.get(0).getOldText().get("ru"));
    }

    @Test
    public void testUnrelatedChangesAreIgnored() {
        assertTrue(find("Shared\nOne<!--en-->\nОдин<!--ru-->\n", "Shared, edited\n\nOne<!--en-->\nОдин<!--ru-->\n").isEmpty());
    }

    @Test
    public void testAcknowledgedEditIsSilent() {
        assertTrue(find("Fast<!--en-->\nБыстро<!--ru-->\n",
                "Fats fixed<!--en--><!--nrg.translation.ok-->\nБыстро<!--ru-->\n").isEmpty());
    }

    @Test
    public void testOneSidedConstructEdit() {
        List<StaleTranslationFinder.Finding> f = find(
                "# ${en:'Title', ru:'Заголовок'}\n",
                "# ${en:'New title', ru:'Заголовок'}\n");

        assertEquals(1, f.size());
        assertEquals("construct", f.get(0).getKind());
        assertEquals("Title", f.get(0).getOldText().get("en"));
        assertEquals("New title", f.get(0).getNewText().get("en"));
        assertTrue(find("# ${en:'Title', ru:'Заголовок'}\n", "# ${en:'New', ru:'Новый'}\n").isEmpty());
        assertTrue(find("# ${en:'Title', ru:'Заголовок'} text\n", "# ${en:'Title', ru:'Заголовок'} more text\n").isEmpty(),
                "text around the construct is not a translation");
    }

    @Test
    public void testSingleLanguageTemplateHasNothingToCompare() {
        assertTrue(StaleTranslationFinder.find(FILE, "a<!--en-->\n", "b<!--en-->\n", Arrays.asList("en")).isEmpty());
    }
}
