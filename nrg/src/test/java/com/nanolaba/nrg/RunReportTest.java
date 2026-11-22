package com.nanolaba.nrg;

import com.nanolaba.nrg.core.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The exact text of {@link RunReport}: the layout of the JSON document with and without entries,
 * the GitHub workflow commands for each kind of diagnostic and output, and how paths inside and
 * outside the working directory are written.
 */
public class RunReportTest {

    @TempDir
    Path tempDir;

    private static String version() {
        return RunReport.str(NRG.getVersion().trim());
    }

    @Test
    public void testEmptyReportHasEmptyArrays() {
        RunReport report = new RunReport("validate", tempDir.toFile());

        assertEquals("{\n  \"version\": " + version() + ",\n  \"mode\": \"validate\",\n  \"ok\": true,\n"
                + "  \"exitCode\": 0,\n  \"files\": [],\n  \"diagnostics\": []\n}", report.toJson(0));
    }

    @Test
    public void testJsonListsFilesAndDiagnostics() {
        File work = tempDir.resolve("work").toFile();
        File outside = tempDir.resolve("other/a.src.md").toFile();
        RunReport report = new RunReport("check", work);
        report.addFile(new File(work, "README.src.md"), new File(work, "docs/README.md"), "en", "stale");
        report.addFile(outside, null, null, "invalid");
        report.addDiagnostic(new Validator.Diagnostic(new File(work, "README.src.md"), 3,
                Validator.Severity.WARNING, "undefined-variable", "undefined", null));
        report.addDiagnostics(Collections.singletonList(
                new Validator.Diagnostic(null, 0, Validator.Severity.ERROR, "bad", "broken", "fix it")));

        String absolute = outside.getAbsolutePath().replace(File.separatorChar, '/');
        assertEquals("{\n  \"version\": " + version() + ",\n  \"mode\": \"check\",\n  \"ok\": false,\n"
                + "  \"exitCode\": 1,\n  \"files\": [\n"
                + "    {\"source\": \"README.src.md\", \"output\": \"docs/README.md\", \"language\": \"en\", \"status\": \"stale\"},\n"
                + "    {\"source\": \"" + absolute + "\", \"status\": \"invalid\"}\n  ],\n"
                + "  \"diagnostics\": [\n"
                + "    {\"severity\": \"warning\", \"code\": \"undefined-variable\", \"file\": \"README.src.md\", \"line\": 3, "
                + "\"message\": \"undefined\", \"hint\": null},\n"
                + "    {\"severity\": \"error\", \"code\": \"bad\", \"file\": null, \"line\": 0, "
                + "\"message\": \"broken\", \"hint\": \"fix it\"}\n  ]\n}", report.toJson(1));
    }

    @Test
    public void testGithubAnnotations() {
        File work = tempDir.toFile();
        File source = new File(work, "README.src.md");
        RunReport report = new RunReport("check", work);
        report.addDiagnostic(new Validator.Diagnostic(source, 0, Validator.Severity.ERROR, "no-line", "whole file", null));
        report.addDiagnostic(new Validator.Diagnostic(source, 1, Validator.Severity.WARNING, "first-line", "a: b,c", "100%"));
        report.addDiagnostic(new Validator.Diagnostic(null, 7, Validator.Severity.ERROR, "no-file", "nowhere", null));
        report.addFile(source, new File(work, "README.md"), "en", "stale");
        report.addFile(source, new File(work, "README.ru.md"), "ru", "missing");
        report.addFile(source, new File(work, "README.de.md"), "de", "written");

        assertEquals(Arrays.asList(
                "::error file=README.src.md,title=NRG no-line::whole file",
                "::warning file=README.src.md,line=1,title=NRG first-line::a: b,c; 100%25",
                "::error title=NRG no-file::nowhere",
                "::error file=README.md,title=NRG stale output::README.md is out of date with README.src.md; "
                        + "regenerate with: nrg -f README.src.md",
                "::error file=README.ru.md,title=NRG missing output::README.ru.md does not exist; "
                        + "regenerate with: nrg -f README.src.md"),
                report.toGithubAnnotations());
    }
}
