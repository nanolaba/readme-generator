package com.nanolaba.nrg.maven;

import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.widgets.NRGWidget;
import com.nanolaba.nrg.widgets.WidgetTag;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class NRGMojoWidgetsTest {

    @Test
    public void testUnknownClassThrowsMojoExecutionException() {
        NRGMojo mojo = new NRGMojo();
        mojo.setFile(new String[]{"README.src.md"});
        mojo.setWidgets(Collections.singletonList("com.does.not.Exist"));

        MojoExecutionException e = assertThrows(MojoExecutionException.class, mojo::execute);
        assertTrue(e.getMessage().contains("Widget class not found"));
        assertTrue(e.getMessage().contains("com.does.not.Exist"));
        assertTrue(e.getMessage().contains("<dependency>"));
    }

    @Test
    public void testClassNotImplementingNRGWidgetThrows() {
        NRGMojo mojo = new NRGMojo();
        mojo.setFile(new String[]{"README.src.md"});
        mojo.setWidgets(Collections.singletonList("java.lang.String"));

        MojoExecutionException e = assertThrows(MojoExecutionException.class, mojo::execute);
        assertTrue(e.getMessage().contains("does not implement"));
        assertTrue(e.getMessage().contains("java.lang.String"));
    }

    @Test
    public void testClassWithoutNoArgConstructorThrows() {
        NRGMojo mojo = new NRGMojo();
        mojo.setFile(new String[]{"README.src.md"});
        mojo.setWidgets(Collections.singletonList(NoArgMissingWidget.class.getName()));

        MojoExecutionException e = assertThrows(MojoExecutionException.class, mojo::execute);
        assertTrue(e.getMessage().contains("no-argument constructor"));
    }

    @Test
    public void testValidWidgetPassesValidation() {
        NRGMojo mojo = new NRGMojo();
        // Point to a non-existent source so NRG.main logs an error and returns without writing files.
        mojo.setFile(new String[]{"this-file-does-not-exist-for-testing.src.md"});
        mojo.setWidgets(Arrays.asList(SampleMojoWidget.class.getName()));

        assertDoesNotThrow(mojo::execute);
    }

    @Test
    public void testBlankAndNullEntriesAreIgnored() {
        NRGMojo mojo = new NRGMojo();
        mojo.setFile(new String[]{"this-file-does-not-exist-for-testing.src.md"});
        mojo.setWidgets(Arrays.asList("", "   ", null, SampleMojoWidget.class.getName()));

        assertDoesNotThrow(mojo::execute);
    }

    public static class SampleMojoWidget implements NRGWidget {
        @Override
        public String getName() {
            return "sampleMojoWidget";
        }

        @Override
        public String getBody(WidgetTag t, GeneratorConfig c, String l) {
            return "";
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public void setEnabled(boolean e) {/**/}
    }

    public static class NoArgMissingWidget implements NRGWidget {
        public NoArgMissingWidget(String required) {/**/}

        @Override
        public String getName() {
            return "noargMojo";
        }

        @Override
        public String getBody(WidgetTag t, GeneratorConfig c, String l) {
            return "";
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public void setEnabled(boolean e) {/**/}
    }
}
