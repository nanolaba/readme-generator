package com.nanolaba.nrg.core.freeze;

import org.junit.jupiter.api.Test;

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
}
