package com.nanolaba.nrg.core.freeze;

import com.nanolaba.nrg.core.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
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

    @Test
    void undeclaredSourceLangIsReportedAtTheOpenLine() {
        File f = new File("README.src.md");
        String body = "intro\n<!--nrg.freeze id=\"x\" source-lang=\"de\"-->\np\n<!--/nrg.freeze-->\n";
        List<Validator.Diagnostic> ds = FreezeValidator.validate(f, body, Arrays.asList("en", "ru"));
        assertEquals(1, ds.size(), ds::toString);
        Validator.Diagnostic d = ds.get(0);
        assertEquals(2, d.getLine());
        assertTrue(d.isError());
        assertEquals("freeze-block", d.getCode());
        assertEquals("freeze id='x' at line 2: source-lang 'de' is not declared in nrg.languages "
                + "(available: en, ru)", d.getMessage());
    }

    @Test
    void declaredOrUncheckableSourceLangIsAccepted() {
        File f = new File("README.src.md");
        String body = "<!--nrg.freeze id=\"x\" source-lang=\"en\"-->\np\n<!--/nrg.freeze-->\n";
        assertEquals(Collections.emptyList(), FreezeValidator.validate(f, body, Arrays.asList("en", "ru")));
        assertEquals(Collections.emptyList(), FreezeValidator.validate(f, body, null));
        assertEquals(Collections.emptyList(), FreezeValidator.validate(f, null, Arrays.asList("en", "ru")));
    }

    @Test
    void parserDiagnosticsArePassedThrough() {
        List<Validator.Diagnostic> ds = FreezeValidator.validate(new File("README.src.md"),
                "<!--/nrg.freeze-->", Collections.singletonList("en"));
        assertEquals(1, ds.size(), ds::toString);
        assertEquals("unmatched <!--/nrg.freeze--> at line 1", ds.get(0).getMessage());
    }
}
