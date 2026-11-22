package com.nanolaba.nrg.core.freeze;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreezeBlockParserTest {

    @Test
    void parsesSingleBlockWithId() {
        String body = String.join("\n",
                "before",
                "<!--nrg.freeze id=\"contrib\"-->",
                "placeholder",
                "<!--/nrg.freeze-->",
                "after");

        FreezeBlockParser.Result r = FreezeBlockParser.parse(body, null);

        assertEquals(1, r.getMarkers().size());
        FreezeMarker m = r.getMarkers().get(0);
        assertEquals("contrib", m.getId());
        assertFalse(m.getSourceLang().isPresent());
        assertEquals(1, m.getOpenLineIndex());
        assertEquals(3, m.getCloseLineIndex());
        assertTrue(r.getDiagnostics().isEmpty(),
                () -> "unexpected diagnostics: " + r.getDiagnostics());
    }

    @Test
    void parsesSourceLangAttribute() {
        String body = String.join("\n",
                "<!--nrg.freeze id=\"x\" source-lang=\"en\"-->",
                "placeholder",
                "<!--/nrg.freeze-->");
        FreezeBlockParser.Result r = FreezeBlockParser.parse(body, null);
        assertEquals(1, r.getMarkers().size());
        assertEquals("en", r.getMarkers().get(0).getSourceLang().orElseThrow(AssertionError::new));
    }

    @Test
    void attributeOrderIndependence() {
        String a = "<!--nrg.freeze id=\"x\" source-lang=\"en\"-->\np\n<!--/nrg.freeze-->";
        String b = "<!--nrg.freeze source-lang=\"en\" id=\"x\"-->\np\n<!--/nrg.freeze-->";
        FreezeBlockParser.Result ra = FreezeBlockParser.parse(a, null);
        FreezeBlockParser.Result rb = FreezeBlockParser.parse(b, null);
        assertEquals(ra.getMarkers().size(), rb.getMarkers().size());
        assertEquals(1, ra.getMarkers().size());
        assertEquals(ra.getMarkers().get(0).getId(), rb.getMarkers().get(0).getId());
        assertEquals(ra.getMarkers().get(0).getSourceLang(), rb.getMarkers().get(0).getSourceLang());
    }

    @Test
    void singleQuotesAccepted() {
        String body = "<!--nrg.freeze id='contrib'-->\np\n<!--/nrg.freeze-->";
        FreezeBlockParser.Result r = FreezeBlockParser.parse(body, null);
        assertEquals(1, r.getMarkers().size());
        assertEquals("contrib", r.getMarkers().get(0).getId());
    }

    @Test
    void whitespaceAroundMarkerTolerated() {
        String body = "  <!--  nrg.freeze id=\"x\"  -->\np\n  <!-- /nrg.freeze -->  ";
        FreezeBlockParser.Result r = FreezeBlockParser.parse(body, null);
        assertEquals(1, r.getMarkers().size());
    }

    @Test
    void missingIdIsError() {
        String body = "<!--nrg.freeze-->\np\n<!--/nrg.freeze-->";
        FreezeBlockParser.Result r = FreezeBlockParser.parse(body, null);
        assertTrue(r.getMarkers().isEmpty());
        assertEquals(1, r.getDiagnostics().size());
        assertTrue(r.getDiagnostics().get(0).getMessage().contains("missing required 'id'"));
    }

    @Test
    void emptyIdIsError() {
        String body = "<!--nrg.freeze id=\"\"-->\np\n<!--/nrg.freeze-->";
        FreezeBlockParser.Result r = FreezeBlockParser.parse(body, null);
        assertTrue(r.getMarkers().isEmpty());
        assertFalse(r.getDiagnostics().isEmpty());
        assertTrue(r.getDiagnostics().get(0).getMessage().contains("missing required 'id'"));
    }

    @Test
    void duplicateIdsAreError() {
        String body = String.join("\n",
                "<!--nrg.freeze id=\"x\"-->", "p", "<!--/nrg.freeze-->",
                "<!--nrg.freeze id=\"x\"-->", "q", "<!--/nrg.freeze-->");
        FreezeBlockParser.Result r = FreezeBlockParser.parse(body, null);
        assertTrue(r.getDiagnostics().stream()
                .anyMatch(d -> d.getMessage().contains("duplicate freeze id 'x'")));
    }

    @Test
    void openWithoutCloseIsError() {
        String body = "<!--nrg.freeze id=\"x\"-->\np\n";
        FreezeBlockParser.Result r = FreezeBlockParser.parse(body, null);
        assertTrue(r.getMarkers().isEmpty());
        assertTrue(r.getDiagnostics().stream()
                .anyMatch(d -> d.getMessage().contains("missing closing")));
    }

    @Test
    void closeWithoutOpenIsError() {
        String body = "<!--/nrg.freeze-->";
        FreezeBlockParser.Result r = FreezeBlockParser.parse(body, null);
        assertTrue(r.getDiagnostics().stream()
                .anyMatch(d -> d.getMessage().contains("unmatched")));
    }

    @Test
    void nestedBlocksAreError() {
        String body = String.join("\n",
                "<!--nrg.freeze id=\"a\"-->",
                "<!--nrg.freeze id=\"b\"-->",
                "p",
                "<!--/nrg.freeze-->",
                "<!--/nrg.freeze-->");
        FreezeBlockParser.Result r = FreezeBlockParser.parse(body, null);
        assertTrue(r.getDiagnostics().stream()
                .anyMatch(d -> d.getMessage().contains("nested freeze block")));
    }

    @Test
    void unknownAttributeIsError() {
        String body = "<!--nrg.freeze id=\"x\" foo=\"bar\"-->\np\n<!--/nrg.freeze-->";
        FreezeBlockParser.Result r = FreezeBlockParser.parse(body, null);
        assertTrue(r.getDiagnostics().stream()
                .anyMatch(d -> d.getMessage().contains("unknown attribute 'foo'")));
        // Marker with unknown attribute is still produced (it has a valid id).
        assertEquals(1, r.getMarkers().size());
        assertEquals("x", r.getMarkers().get(0).getId());
    }

    /** Every diagnostic as {@code "<line>: <message>"}, in order, so a test can pin both. */
    private static List<String> diagnostics(FreezeBlockParser.Result r) {
        return r.getDiagnostics().stream()
                .map(d -> d.getLine() + ": " + d.getMessage())
                .collect(Collectors.toList());
    }

    @Test
    void nullOrEmptyBodyYieldsAnEmptyResult() {
        for (String body : new String[]{null, ""}) {
            FreezeBlockParser.Result r = FreezeBlockParser.parse(body, null);
            assertTrue(r.getMarkers().isEmpty());
            assertTrue(r.getDiagnostics().isEmpty());
        }
    }

    @Test
    void nestedOpenIsReportedAtItsLineAndTheOuterBlockStaysOpen() {
        String body = String.join("\n",
                "intro",
                "<!--nrg.freeze id=\"a\"-->",
                "<!--nrg.freeze id=\"b\"-->",
                "p",
                "<!--/nrg.freeze-->");
        FreezeBlockParser.Result r = FreezeBlockParser.parse(body, null);
        assertEquals(Collections.singletonList(
                        "3: nested freeze block at line 3 inside open block from line 2; nesting is not supported"),
                diagnostics(r));
        assertEquals(1, r.getMarkers().size());
        FreezeMarker m = r.getMarkers().get(0);
        assertEquals("a", m.getId());
        assertEquals(1, m.getOpenLineIndex());
        assertEquals(4, m.getCloseLineIndex());
        assertEquals("<!--nrg.freeze id=\"a\"-->", m.getOpenLine());
        assertEquals("<!--/nrg.freeze-->", m.getCloseLine());
    }

    @Test
    void unmatchedCloseIsReportedAtItsLine() {
        FreezeBlockParser.Result r = FreezeBlockParser.parse("intro\n<!--/nrg.freeze-->", null);
        assertEquals(Collections.singletonList("2: unmatched <!--/nrg.freeze--> at line 2"), diagnostics(r));
    }

    @Test
    void unknownAttributeIsReportedAtTheOpenLineWithTheId() {
        String body = "intro\n<!--nrg.freeze id=\"x\" foo=\"bar\"-->\np\n<!--/nrg.freeze-->";
        assertEquals(Collections.singletonList(
                        "2: freeze id='x' at line 2: unknown attribute 'foo' (allowed: id, source-lang)"),
                diagnostics(FreezeBlockParser.parse(body, null)));
    }

    @Test
    void unknownAttributeWithoutUsableIdNamesTheBlockUnknown() {
        for (String idAttr : new String[]{"", " id=\"\""}) {
            String body = "intro\n<!--nrg.freeze" + idAttr + " foo=\"bar\"-->\np\n<!--/nrg.freeze-->";
            FreezeBlockParser.Result r = FreezeBlockParser.parse(body, null);
            assertEquals(Arrays.asList(
                            "2: freeze id='<unknown>' at line 2: unknown attribute 'foo' (allowed: id, source-lang)",
                            "2: freeze marker at line 2: missing required 'id' attribute"),
                    diagnostics(r), idAttr);
            assertTrue(r.getMarkers().isEmpty());
        }
    }

    @Test
    void missingIdIsReportedAtTheOpenLine() {
        FreezeBlockParser.Result r = FreezeBlockParser.parse(
                "intro\n<!--nrg.freeze   -->\np\n<!--/nrg.freeze-->", null);
        assertEquals(Collections.singletonList("2: freeze marker at line 2: missing required 'id' attribute"),
                diagnostics(r));
    }

    @Test
    void unclosedBlockIsReportedAtItsOpenLine() {
        assertEquals(Collections.singletonList(
                        "2: freeze id='x' at line 2: missing closing <!--/nrg.freeze--> marker"),
                diagnostics(FreezeBlockParser.parse("intro\n<!--nrg.freeze id=\"x\"-->\np", null)));
        for (String open : new String[]{"<!--nrg.freeze-->", "<!--nrg.freeze id=\"\"-->"}) {
            assertEquals(Collections.singletonList(
                            "2: freeze id='<unknown>' at line 2: missing closing <!--/nrg.freeze--> marker"),
                    diagnostics(FreezeBlockParser.parse("intro\n" + open + "\np", null)), open);
        }
    }

    @Test
    void everyDuplicateIsPairedWithTheFirstOccurrence() {
        String body = String.join("\n",
                "intro",
                "<!--nrg.freeze id=\"d\"-->", "p", "<!--/nrg.freeze-->",
                "<!--nrg.freeze id=\"d\"-->", "q", "<!--/nrg.freeze-->",
                "<!--nrg.freeze id=\"d\"-->", "r", "<!--/nrg.freeze-->");
        FreezeBlockParser.Result r = FreezeBlockParser.parse(body, null);
        assertEquals(Arrays.asList(
                        "5: duplicate freeze id 'd' at lines 2 and 5",
                        "8: duplicate freeze id 'd' at lines 2 and 8"),
                diagnostics(r));
        assertEquals(3, r.getMarkers().size());
    }
}
