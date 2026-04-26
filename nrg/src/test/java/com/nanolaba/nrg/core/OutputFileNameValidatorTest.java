package com.nanolaba.nrg.core;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class OutputFileNameValidatorTest {

    @Test
    void noCollision_returnsEmpty() {
        Optional<String> err = OutputFileNameValidator.findError(
                new File("repo/README.src.md"), "en", Arrays.asList("en", "ru"), new Properties());
        assertFalse(err.isPresent());
    }

    @Test
    void collision_singlePatternMissingLangPlaceholder_reportsError() {
        Properties p = new Properties();
        p.setProperty("nrg.fileNamePattern", "README.md");
        Optional<String> err = OutputFileNameValidator.findError(
                new File("repo/README.src.md"), "en", Arrays.asList("en", "ru"), p);
        assertTrue(err.isPresent(), "expected error");
        assertTrue(err.get().contains("collide"), err.get());
    }

    @Test
    void collision_perLanguagePatternsTargetingSamePath_reportsError() {
        Properties p = new Properties();
        p.setProperty("nrg.fileNamePattern.en", "docs/README.md");
        p.setProperty("nrg.fileNamePattern.ru", "docs/README.md");
        Optional<String> err = OutputFileNameValidator.findError(
                new File("repo/README.src.md"), "en", Arrays.asList("en", "ru"), p);
        assertTrue(err.isPresent(), "expected error");
    }

    @Test
    void singleLanguage_patternWithoutLangPlaceholder_isOk() {
        Properties p = new Properties();
        p.setProperty("nrg.fileNamePattern", "README.md");
        Optional<String> err = OutputFileNameValidator.findError(
                new File("repo/README.src.md"), "en", Arrays.asList("en"), p);
        assertFalse(err.isPresent(), err.orElse(""));
    }

    @Test
    void emptyPattern_propagatesAsError() {
        Properties p = new Properties();
        p.setProperty("nrg.fileNamePattern", "");
        Optional<String> err = OutputFileNameValidator.findError(
                new File("repo/README.src.md"), "en", Arrays.asList("en", "ru"), p);
        assertTrue(err.isPresent(), "expected error");
        assertTrue(err.get().toLowerCase().contains("empty"), err.get());
    }
}
