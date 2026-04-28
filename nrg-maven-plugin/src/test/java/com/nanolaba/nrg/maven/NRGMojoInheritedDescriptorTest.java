package com.nanolaba.nrg.maven;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the generated Maven plugin descriptor declares
 * {@code <inheritByDefault>false</inheritByDefault>} for the {@code create-files}
 * goal. The {@code @Mojo} annotation has {@code @Retention(CLASS)}, so we cannot
 * inspect it via runtime reflection — instead we read the descriptor that the
 * {@code maven-plugin-plugin} generates from the annotations into
 * {@code META-INF/maven/plugin.xml} on the test classpath.
 *
 * <p>Regression for #49: in multi-module aggregator projects, README generation
 * is intrinsically a root-of-repo concern. Defaulting {@code inheritByDefault}
 * to {@code false} stops child modules from silently re-running the goal in
 * their own directory (where {@code README.src.md} usually does not exist).
 */
class NRGMojoInheritedDescriptorTest {

    @Test
    public void testCreateFilesGoalIsNotInheritedByDefault() {
        String descriptor = readDescriptor();

        int goalIdx = descriptor.indexOf("<goal>create-files</goal>");
        assertTrue(goalIdx >= 0, "create-files goal must be declared in plugin.xml");

        // Find the enclosing <mojo>...</mojo> block.
        int mojoStart = descriptor.lastIndexOf("<mojo>", goalIdx);
        int mojoEnd = descriptor.indexOf("</mojo>", goalIdx);
        assertTrue(mojoStart >= 0 && mojoEnd > mojoStart,
                "could not locate <mojo> block for create-files in plugin.xml");

        String mojoBlock = descriptor.substring(mojoStart, mojoEnd);
        assertTrue(mojoBlock.contains("<inheritedByDefault>false</inheritedByDefault>"),
                "expected <inheritedByDefault>false</inheritedByDefault> for create-files goal " +
                        "(child modules of an aggregator project must not re-run NRG by default), " +
                        "but the descriptor block was: " + mojoBlock);
    }

    private String readDescriptor() {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("META-INF/maven/plugin.xml")) {
            assertNotNull(in, "META-INF/maven/plugin.xml not on the test classpath; " +
                    "rebuild the plugin so maven-plugin-plugin regenerates the descriptor");
            try (Scanner scanner = new Scanner(in, StandardCharsets.UTF_8.name())) {
                scanner.useDelimiter("\\A");
                return scanner.hasNext() ? scanner.next() : "";
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }
}
