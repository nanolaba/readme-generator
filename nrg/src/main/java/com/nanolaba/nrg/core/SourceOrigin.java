package com.nanolaba.nrg.core;

import java.io.File;
import java.util.function.IntUnaryOperator;

/**
 * Where one line of a generated file comes from: a line of a template (or of a file it
 * imports), the chain of {@code import} lines that led there, and — when a widget produced
 * the line and its output cannot be traced further — the widget's name. Collected only on
 * request ({@link GeneratorConfig#setCollectSourceMap}) and read through
 * {@link GenerationResult#getSourceMap()}; it backs {@code --locate}, which answers "where do
 * I fix line 340 of README.ru.md?" without searching the templates for the text.
 *
 * <p>Instances are immutable. {@link #getFile()} is {@code null} for lines no template line
 * produced (the head comment); {@link #getNote()} then says what they are.
 */
public final class SourceOrigin {

    private final File file;
    private final int line;
    private final String widget;
    private final String note;
    private final SourceOrigin importedFrom;

    private SourceOrigin(File file, int line, String widget, String note, SourceOrigin importedFrom) {
        this.file = file;
        this.line = line;
        this.widget = widget;
        this.note = note;
        this.importedFrom = importedFrom;
    }

    /** Line {@code line} (1-based) of {@code file}. */
    public static SourceOrigin of(File file, int line) {
        return new SourceOrigin(file, line, null, null, null);
    }

    /** Output that no template line produced, such as the head comment. */
    public static SourceOrigin note(String note) {
        return new SourceOrigin(null, 0, null, note, null);
    }

    /** The same location, attributed to the output of {@code widgetName}. */
    public SourceOrigin producedBy(String widgetName) {
        return new SourceOrigin(file, line, widgetName, note, importedFrom);
    }

    /** The same location with {@code note} attached (e.g. a frozen region). */
    public SourceOrigin withNote(String text) {
        return new SourceOrigin(file, line, widget, text, importedFrom);
    }

    /**
     * This origin as seen from the template that imported it: {@code importLine} becomes the
     * outermost link of the import chain.
     */
    public SourceOrigin importedVia(SourceOrigin importLine) {
        return new SourceOrigin(file, line, widget, note,
                importedFrom == null ? importLine : importedFrom.importedVia(importLine));
    }

    /**
     * Rewrites the line number of the outermost link of the chain — the link that belongs to the
     * template this origin was computed in — leaving the inner links as they are. An import
     * uses it to turn "line of the selected text" into "line of the imported file".
     */
    public SourceOrigin mapOutermostLine(IntUnaryOperator mapping) {
        if (importedFrom == null) {
            return new SourceOrigin(file, file == null ? line : mapping.applyAsInt(line), widget, note, null);
        }
        return new SourceOrigin(file, line, widget, note, importedFrom.mapOutermostLine(mapping));
    }

    public File getFile() {
        return file;
    }

    public int getLine() {
        return line;
    }

    /** Name of the widget whose output this line is, or {@code null}. */
    public String getWidget() {
        return widget;
    }

    public String getNote() {
        return note;
    }

    /** The {@code import} line through which this file was included, or {@code null} for the root template. */
    public SourceOrigin getImportedFrom() {
        return importedFrom;
    }

    @Override
    public String toString() {
        if (file == null) {
            return note == null ? "<generated>" : note;
        }
        return file.getName() + ":" + line + (widget == null ? "" : " widget " + widget)
                + (note == null ? "" : " (" + note + ")")
                + (importedFrom == null ? "" : " <- " + importedFrom);
    }
}
