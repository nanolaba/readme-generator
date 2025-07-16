package com.nanolaba.nrg.examples;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.DefaultNRGTest;
import com.nanolaba.nrg.NRG;
import com.nanolaba.nrg.core.GenerationResult;
import com.nanolaba.nrg.core.Generator;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class ExampleWidgetTest extends DefaultNRGTest {

    @Test
    public void testLaunch() throws IOException {

        NRG.addWidget(new ExampleWidget());
        NRG.main("--charset", "UTF-8", "-f", "./target/test-classes/WidgetExampleTest.src.md");

        String result = FileUtils.readFileToString(new File("./target/test-classes/WidgetExampleTest.md"), StandardCharsets.UTF_8);

        LOG.info("Result: {}", result);

        assertNotNull(result);
        assertTrue(result.contains("Hello, World!"));
    }

    @Test
    public void testGenerator() {
        Generator generator = new Generator(new File("README.src.md"),
                "${widget:exampleWidget(name='World')}",
                Collections.singletonList(new ExampleWidget()));

        Collection<GenerationResult> results = generator.getResults();
        assertNotNull(results);
        assertEquals(1, results.size());

        String result = CollectionUtils.getFirstElement(results).get().getContent().toString();
        assertTrue(result.contains("Hello, World!"));
    }
}