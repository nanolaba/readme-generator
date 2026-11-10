package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.DefaultNRGTest;
import com.nanolaba.nrg.core.Generator;
import com.nanolaba.nrg.core.GeneratorConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A generator created on behalf of another one — for an imported template, or for the
 * table of contents' heading scan — has to work with what the outer one was given from
 * outside: the caller's widgets, exec permission, environment.
 */
class NestedGeneratorContextTest extends DefaultNRGTest {

    @TempDir
    Path tmp;

    /** Custom widget as a caller would register it through the API or {@code --widgets}. */
    private static final class GreetWidget extends DefaultWidget {
        @Override
        public String getName() {
            return "greet";
        }

        @Override
        public String getBody(WidgetTag widgetTag, GeneratorConfig config, String language) {
            return "Hello from greet";
        }
    }

    private static final class CountingRunner implements ExecWidget.CommandRunner {
        final List<String> commands = new ArrayList<>();

        @Override
        public ExecWidget.ExecResult run(List<String> argv, File cwd, long timeoutSeconds) {
            commands.add(String.join(" ", argv));
            return ExecWidget.ExecResult.ok("ran:" + String.join(" ", argv), "");
        }
    }

    private File write(String name, String content) throws IOException {
        Path file = tmp.resolve(name);
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        return file.toFile();
    }

    private static String render(Generator generator) {
        return generator.getResult("en").getContent().toString();
    }

    @Test
    void callerWidgetWorksInsideAnImportedTemplate() throws IOException {
        write("child.src.md", "child: ${widget:greet}\n");
        File root = write("README.src.md", "root: ${widget:greet}\n${widget:import(path='child.src.md')}\n");

        String out = render(new Generator(root, StandardCharsets.UTF_8,
                Collections.<NRGWidget>singletonList(new GreetWidget())));

        assertTrue(out.contains("root: Hello from greet"), out);
        assertTrue(out.contains("child: Hello from greet"), out);
        assertFalse(getOutAndClear().contains("Unknown widget name"));
    }

    @Test
    void headingScanKnowsTheCallerWidgets() throws IOException {
        File root = write("README.src.md", "${widget:toc}\n## Section\n${widget:greet}\n");

        String out = render(new Generator(root, StandardCharsets.UTF_8,
                Collections.<NRGWidget>singletonList(new GreetWidget())));

        assertTrue(out.contains("- [Section](#section)"), out);
        assertTrue(out.contains("Hello from greet"), out);
        assertFalse(getOutAndClear().contains("Unknown widget name"), "the scan must not warn about a registered widget");
    }

    @Test
    void execRunsInsideAnImportedLocalTemplateWhenAllowed() throws IOException {
        write("child.src.md", "child: ${widget:exec(cmd='child-cmd')}\n");
        File root = write("README.src.md", "root: ${widget:exec(cmd='root-cmd')}\n${widget:import(path='child.src.md')}\n");
        CountingRunner runner = new CountingRunner();
        Generator generator = new Generator(root, StandardCharsets.UTF_8,
                Collections.<NRGWidget>singletonList(new ExecWidget(runner)));
        generator.getConfig().setExecAllowed(true);

        String out = render(generator);

        assertTrue(out.contains("root: ran:root-cmd"), out);
        assertTrue(out.contains("child: ran:child-cmd"), out);
        assertEquals("", getErrAndClear());
    }

    @Test
    void execStaysRefusedInImportedTemplateWithoutPermission() throws IOException {
        write("child.src.md", "child: [${widget:exec(cmd='child-cmd')}]\n");
        File root = write("README.src.md", "${widget:import(path='child.src.md')}\n");
        CountingRunner runner = new CountingRunner();

        String out = render(new Generator(root, StandardCharsets.UTF_8,
                Collections.<NRGWidget>singletonList(new ExecWidget(runner))));

        assertTrue(out.contains("child: []"), out);
        assertTrue(runner.commands.isEmpty());
        assertTrue(getErrAndClear().contains("execution is disabled"));
    }

    @Test
    void headingScanDoesNotRunCommandsASecondTime() throws IOException {
        // One language, one table of contents: the command must run once, for the real pass.
        // Before, the scan pass had no exec permission and logged "execution is disabled".
        File root = write("README.src.md", "${widget:toc}\n## Section\n[${widget:exec(cmd='once')}]\n");
        CountingRunner runner = new CountingRunner();
        Generator generator = new Generator(root, StandardCharsets.UTF_8,
                Collections.<NRGWidget>singletonList(new ExecWidget(runner)));
        generator.getConfig().setExecAllowed(true);

        String out = render(generator);

        assertTrue(out.contains("[ran:once]"), out);
        assertEquals(Collections.singletonList("once"), runner.commands);
        assertEquals("", getErrAndClear());
    }

    @Test
    void headingScanUsesTheInjectedEnvironment() throws IOException {
        Map<String, String> env = new HashMap<>();
        env.put("PRODUCT", "Widgetron");
        Generator generator = new Generator(new File(tmp.toFile(), "README.src.md"),
                "${widget:toc}\n## About ${env.PRODUCT}\n", null, env::get);

        String out = render(generator);

        assertTrue(out.contains("- [About Widgetron](#about-widgetron)"), out);
        assertTrue(out.contains("## About Widgetron"), out);
    }

    @Test
    void tableOfContentsInsideAnImportedTemplateKnowsAllLanguages() throws IOException {
        // The imported file does not declare nrg.languages itself; it gets them from the root.
        write("child.src.md", "${widget:toc}\n## Child heading<!--en-->\n## Дочерний заголовок<!--ru-->\n");
        File root = write("README.src.md", "<!--@nrg.languages=en,ru-->\n${widget:import(path='child.src.md')}\n");
        Generator generator = new Generator(root, StandardCharsets.UTF_8);

        assertTrue(generator.getResult("en").getContent().toString().contains("- [Child heading](#child-heading)"));
        assertTrue(generator.getResult("ru").getContent().toString()
                .contains("- [Дочерний заголовок](#дочерний-заголовок)"));
    }

    @Test
    void callerSuppliedTableOfContentsWidgetStaysEnabledAfterItsScan() throws IOException {
        // A caller-supplied instance is shared with the scan generator, which switches table
        // of contents widgets off; it has to be switched back on for the next line and language.
        File root = write("README.src.md", "<!--@nrg.languages=en,ru-->\n${widget:toc}\n## A\n\n${widget:toc}\n## B\n");
        TableOfContentsWidget custom = new TableOfContentsWidget();
        Generator generator = new Generator(root, StandardCharsets.UTF_8, Arrays.<NRGWidget>asList(custom));

        for (String language : new String[]{"en", "ru"}) {
            String out = generator.getResult(language).getContent().toString();
            assertEquals(2, org.apache.commons.lang3.StringUtils.countMatches(out, "- [B](#b)"), out);
        }
        assertTrue(custom.isEnabled());
    }
}
