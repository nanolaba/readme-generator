package com.nanolaba.nrg.core;

import com.nanolaba.nrg.DefaultNRGTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DomPomReaderTest extends DefaultNRGTest {

    private static File write(Path file, String content) throws IOException {
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        return file.toFile();
    }

    @Test
    void readsPlainPom(@TempDir Path tmp) throws IOException {
        File pom = write(tmp.resolve("pom.xml"),
                "<?xml version=\"1.0\"?>\n<project><version>1.2.3</version>" +
                        "<properties><a.b>x</a.b></properties></project>");

        DomPomReader reader = new DomPomReader(pom);

        assertEquals(Optional.of("1.2.3"), reader.read("version"));
        assertEquals(Optional.of("x"), reader.read("properties.a.b"));
        assertEquals("", getErrAndClear());
    }

    @Test
    void externalEntityIsNotResolved(@TempDir Path tmp) throws IOException {
        // The descriptor path is template data (nrg.pom.path), so the XML is untrusted: an
        // external entity must neither read a local file nor open a connection.
        write(tmp.resolve("secret.txt"), "TOP-SECRET");
        File pom = write(tmp.resolve("evil.xml"),
                "<?xml version=\"1.0\"?>\n" +
                        "<!DOCTYPE project [<!ENTITY xxe SYSTEM \"secret.txt\">]>\n" +
                        "<project><version>&xxe;</version><name>plain</name></project>");

        DomPomReader reader = new DomPomReader(pom);

        assertEquals(Optional.empty(), reader.read("version"));
        // A document with a DOCTYPE is refused as a whole rather than read in part.
        assertEquals(Optional.empty(), reader.read("name"));
        String err = getErrAndClear();
        assertTrue(err.contains("pom reader: failed to parse"), err);
        assertFalse(err.contains("TOP-SECRET"), err);
    }

    @Test
    void entityExpansionBombIsRefused(@TempDir Path tmp) throws IOException {
        File pom = write(tmp.resolve("bomb.xml"),
                "<?xml version=\"1.0\"?>\n" +
                        "<!DOCTYPE project [<!ENTITY a \"aaaaaaaaaa\"><!ENTITY b \"&a;&a;&a;&a;&a;&a;&a;&a;\">]>\n" +
                        "<project><version>&b;</version></project>");

        assertEquals(Optional.empty(), new DomPomReader(pom).read("version"));
        assertTrue(getErrAndClear().contains("pom reader: failed to parse"));
    }
}
