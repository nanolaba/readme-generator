package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.DefaultNRGTest;
import com.nanolaba.nrg.core.Generator;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class LanguagesWidgetRelativeLinkTest extends DefaultNRGTest {

    @Test
    public void siblingFilesProduceBareName() {
        String body = "<!--@nrg.languages=en,ru-->\n" +
                "<!--@nrg.defaultLanguage=en-->\n" +
                "${widget:languages}\n";
        Generator g = new Generator(new File("repo/README.src.md"), body);
        String en = g.getResult("en").getContent().toString();
        String ru = g.getResult("ru").getContent().toString();
        assertTrue(en.contains("[ru](README.ru.md)"), en);
        assertTrue(ru.contains("[en](README.md)"), ru);
    }

    @Test
    public void crossDirectoryLayoutEmitsRelativePath() {
        String body = "<!--@nrg.languages=en,ru-->\n" +
                "<!--@nrg.defaultLanguage=en-->\n" +
                "<!--@nrg.fileNamePattern=docs/<lang>/<base>.md-->\n" +
                "${widget:languages}\n";
        Generator g = new Generator(new File("repo/README.src.md"), body);
        String en = g.getResult("en").getContent().toString();
        String ru = g.getResult("ru").getContent().toString();
        assertTrue(en.contains("[ru](../ru/README.md)"),
                "en output should link to ru via ../ru/README.md, got: " + en);
        assertTrue(ru.contains("[en](../en/README.md)"),
                "ru output should link to en via ../en/README.md, got: " + ru);
    }

    @Test
    public void perLanguageMixedLayoutEmitsRelativePath() {
        String body = "<!--@nrg.languages=en,ru-->\n" +
                "<!--@nrg.defaultLanguage=en-->\n" +
                "<!--@nrg.fileNamePattern.ru=ru/README.md-->\n" +
                "${widget:languages}\n";
        Generator g = new Generator(new File("repo/README.src.md"), body);
        String en = g.getResult("en").getContent().toString();
        String ru = g.getResult("ru").getContent().toString();
        assertTrue(en.contains("[ru](ru/README.md)"), en);
        assertTrue(ru.contains("[en](../README.md)"), ru);
    }
}
