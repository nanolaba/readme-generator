package com.nanolaba.nrg.core;

import com.nanolaba.nrg.DefaultNRGTest;
import com.nanolaba.nrg.widgets.DefaultWidget;
import com.nanolaba.nrg.widgets.DetailsWidget;
import com.nanolaba.nrg.widgets.IfWidget;
import com.nanolaba.nrg.widgets.NRGWidget;
import com.nanolaba.nrg.widgets.WidgetTag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratorConfigAliasTest extends DefaultNRGTest {

    /** Custom widget exposing a single alias for testing. */
    private static class CustomAliasedWidget extends DefaultWidget {
        private final String name;
        private final Set<String> aliases;

        CustomAliasedWidget(String name, Set<String> aliases) {
            this.name = name;
            this.aliases = aliases;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Set<String> getAliases() {
            return aliases;
        }

        @Override
        public String getBody(WidgetTag tag, GeneratorConfig config, String language) {
            return "X";
        }
    }

    private static GeneratorConfig build(String body) {
        return new GeneratorConfig(new File("README.src.md"), body, null);
    }

    private static GeneratorConfig buildWith(String body, NRGWidget... extras) {
        return new GeneratorConfig(new File("README.src.md"), body, Arrays.asList(extras));
    }

    @Test
    public void testGetWidgetResolvesViaPrimaryName() {
        GeneratorConfig cfg = build("");
        NRGWidget tocByPrimary = cfg.getWidget("tableOfContents");
        assertTrue(tocByPrimary != null, "tableOfContents must resolve");
    }

    @Test
    public void testGetWidgetResolvesBlockCloserViaAlias() {
        GeneratorConfig cfg = build("");
        NRGWidget endIfResolved = cfg.getWidget("endIf");
        NRGWidget endDetailsResolved = cfg.getWidget("endDetails");
        assertTrue(endIfResolved instanceof IfWidget,
                "endIf should resolve to IfWidget, got: " + endIfResolved);
        assertTrue(endDetailsResolved instanceof DetailsWidget,
                "endDetails should resolve to DetailsWidget, got: " + endDetailsResolved);
    }

    @Test
    public void testGetWidgetReturnsNullForUnknown() {
        GeneratorConfig cfg = build("");
        assertNull(cfg.getWidget("noSuchWidget"));
    }

    @Test
    public void testCustomAliasResolvesToCustomWidget() {
        CustomAliasedWidget w = new CustomAliasedWidget("foo", Collections.singleton("f"));
        GeneratorConfig cfg = buildWith("", w);
        assertSame(w, cfg.getWidget("foo"));
        assertSame(w, cfg.getWidget("f"));
    }

    @Test
    public void testLastRegisteredWidgetWinsOnNameCollision() {
        // Documented precedence model: last-wins across the widget list.
        CustomAliasedWidget first = new CustomAliasedWidget("twin", Collections.<String>emptySet());
        CustomAliasedWidget second = new CustomAliasedWidget("twin", Collections.<String>emptySet());
        GeneratorConfig cfg = buildWith("", first, second);
        assertSame(second, cfg.getWidget("twin"));
    }

    @Test
    public void testCustomAliasOverridesBuiltinAliasUnderLastWins() {
        // A custom widget aliased to 'endIf' is registered AFTER IfWidget, so getWidget("endIf")
        // returns the custom widget. Preserves precedence semantics; alias resolution sits on
        // the same last-wins linear scan as primary-name resolution.
        CustomAliasedWidget shadow = new CustomAliasedWidget("custom",
                Collections.singleton("endIf"));
        GeneratorConfig cfg = buildWith("", shadow);
        assertSame(shadow, cfg.getWidget("endIf"));
    }

    @Test
    public void testCustomPrimaryOverridesBuiltinAlias() {
        // A widget primary-named 'endIf' registered after IfWidget wins for the "endIf" lookup.
        CustomAliasedWidget shadow = new CustomAliasedWidget("endIf",
                Collections.<String>emptySet());
        GeneratorConfig cfg = buildWith("", shadow);
        assertSame(shadow, cfg.getWidget("endIf"));
    }

    @Test
    public void testDefaultGetAliasesIsEmpty() {
        GeneratorConfig cfg = build("");
        NRGWidget todo = cfg.getWidget("todo");
        assertTrue(todo != null);
        assertEquals(Collections.emptySet(), todo.getAliases());
    }

    @Test
    public void testMidLineCloserLogsClearerMessage() {
        // Mid-line ${widget:endIf} slips through the pre-pass's line-anchored END_LINE regex
        // and reaches per-line dispatch via the alias. BlockWidget.getBody should shortcircuit
        // with a clearer "closer must appear on its own line" message — not the misleading
        // "opener without inline-form parameter" path.
        new Generator(new File("README.src.md"), "text ${widget:endIf} text\n")
                .getResult("en").getContent().toString();
        String err = getErrAndClear();
        assertTrue(err.contains("if widget: closer") && err.contains("must appear on its own line"),
                "expected mid-line closer error, got: " + err);
    }

    @Test
    public void testCrossClassNameCollisionLogsWarning() {
        // A custom widget aliased to 'endIf' shadows IfWidget's closer alias (different class).
        // Last-wins resolution still applies — the warning is just informational. nanolog
        // routes WARN to stdout, so we read getOutAndClear() rather than stderr.
        CustomAliasedWidget shadow = new CustomAliasedWidget("custom",
                Collections.singleton("endIf"));
        buildWith("", shadow);
        String out = getOutAndClear();
        assertTrue(out.contains("'endIf'") && out.contains("last-registered wins"),
                "expected name-collision warning, got: " + out);
    }

    @Test
    public void testSameClassReRegistrationDoesNotWarn() {
        // The established "configured override" pattern (passing a configured instance of an
        // existing widget class) must stay silent — otherwise CLI/Maven override paths get noisy.
        CustomAliasedWidget first = new CustomAliasedWidget("twin", Collections.<String>emptySet());
        CustomAliasedWidget second = new CustomAliasedWidget("twin", Collections.<String>emptySet());
        buildWith("", first, second);
        String out = getOutAndClear();
        assertFalse(out.contains("last-registered wins"),
                "same-class re-registration must not warn, got: " + out);
    }

    @Test
    public void testWidgetAddedAfterInitIsResolvable() {
        // Widgets added to the widget list AFTER GeneratorConfig construction (used by the
        // API-precedence path) are reachable through getWidget — no precomputed index that
        // would freeze the registration set at init time.
        CustomAliasedWidget late = new CustomAliasedWidget("late", Collections.<String>emptySet());
        GeneratorConfig cfg = build("");
        assertNull(cfg.getWidget("late"));
        cfg.getWidgets().add(late);
        assertSame(late, cfg.getWidget("late"));
    }
}
