package com.nanolaba.nrg.core;

import com.nanolaba.nrg.DefaultNRGTest;
import com.nanolaba.nrg.examples.ExampleWidget;
import com.nanolaba.nrg.widgets.NRGWidget;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NRGUtilLoadWidgetsTest extends DefaultNRGTest {

    @Test
    public void testNullOrEmptyReturnsEmptyList() {
        assertTrue(NRGUtil.loadWidgets(null, null).isEmpty());
        assertTrue(NRGUtil.loadWidgets("", null).isEmpty());
        assertTrue(NRGUtil.loadWidgets("   ", null).isEmpty());
    }

    @Test
    public void testLoadsSingleWidget() {
        List<NRGWidget> loaded = NRGUtil.loadWidgets(ExampleWidget.class.getName(), null);
        assertEquals(1, loaded.size());
        assertTrue(loaded.get(0) instanceof ExampleWidget);
        assertEquals("exampleWidget", loaded.get(0).getName());
    }

    @Test
    public void testLoadsMultipleWidgetsAndSkipsEmptyEntries() {
        List<NRGWidget> loaded = NRGUtil.loadWidgets(
                " " + ExampleWidget.class.getName() + " ,, " + ExampleWidget.class.getName(),
                null);
        assertEquals(2, loaded.size());
    }

    @Test
    public void testMissingClassLogsErrorAndSkips() {
        List<NRGWidget> loaded = NRGUtil.loadWidgets("com.does.not.Exist", null);
        assertTrue(loaded.isEmpty());
        assertTrue(getErrAndClear().contains("Widget class not found: 'com.does.not.Exist'"));
    }

    @Test
    public void testClassThatDoesNotImplementNRGWidgetIsRejected() {
        List<NRGWidget> loaded = NRGUtil.loadWidgets("java.lang.String", null);
        assertTrue(loaded.isEmpty());
        assertTrue(getErrAndClear().contains("does not implement"));
    }

    @Test
    public void testClassWithoutNoArgConstructorIsRejected() {
        List<NRGWidget> loaded = NRGUtil.loadWidgets(NoArgConstructorMissing.class.getName(), null);
        assertTrue(loaded.isEmpty());
        assertTrue(getErrAndClear().contains("no-argument constructor"));
    }

    @Test
    public void testRejectedClassIsNotInitialised() {
        // nrg.widgets is template data: naming a class there must not run its static
        // initialiser unless the class really is a widget.
        List<NRGWidget> loaded = NRGUtil.loadWidgets(HasStaticInitialiser.class.getName(), null);

        assertTrue(loaded.isEmpty());
        assertTrue(getErrAndClear().contains("does not implement"));
        assertEquals(0, InitialisationProbe.count, "static initialiser of a non-widget class must not run");
    }

    /** Separate holder so that reading the counter does not itself initialise the class under test. */
    static class InitialisationProbe {
        static int count;
    }

    public static class HasStaticInitialiser {
        static {
            InitialisationProbe.count++;
        }
    }

    public static class NoArgConstructorMissing implements NRGWidget {
        public NoArgConstructorMissing(String required) {/**/}

        @Override
        public String getName() {
            return "noarg";
        }

        @Override
        public String getBody(com.nanolaba.nrg.widgets.WidgetTag t, GeneratorConfig c, String l) {
            return "";
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public void setEnabled(boolean e) {/**/}

        @Override
        public void beforeRenderLine(org.apache.commons.text.TextStringBuilder line) {/**/}

        @Override
        public void afterRenderLine(org.apache.commons.text.TextStringBuilder line, GeneratorConfig config) {/**/}
    }
}
