package com.nanolaba.nrg.widgets;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.DefaultNRGTest;
import com.nanolaba.nrg.core.Generator;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguagesWidgetTest extends DefaultNRGTest {

    @Test
    public void testLanguagesWidget() {
        Generator generator = new Generator(new File("README.src.md"),
                "<!--@nrg.languages=en,ru,fr-->" +
                "${widget:languages}"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);
        assertTrue(bodyEn.contains("[ **en** | [ru](README.ru.md) | [fr](README.fr.md) ]"));
        assertFalse(bodyEn.contains("${widget:languages}"));

        String bodyFr = generator.getResult("fr").getContent().toString();
        LOG.info(bodyFr);
        assertTrue(bodyFr.contains("[ [en](README.md) | [ru](README.ru.md) | **fr** ]"));
        assertFalse(bodyFr.contains("${widget:languages}"));
    }

    @Test
    public void languagesWidgetInsideImportUsesRootFileName() throws IOException {
        // Regression: when ${widget:languages} is placed inside an imported fragment,
        // the rendered links must target the root document's generated files
        // (README.md / README.ru.md), not files named after the imported fragment.
        File sourceFile = new File(getClass().getClassLoader().getResource("LanguagesWidgetTest/README.src.md").getFile());
        Generator generator = new Generator(sourceFile, StandardCharsets.UTF_8);

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);
        assertTrue(bodyEn.contains("[ **en** | [ru](README.ru.md) ]"), bodyEn);
        assertFalse(bodyEn.contains("fragment.ru.md"), bodyEn);

        String bodyRu = generator.getResult("ru").getContent().toString();
        LOG.info(bodyRu);
        assertTrue(bodyRu.contains("[ [en](README.md) | **ru** ]"), bodyRu);
        assertFalse(bodyRu.contains("fragment.md"), bodyRu);
    }
}