package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.core.TemplateLine;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/** Unit tests of {@link WidgetTag}: the accessors return what the tag was created with. */
class WidgetTagTest {

    @Test
    public void testExposesWhatItWasCreatedWith() {
        GeneratorConfig config = new GeneratorConfig(new File("README.src.md"), "", null);
        TemplateLine line = new TemplateLine(config, "a ${widget:x(p='1')} b", 1);

        WidgetTag tag = new WidgetTag(line, "x", "p='1'", 2, 19);

        assertSame(line, tag.getLine());
        assertEquals("x", tag.getName());
        assertEquals("p='1'", tag.getParameters());
        assertEquals(2, tag.getStart());
        assertEquals(19, tag.getEnd());
    }
}
