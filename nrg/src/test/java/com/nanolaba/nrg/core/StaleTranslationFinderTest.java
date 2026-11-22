package com.nanolaba.nrg.core;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
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
        assertTrue(StaleTranslationFinder.find(FILE, "a<!--en-->\n", "b<!--en-->\n", null).isEmpty());
    }

    @Test
    public void testAddedAndDeletedFilesHaveNothingStale() {
        String body = "One<!--en-->\nОдин<!--ru-->\n";
        assertTrue(find("", body).isEmpty(), "every language of a new file is added");
        assertTrue(find(null, body).isEmpty());
        assertTrue(find(body, "").isEmpty(), "nothing is left of a deleted file");
        assertTrue(find(body, null).isEmpty());
    }

    @Test
    public void testLastLineWithoutTrailingNewline() {
        List<StaleTranslationFinder.Finding> f = find("One<!--en-->\nОдин<!--ru-->", "One<!--en-->\nОдин!<!--ru-->");

        assertEquals(1, f.size());
        assertEquals(FILE, f.get(0).getFile());
        assertEquals(2, f.get(0).getLastLine());
        assertEquals(Collections.singleton("ru"), f.get(0).getChanged());
        assertEquals("Один", f.get(0).getOldText().get("ru"));
        assertEquals("Один!", f.get(0).getNewText().get("ru"));
    }

    @Test
    public void testRemovedLineRightBeforeAGroupBelongsToIt() {
        List<StaleTranslationFinder.Finding> removed = find(
                "Old<!--en-->\nOne<!--en-->\nОдин<!--ru-->\n",
                "One<!--en-->\nОдин<!--ru-->\n");
        assertEquals(1, removed.size());
        assertEquals(Collections.singleton("en"), removed.get(0).getChanged());
        assertEquals(Collections.singleton("ru"), removed.get(0).getUnchanged());
        assertEquals("Old", removed.get(0).getOldText().get("en"));
        assertEquals("", removed.get(0).getNewText().get("en"));

        List<StaleTranslationFinder.Finding> replaced = find(
                "Intro\nOld<!--en-->\nOne<!--en-->\nОдин<!--ru-->\n",
                "Intro\nShared text\nOne<!--en-->\nОдин<!--ru-->\n");
        assertEquals(1, replaced.size(), "a tagged line replaced by a shared one sits after its replacement");
        assertEquals(3, replaced.get(0).getFirstLine());
        assertEquals("Old", replaced.get(0).getOldText().get("en"));
    }

    @Test
    public void testRepeatedLineAppendedOrRemovedAtTheEnd() {
        List<StaleTranslationFinder.Finding> appended = find(
                "One<!--en-->\nОдин<!--ru-->\n",
                "One<!--en-->\nОдин<!--ru-->\nОдин<!--ru-->\n");
        assertEquals(1, appended.size());
        assertEquals(3, appended.get(0).getLastLine());
        assertEquals(Collections.singleton("ru"), appended.get(0).getChanged());
        assertEquals("Один", appended.get(0).getNewText().get("ru"));

        List<StaleTranslationFinder.Finding> dropped = find(
                "One<!--en-->\nОдин<!--ru-->\nОдин<!--ru-->\n",
                "One<!--en-->\nОдин<!--ru-->\n");
        assertEquals(1, dropped.size());
        assertEquals(Collections.singleton("ru"), dropped.get(0).getChanged());
        assertEquals("Один", dropped.get(0).getOldText().get("ru"));
    }

    @Test
    public void testUnchangedGroupBetweenEditedSharedLines() {
        assertTrue(find("Alpha<!--en-->\nАльфа<!--ru-->\nend one\n",
                        "Intro\nAlpha<!--en-->\nАльфа<!--ru-->\nend two\n").isEmpty(),
                "a line added before the group");
        assertTrue(find("Intro\nAlpha<!--en-->\nАльфа<!--ru-->\nend one\n",
                        "Alpha<!--en-->\nАльфа<!--ru-->\nend two\n").isEmpty(),
                "a line removed before the group");
        assertTrue(find("Intro\nAlpha<!--en-->\nold note\nАльфа<!--ru-->\n",
                        "Alpha<!--en-->\n\nАльфа<!--ru-->\n").isEmpty(),
                "a line removed before the group and a note inside it blanked");
    }

    @Test
    public void testEditsArePairedWithinTheirOwnHunk() {
        List<StaleTranslationFinder.Finding> f = find(
                "Intro one\nShared\n${en:'Title', ru:'Заголовок'}\nEnd\n",
                "Intro two\nShared\n${en:'New title', ru:'Заголовок'}\nEnd\n");
        assertEquals(1, f.size());
        assertEquals("construct", f.get(0).getKind());
        assertEquals(3, f.get(0).getFirstLine());
        assertEquals("Title", f.get(0).getOldText().get("en"));

        assertTrue(find("${en:'Old', ru:'Старый'}\nShared\nEnd\n",
                        "Shared\n${en:'New', ru:'Старый'}\nEnd\n").isEmpty(),
                "a construct moved past an unchanged line is a removal and an addition, not an edit");
    }

    @Test
    public void testConstructAtTheStartOfALine() {
        List<StaleTranslationFinder.Finding> f = find(
                "${en:'Title', ru:'Заголовок'}\n",
                "${en:'New title', ru:'Заголовок'}\n");
        assertEquals(1, f.size());
        assertEquals("construct", f.get(0).getKind());
        assertEquals(1, f.get(0).getFirstLine());
    }

    @Test
    public void testEscapedConstructIsNotATranslation() {
        assertTrue(find("\\${en:'Title', ru:'Заголовок'}\n", "\\${en:'New title', ru:'Заголовок'}\n").isEmpty());

        List<StaleTranslationFinder.Finding> f = find(
                "\\${x} ${en:'Title', ru:'Заголовок'}\n",
                "\\${x} ${en:'New title', ru:'Заголовок'}\n");
        assertEquals(1, f.size(), "a real construct after an escaped one still counts");
    }

    @Test
    public void testFindingsAreSortedByLine() {
        List<StaleTranslationFinder.Finding> f = find(
                "# ${en:'Title', ru:'Заголовок'}\n\nFast<!--en-->\nБыстро<!--ru-->\n",
                "# ${en:'New title', ru:'Заголовок'}\n\nFaster<!--en-->\nБыстро<!--ru-->\n");

        assertEquals(2, f.size());
        assertEquals("construct", f.get(0).getKind());
        assertEquals(1, f.get(0).getFirstLine());
        assertEquals("block", f.get(1).getKind());
        assertEquals(3, f.get(1).getFirstLine());
    }
}
