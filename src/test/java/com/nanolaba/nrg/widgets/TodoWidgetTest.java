package com.nanolaba.nrg.widgets;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.core.Generator;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TodoWidgetTest {

    @Test
    public void testWidget() {
        Generator generator = new Generator(new File("README.src.md"),
                "${widget:todo}"
        );

        String body = generator.getResult("en").getContent().toString();
        LOG.info(body);
        assertFalse(body.contains("${widget:todo}"));
        assertTrue(body.contains("<pre>\uD83D\uDCCC ⌛ Not done yet...</pre>"), body);
    }

    @Test
    public void testWidgetWithParameters() {
        Generator generator = new Generator(new File("README.src.md"),
                "${widget:todo(text='Some text')}"
        );

        String body = generator.getResult("en").getContent().toString();
        LOG.info(body);
        assertFalse(body.contains("${widget:todo}"));
        assertTrue(body.contains("<pre>\uD83D\uDCCC ⌛ Some text</pre>"), body);
    }
}