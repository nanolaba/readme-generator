package com.nanolaba.nrg.examples;

import com.nanolaba.nrg.core.GenerationResult;
import com.nanolaba.nrg.core.Generator;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class GeneratorExample {

    public static void main(String[] args) throws IOException {

        Generator generator = new Generator(new File("template.md"), StandardCharsets.UTF_8);

        for (GenerationResult generationResult : generator.getResults()) {

            FileUtils.write(
                    new File("result." + generationResult.getLanguage() + ".md"),
                    generationResult.getContent(),
                    StandardCharsets.UTF_8);
        }
    }
}