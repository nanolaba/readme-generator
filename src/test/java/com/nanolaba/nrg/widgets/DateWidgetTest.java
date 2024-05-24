package com.nanolaba.nrg.widgets;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.core.Generator;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateWidgetTest {

    @Test
    public void testDateWidget() {
        Generator generator = new Generator(new File("README.src.md"),
                "${widget:date}"
        );

        String body = generator.getResult("en").getContent().toString();
        LOG.info(body);
        assertFalse(body.contains("${widget:date}"));
        assertTrue(body.contains(new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date())));

    }

    @Test
    public void testDateWidget1() {
        Generator generator = new Generator(new File("README.src.md"),
                "${widget:date(pattern='yyyy-MM-dd')}"
        );

        String body = generator.getResult("en").getContent().toString();
        LOG.info(body);
        assertFalse(body.contains("${widget:date"));
        assertTrue(body.contains(new SimpleDateFormat("yyyy-MM-dd").format(new Date())));

    }
}