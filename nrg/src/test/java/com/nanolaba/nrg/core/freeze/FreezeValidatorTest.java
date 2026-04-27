package com.nanolaba.nrg.core.freeze;

import com.nanolaba.nrg.core.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FreezeValidatorTest {

    @Test
    void duplicateIdReported(@TempDir Path tmp) throws Exception {
        File f = tmp.resolve("README.src.md").toFile();
        String body = String.join("\n",
                "<!--@nrg.languages=en-->",
                "<!--nrg.freeze id=\"x\"-->", "p", "<!--/nrg.freeze-->",
                "<!--nrg.freeze id=\"x\"-->", "q", "<!--/nrg.freeze-->",
                "");
        Files.write(f.toPath(), body.getBytes(StandardCharsets.UTF_8));
        List<Validator.Diagnostic> ds = new Validator(f).validate();
        assertTrue(ds.stream().anyMatch(d -> d.getMessage().contains("duplicate freeze id")),
                () -> "expected duplicate-id diagnostic, got: " + ds);
    }

    @Test
    void sourceLangNotInLanguagesIsError(@TempDir Path tmp) throws Exception {
        File f = tmp.resolve("README.src.md").toFile();
        String body = String.join("\n",
                "<!--@nrg.languages=en,ru-->",
                "<!--nrg.freeze id=\"x\" source-lang=\"de\"-->",
                "p",
                "<!--/nrg.freeze-->",
                "");
        Files.write(f.toPath(), body.getBytes(StandardCharsets.UTF_8));
        List<Validator.Diagnostic> ds = new Validator(f).validate();
        assertTrue(ds.stream().anyMatch(d -> d.getMessage().contains("source-lang 'de'")),
                () -> "expected source-lang diagnostic, got: " + ds);
    }
}
