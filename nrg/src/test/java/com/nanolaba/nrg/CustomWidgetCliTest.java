package com.nanolaba.nrg;

import com.nanolaba.nrg.core.Generator;
import com.nanolaba.nrg.examples.ExampleWidget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomWidgetCliTest extends DefaultNRGTest {

    @TempDir
    Path tempDir;

    @Test
    public void testWidgetRegisteredViaTemplateProperty() {
        String src = "<!--@nrg.languages=en-->\n" +
                "<!--@nrg.widgets=" + ExampleWidget.class.getName() + "-->\n" +
                "Hi ${widget:exampleWidget(name=\"Alice\")}!\n";

        Generator generator = new Generator(new File("README.src.md"), src);
        String body = generator.getResult("en").getContent().toString();

        assertTrue(body.contains("Hi Hello, Alice!!"), "Expected widget output, got: " + body);
    }

    @Test
    public void testWidgetsClassNotFoundLogsError() {
        String src = "<!--@nrg.languages=en-->\n" +
                "<!--@nrg.widgets=com.does.not.Exist-->\n" +
                "plain\n";

        new Generator(new File("README.src.md"), src).getResult("en");
        String err = getErrAndClear();
        assertTrue(err.contains("Widget class not found: 'com.does.not.Exist'"));
    }

    @Test
    public void testCliWidgetsFlagRegistersWidget() throws IOException {
        Path src = tempDir.resolve("README.src.md");
        Files.write(src,
                ("<!--@nrg.languages=en-->\n" +
                        "Hi ${widget:exampleWidget(name=\"Bob\")}!\n").getBytes(StandardCharsets.UTF_8));

        NRG.main("--stdout", "--widgets", ExampleWidget.class.getName(), "-f", src.toString());

        String out = getOutAndClear();
        assertTrue(out.contains("Hi Hello, Bob!!"), "Expected widget output, got: " + out);
    }

    @Test
    public void testCliWidgetsFlagUnknownClassLogsError() throws IOException {
        Path src = tempDir.resolve("README.src.md");
        Files.write(src, "<!--@nrg.languages=en-->\nplain\n".getBytes(StandardCharsets.UTF_8));

        NRG.main("--stdout", "--widgets", "com.does.not.Exist", "-f", src.toString());

        String err = getErrAndClear();
        assertTrue(err.contains("Widget class not found: 'com.does.not.Exist'"));
    }

    @Test
    public void testCliClasspathEntryNotExistingLogsWarning() throws IOException {
        Path src = tempDir.resolve("README.src.md");
        Files.write(src, "<!--@nrg.languages=en-->\nplain\n".getBytes(StandardCharsets.UTF_8));

        NRG.main("--stdout", "--classpath", tempDir.resolve("missing-dir").toString(),
                "-f", src.toString());

        String combined = getOutAndClear() + getErrAndClear();
        assertTrue(combined.contains("--classpath entry does not exist"));
    }
}
