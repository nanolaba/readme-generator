package com.nanolaba.nrg.core;

import com.nanolaba.nrg.DefaultNRGTest;
import com.nanolaba.nrg.widgets.NRGWidget;
import com.nanolaba.nrg.widgets.WidgetTag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WidgetPrecedenceTest extends DefaultNRGTest {

    @Test
    public void testCliOverridesTemplateOnNameCollision() {
        String src = "<!--@nrg.languages=en-->\n" +
                "<!--@nrg.widgets=" + TemplateFooWidget.class.getName() + "-->\n" +
                "value: ${widget:foo}\n";

        Generator generator = new Generator(
                new File("README.src.md"), src,
                Collections.singletonList(new CliFooWidget()));

        String body = generator.getResult("en").getContent().toString();
        assertTrue(body.contains("value: CLI"), "CLI widget should win, got: " + body);
        assertFalse(body.contains("TEMPLATE"), "Template widget must be shadowed by CLI");
    }

    @Test
    public void testApiOverridesCliAndTemplate() {
        String src = "<!--@nrg.languages=en-->\n" +
                "<!--@nrg.widgets=" + TemplateFooWidget.class.getName() + "-->\n" +
                "value: ${widget:foo}\n";

        GeneratorConfig config = new GeneratorConfig(new File("README.src.md"), src, null);
        config.getWidgets().add(new ApiFooWidget());

        NRGWidget resolved = config.getWidget("foo");
        assertTrue(resolved instanceof ApiFooWidget);
    }

    public static class TemplateFooWidget implements NRGWidget {
        @Override
        public String getName() {
            return "foo";
        }

        @Override
        public String getBody(WidgetTag t, GeneratorConfig c, String l) {
            return "TEMPLATE";
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public void setEnabled(boolean e) {/**/}
    }

    public static class CliFooWidget implements NRGWidget {
        @Override
        public String getName() {
            return "foo";
        }

        @Override
        public String getBody(WidgetTag t, GeneratorConfig c, String l) {
            return "CLI";
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public void setEnabled(boolean e) {/**/}
    }

    public static class ApiFooWidget implements NRGWidget {
        @Override
        public String getName() {
            return "foo";
        }

        @Override
        public String getBody(WidgetTag t, GeneratorConfig c, String l) {
            return "API";
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public void setEnabled(boolean e) {/**/}
    }
}
