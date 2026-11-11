package com.nanolaba.nrg.core;

import com.nanolaba.nrg.NRG;
import com.nanolaba.nrg.core.freeze.FreezeBlockProcessor;
import com.nanolaba.nrg.widgets.NRGWidget;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orchestrates the generation pipeline for a single source template.
 *
 * <p>Holds a {@link GeneratorConfig} (parsed from the source body) and lazily produces one
 * {@link GenerationResult} per language declared via {@code nrg.languages}. Results are
 * computed on first access via {@link #getResult(String)} / {@link #getResults()} and cached
 * for the lifetime of the instance.
 *
 * <p>Each result is a head comment plus the rendered body — {@link BlockDispatcher} runs each
 * registered {@code BlockWidget}'s pre-pass first (paired-marker rewriting, false-branch
 * pruning for {@code if}, etc.), then every remaining line is wrapped in a {@link TemplateLine}
 * for visibility check, property/widget substitution, and final output assembly.
 */
public class Generator {

    /** Same shapes FreezeBlockParser accepts; used to keep the source map aligned across frozen regions. */
    private static final Pattern FREEZE_OPEN = Pattern.compile("^\\s*<!--\\s*nrg\\.freeze(\\s+[^>]*?)?\\s*-->\\s*$");
    private static final Pattern FREEZE_CLOSE = Pattern.compile("^\\s*<!--\\s*/nrg\\.freeze\\s*-->\\s*$");

    private final GeneratorConfig config;
    private Map<String, GenerationResult> results;

    public Generator(File sourceFile, Charset charset) throws IOException {
        this(sourceFile, charset, null);
    }

    public Generator(File sourceFile, Charset charset, List<NRGWidget> widgets) throws IOException {
        this(sourceFile, FileUtils.readFileToString(sourceFile, charset), widgets);
    }

    public Generator(File sourceFile, String sourceBody) {
        this(sourceFile, sourceBody, null);
    }

    public Generator(File sourceFile, String sourceBody, List<NRGWidget> widgets) {
        config = new GeneratorConfig(sourceFile, sourceBody, widgets);
    }

    public Generator(File sourceFile, String sourceBody, List<NRGWidget> widgets,
                     Function<String, String> envProvider) {
        config = new GeneratorConfig(sourceFile, sourceBody, widgets, envProvider);
    }

    public Generator(File sourceFile, String sourceBody, List<NRGWidget> widgets,
                     Function<String, String> envProvider, PomReader pomReader) {
        config = new GeneratorConfig(sourceFile, sourceBody, widgets, envProvider, pomReader);
    }

    public Generator(File sourceFile, String sourceBody, List<NRGWidget> widgets,
                     Function<String, String> envProvider, PomReader pomReader,
                     NpmReader npmReader, GradleReader gradleReader) {
        config = new GeneratorConfig(sourceFile, sourceBody, widgets,
                envProvider, pomReader, npmReader, gradleReader);
    }

    protected void generateContents() {
        for (String language : config.getLanguages()) {
            results.put(language, generateContent(language));
        }
    }

    protected void generateContentIfNecessary() {
        if (results == null) {
            results = new HashMap<>();
            generateContents();
        }
    }

    private GenerationResult generateContent(String language) {
        GenerationResult result = new GenerationResult(language);
        String head = generateHeadComment(language);
        result.getContent().append(head);

        // With collectSourceMap on, one origin per output line is recorded next to the content;
        // otherwise the bookkeeping below is skipped and the output is the same either way.
        boolean collect = config.isCollectSourceMap();
        List<SourceOrigin> map = collect ? new ArrayList<>() : null;
        List<SourceOrigin> bodyOrigins = null;
        if (collect) {
            for (int i = 0; i < lineCount(head); i++) {
                map.add(SourceOrigin.note("head comment"));
            }
            bodyOrigins = new ArrayList<>();
            for (Integer n : config.getSourceLineNumbers()) {
                bodyOrigins.add(SourceOrigin.of(config.getSourceFile(), n));
            }
        }

        String body = BlockDispatcher.process(config.getSourceFileBody(), config, language, bodyOrigins);
        int index = 0;
        for (String raw : (Iterable<String>) new BufferedReader(new StringReader(body)).lines()::iterator) {
            TemplateLine line = new TemplateLine(config, raw, index);
            SourceOrigin origin = collect && index < bodyOrigins.size() ? bodyOrigins.get(index) : null;
            index++;
            if (!line.isLineVisible(language)) {
                continue;
            }
            if (collect) {
                config.takeWidgetOrigins();
            }
            String rendered = line.generateLine(language);
            result.getContent().append(rendered).append(System.lineSeparator());
            if (collect) {
                mapRenderedLine(map, rendered, origin, raw, config.takeWidgetOrigins());
            }
        }

        if (config.isRootGenerator()) {
            String rendered = result.getContent().toString();
            String resolved = FreezeBlockProcessor.resolve(rendered, config, language);
            if (collect && !resolved.equals(rendered)) {
                map = remapFrozenRegions(rendered, resolved, map);
            }
            resolved = resolved
                    .replace("\\$", "$")
                    .replace("<\\!--", "<!--")
                    .replace("<!--\\@", "<!--@");
            result.getContent().setLength(0);
            result.getContent().append(resolved);
        }

        result.setSourceMap(map);
        return result;
    }

    private static int lineCount(String text) {
        return text.isEmpty() ? 0 : text.split("\\R", -1).length - (text.endsWith("\n") || text.endsWith("\r") ? 1 : 0);
    }

    /**
     * Origins of the physical lines one template line rendered to. An {@code import} on the line
     * reports where each line of its output comes from; when that report covers the rendered
     * text exactly, those origins are used (seen through this line's import). Anything else a
     * widget produced is attributed to this template line and the widget.
     */
    private static void mapRenderedLine(List<SourceOrigin> map, String rendered, SourceOrigin origin, String raw,
                                        List<List<SourceOrigin>> reports) {
        SourceOrigin here = origin != null ? origin : SourceOrigin.note("unknown");
        String[] physical = rendered.split("\\R", -1);
        List<SourceOrigin> traced = reports.size() == 1 && reports.get(0).size() == physical.length
                ? reports.get(0) : null;
        String widget = firstWidgetName(raw);
        for (int k = 0; k < physical.length; k++) {
            SourceOrigin inner = traced != null ? traced.get(k) : null;
            if (inner != null) {
                map.add(inner.importedVia(here));
            } else {
                map.add(widget != null ? here.producedBy(widget) : here);
            }
        }
    }

    private static String firstWidgetName(String raw) {
        Matcher m = TemplateLine.WIDGET_TAG_PATTERN.matcher(raw);
        while (m.find()) {
            if (m.start() == 0 || raw.charAt(m.start() - 1) != '\\') {
                return m.group(1);
            }
        }
        return null;
    }

    /**
     * A frozen region takes its body from the file on disk, so the number of lines between its
     * markers can change. Lines outside the regions keep their origins in order; lines inside
     * get the origin of the region's opening marker.
     */
    private static List<SourceOrigin> remapFrozenRegions(String before, String after, List<SourceOrigin> map) {
        String[] a = before.split("\\R", -1);
        String[] b = after.split("\\R", -1);
        int linesAfter = b.length - (after.endsWith("\n") ? 1 : 0);
        List<SourceOrigin> out = new ArrayList<>(linesAfter);
        int i = 0;
        int j = 0;
        while (j < linesAfter) {
            SourceOrigin origin = i < map.size() ? map.get(i) : SourceOrigin.note("unknown");
            out.add(origin);
            if (FREEZE_OPEN.matcher(b[j]).find()) {
                i++;
                j++;
                while (i < a.length && !FREEZE_CLOSE.matcher(a[i]).find()) {
                    i++;
                }
                while (j < linesAfter && !FREEZE_CLOSE.matcher(b[j]).find()) {
                    out.add(origin.withNote("frozen region: content kept from the file on disk"));
                    j++;
                }
                continue;
            }
            i++;
            j++;
        }
        return out;
    }

    /**
     * Builds the comment that opens every generated file. The default names the root template,
     * so that whoever opens the generated file — a contributor or a coding agent — learns which
     * file to edit instead of patching the output. The NRG version is deliberately left out of
     * the default: it would change every generated file on each NRG upgrade and turn a version
     * bump into drift for {@code --check}. A custom header ({@code nrg.headerText}) may ask for
     * it through the {@code <version>} placeholder.
     */
    protected String generateHeadComment(String language) {
        if (Boolean.parseBoolean(config.getProperties().getProperty(NRGConstants.PROPERTY_NO_HEADER))) {
            return "";
        }
        String source = sourcePathRelativeToOutput(language);
        String custom = config.getProperties().getProperty(NRGConstants.PROPERTY_HEADER_TEXT);
        if (custom != null) {
            return interpretHeaderEscapes(custom)
                    .replace("<source>", source)
                    .replace("<lang>", language == null ? "" : language)
                    .replace("<version>", NRG.getVersion()) + System.lineSeparator();
        }
        String ls = System.lineSeparator();
        return "<!-- This file was automatically generated by Nanolaba Readme Generator (NRG) from " + source + " -->" + ls +
                "<!-- Do not edit it by hand: change " + source + " and regenerate. See https://github.com/nanolaba/readme-generator -->" + ls;
    }

    /**
     * Path of the root template as seen from the directory of the output file for
     * {@code language}, with {@code /} separators — {@code README.src.md} for the default
     * layout, {@code ../../README.src.md} for {@code docs/<lang>/<base>.md}. Falls back to the
     * bare template name when the output location cannot be resolved (an invalid file name
     * pattern is reported elsewhere) or the two paths share no common root.
     */
    private String sourcePathRelativeToOutput(String language) {
        File sourceFile = config.getSourceFile();
        if (sourceFile == null) {
            return "the source template";
        }
        String fallback = sourceFile.getName();
        try {
            File output = OutputFileNameResolver.resolve(
                    sourceFile, config.getDefaultLanguage(), language, config.getProperties());
            Path outputDir = output.getAbsoluteFile().toPath().normalize().getParent();
            if (outputDir == null) {
                return fallback;
            }
            Path source = sourceFile.getAbsoluteFile().toPath().normalize();
            return outputDir.relativize(source).toString().replace(File.separatorChar, '/');
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    /**
     * Translates the escape sequences {@code \n}, {@code \r}, {@code \t}, {@code \\} and
     * {@code \>} inside a custom header string into their respective characters. {@code \n}
     * maps to {@link System#lineSeparator()} so the rendered header matches the platform
     * convention used everywhere else in the output. {@code \>} exists for the template form:
     * a {@code <!--@nrg.headerText=...-->} marker ends at the first {@code -->}, so a header that
     * is itself an HTML comment has to spell its terminator {@code --\>} (and its opener
     * {@code <\!--}, which the final unescape pass of the generator restores).
     *
     * <p>Single-pass scan to keep escapes literal — replacing them with {@link String#replace}
     * one at a time would let an earlier substitution's output (e.g. a literal {@code \n}
     * unescaped from a {@code \\n} input) be re-interpreted by the next pass.
     */
    private static String interpretHeaderEscapes(String raw) {
        StringBuilder sb = new StringBuilder(raw.length());
        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c == '\\' && i + 1 < raw.length()) {
                char next = raw.charAt(i + 1);
                switch (next) {
                    case 'n': sb.append(System.lineSeparator()); i += 2; continue;
                    case 'r': sb.append('\r'); i += 2; continue;
                    case 't': sb.append('\t'); i += 2; continue;
                    case '\\': sb.append('\\'); i += 2; continue;
                    case '>': sb.append('>'); i += 2; continue;
                    default: break;
                }
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    public GenerationResult getResult(String language) {
        generateContentIfNecessary();
        return results.get(language);
    }

    public Collection<GenerationResult> getResults() {
        generateContentIfNecessary();
        return results.values();
    }

    public GeneratorConfig getConfig() {
        return config;
    }
}
