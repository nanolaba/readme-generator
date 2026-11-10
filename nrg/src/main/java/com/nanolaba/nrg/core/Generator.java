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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

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
        result.getContent().append(generateHeadComment(language));

        String body = BlockDispatcher.process(config.getSourceFileBody(), config, language);
        AtomicInteger counter = new AtomicInteger(0);
        new BufferedReader(new StringReader(body))
                .lines()
                .map(s -> new TemplateLine(config, s, counter.getAndIncrement()))
                .filter(t -> t.isLineVisible(language))
                .map(t -> t.generateLine(language))
                .forEachOrdered(line -> result.getContent().append(line).append(System.lineSeparator()));

        if (config.isRootGenerator()) {
            String resolved = FreezeBlockProcessor.resolve(result.getContent().toString(), config, language);
            resolved = resolved
                    .replace("\\$", "$")
                    .replace("<\\!--", "<!--")
                    .replace("<!--\\@", "<!--@");
            result.getContent().setLength(0);
            result.getContent().append(resolved);
        }

        return result;
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
