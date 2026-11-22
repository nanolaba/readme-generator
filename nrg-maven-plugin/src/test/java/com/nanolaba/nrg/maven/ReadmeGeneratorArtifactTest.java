package com.nanolaba.nrg.maven;

import com.nanolaba.nrg.NRG;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks that the {@code readme-generator} artifact the plugin runs against holds the classes
 * of NRG alone, and that nanolog, sugar and the commons libraries reach the plugin through its
 * POM.
 *
 * <p>Regression for #90: the executable jar with dependencies ({@code nrg.jar}) had replaced
 * the main artifact, so Maven Central served every dependency twice. The plugin then ran the
 * nanolog classes copied into NRG at build time, not the nanolog that Maven resolved. In the
 * reactor build this module's test classpath gets the file the {@code nrg} module attached as
 * its main artifact, so the test fails as soon as the assembly takes that place again. Under
 * {@code mvn test} the classpath holds {@code nrg/target/classes} instead, which is checked the
 * same way.
 */
class ReadmeGeneratorArtifactTest {

    @Test
    public void testTheArtifactHoldsNoClassesOfItsDependencies() throws IOException, URISyntaxException {
        File artifact = new File(NRG.class.getProtectionDomain().getCodeSource().getLocation().toURI());

        List<String> foreign = classesIn(artifact).stream()
                .filter(name -> !name.startsWith("com/nanolaba/nrg/"))
                .limit(5)
                .collect(Collectors.toList());

        assertEquals(Collections.emptyList(), foreign, artifact + " bundles classes of other libraries");
    }

    private static List<String> classesIn(File artifact) throws IOException {
        if (artifact.isDirectory()) {
            Path root = artifact.toPath();
            try (Stream<Path> files = Files.walk(root)) {
                return files.map(p -> root.relativize(p).toString().replace(File.separatorChar, '/'))
                        .filter(name -> name.endsWith(".class"))
                        .collect(Collectors.toList());
            }
        }
        try (JarFile jar = new JarFile(artifact)) {
            return jar.stream().map(ZipEntry::getName)
                    .filter(name -> name.endsWith(".class"))
                    .collect(Collectors.toList());
        }
    }
}
