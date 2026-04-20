package com.nanolaba.nrg.widgets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImportLanguageDetectorTest {

    @ParameterizedTest
    @CsvSource({
            "Foo.java, java",
            "Foo.JAVA, java",
            "script.kt, kotlin",
            "script.kts, kotlin",
            "build.gradle, groovy",
            "lib.scala, scala",
            "app.js, javascript",
            "app.mjs, javascript",
            "app.cjs, javascript",
            "app.ts, typescript",
            "app.tsx, typescript",
            "main.py, python",
            "test.rb, ruby",
            "main.go, go",
            "lib.rs, rust",
            "main.c, c",
            "header.h, c",
            "main.cpp, cpp",
            "main.cc, cpp",
            "header.hpp, cpp",
            "Program.cs, csharp",
            "index.php, php",
            "App.swift, swift",
            "deploy.sh, bash",
            "deploy.bash, bash",
            "task.ps1, powershell",
            "run.bat, batch",
            "run.cmd, batch",
            "query.sql, sql",
            "config.xml, xml",
            "page.html, html",
            "page.htm, html",
            "style.css, css",
            "style.scss, scss",
            "data.json, json",
            "config.yaml, yaml",
            "config.yml, yaml",
            "config.toml, toml",
            "app.properties, properties",
            "doc.md, markdown",
            "foo.src.md, markdown",
            "init.lua, lua",
            "main.dart, dart"
    })
    void detectsKnownExtensions(String filename, String expected) {
        assertEquals(expected, ImportLanguageDetector.detectFromFilename(filename));
    }

    @Test
    void unknownExtensionReturnsEmpty() {
        assertEquals("", ImportLanguageDetector.detectFromFilename("foo.unknownext"));
    }

    @Test
    void filenameWithoutExtensionReturnsEmpty() {
        assertEquals("", ImportLanguageDetector.detectFromFilename("Dockerfile"));
    }

    @Test
    void filenameStartingWithDotReturnsEmpty() {
        assertEquals("", ImportLanguageDetector.detectFromFilename(".bashrc"));
    }

    @Test
    void filenameEndingWithDotReturnsEmpty() {
        assertEquals("", ImportLanguageDetector.detectFromFilename("foo."));
    }

    @Test
    void nullReturnsEmpty() {
        assertEquals("", ImportLanguageDetector.detectFromFilename(null));
    }

    @Test
    void multiDotTakesLastExtension() {
        assertEquals("markdown", ImportLanguageDetector.detectFromFilename("readme.src.md"));
    }
}
