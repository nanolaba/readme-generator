package com.nanolaba.nrg.widgets;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.core.Generator;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguagesWidgetTest {

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
}