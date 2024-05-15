package com.nanolaba.nrg.core;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class GeneratorConfigTest {

    @Test
    public void testGetWidget() {
        GeneratorConfig config = new GeneratorConfig(new File("README.src.md"), "");

        assertNull(config.getWidget(null));
        assertNull(config.getWidget(""));
        assertNull(config.getWidget("123"));

        assertNotNull(config.getWidget("languages"));
    }

}