package com.nanolaba.nrg.widgets;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Unit tests of the static helpers of {@link NRGWidget}. */
class NRGWidgetTest {

    @Test
    public void testParameterNamesKeepsDeclarationOrderAndIsUnmodifiable() {
        Set<String> names = NRGWidget.parameterNames("path", "depth", "path");

        assertEquals(Arrays.asList("path", "depth"), new ArrayList<>(names));
        assertThrows(UnsupportedOperationException.class, () -> names.add("other"));
    }
}
