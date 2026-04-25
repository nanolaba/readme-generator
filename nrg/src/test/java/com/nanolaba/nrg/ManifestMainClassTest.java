package com.nanolaba.nrg;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ManifestMainClassTest {

    @Test
    public void mainClassInPomMatchesActualNrgClass() throws IOException {
        Path pom = Paths.get("pom.xml");
        assertTrue(Files.exists(pom), "expected to run from nrg module dir, pom.xml not found at " + pom.toAbsolutePath());

        String xml = new String(Files.readAllBytes(pom), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("<mainClass>([^<]+)</mainClass>").matcher(xml);
        assertTrue(m.find(), "no <mainClass> entry found in nrg/pom.xml");
        String declared = m.group(1).trim();

        assertEquals(NRG.class.getName(), declared,
                "nrg/pom.xml <mainClass> must match the real NRG class FQN. "
                        + "Mismatch will yield a broken Main-Class in the published nrg.jar MANIFEST.MF.");

        assertDoesNotThrow(() -> Class.forName(declared),
                "<mainClass> in nrg/pom.xml does not resolve to a real class: " + declared);
    }
}
