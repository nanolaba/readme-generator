package com.nanolaba.nrg.core;

import com.nanolaba.nrg.DefaultNRGTest;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class IfConditionTest extends DefaultNRGTest {

    private static GeneratorConfig configWith(String body) {
        return configWith(body, new HashMap<>());
    }

    private static GeneratorConfig configWith(String body, Map<String, String> env) {
        return new GeneratorConfig(new File("README.src.md"), body, null, env::get);
    }

    @Test
    public void testTruthyEmptyIsFalse() {
        GeneratorConfig cfg = configWith("");
        assertFalse(IfCondition.evaluate("", cfg, "en"));
        assertFalse(IfCondition.evaluate("   ", cfg, "en"));
    }

    @Test
    public void testTruthyNonEmptyLiteral() {
        GeneratorConfig cfg = configWith("");
        assertTrue(IfCondition.evaluate("yes", cfg, "en"));
    }

    @Test
    public void testTruthyPlaceholder() {
        Map<String, String> env = new HashMap<>();
        env.put("X", "value");
        GeneratorConfig cfg = configWith("", env);
        assertTrue(IfCondition.evaluate("${env.X}", cfg, "en"));
    }

    @Test
    public void testTruthyPlaceholderEmpty() {
        GeneratorConfig cfg = configWith("", new HashMap<>());
        assertFalse(IfCondition.evaluate("${env.X}", cfg, "en"));
    }

    @Test
    public void testNotOperator() {
        GeneratorConfig cfg = configWith("");
        assertTrue(IfCondition.evaluate("!\"\"", cfg, "en"));
        assertFalse(IfCondition.evaluate("!yes", cfg, "en"));
    }

    @Test
    public void testEqualityLiterals() {
        GeneratorConfig cfg = configWith("");
        assertTrue(IfCondition.evaluate("foo==foo", cfg, "en"));
        assertFalse(IfCondition.evaluate("foo==bar", cfg, "en"));
    }

    @Test
    public void testEqualityWithPlaceholder() {
        Map<String, String> env = new HashMap<>();
        env.put("X", "abc");
        GeneratorConfig cfg = configWith("", env);
        assertTrue(IfCondition.evaluate("${env.X}==abc", cfg, "en"));
        assertFalse(IfCondition.evaluate("${env.X}==xyz", cfg, "en"));
    }

    @Test
    public void testInequalityWithPlaceholder() {
        Map<String, String> env = new HashMap<>();
        env.put("X", "abc");
        GeneratorConfig cfg = configWith("", env);
        assertFalse(IfCondition.evaluate("${env.X}!=abc", cfg, "en"));
        assertTrue(IfCondition.evaluate("${env.X}!=xyz", cfg, "en"));
    }

    @Test
    public void testEqualityTrimsWhitespace() {
        Map<String, String> env = new HashMap<>();
        env.put("X", "abc");
        GeneratorConfig cfg = configWith("", env);
        assertTrue(IfCondition.evaluate("${env.X} == abc", cfg, "en"));
        assertTrue(IfCondition.evaluate("${env.X}== abc ", cfg, "en"));
    }

    @Test
    public void testQuotedLiteralPreservesWhitespace() {
        Map<String, String> env = new HashMap<>();
        env.put("X", " abc ");
        GeneratorConfig cfg = configWith("", env);
        assertTrue(IfCondition.evaluate("\"${env.X}\" == \" abc \"", cfg, "en"));
    }

    @Test
    public void testAndShortCircuit() {
        GeneratorConfig cfg = configWith("");
        assertFalse(IfCondition.evaluate("\"\" && yes", cfg, "en"));
        assertTrue(IfCondition.evaluate("yes && yes", cfg, "en"));
        assertFalse(IfCondition.evaluate("yes && \"\"", cfg, "en"));
        assertFalse(IfCondition.evaluate("yes && no==stop", cfg, "en"));
        assertTrue(IfCondition.evaluate("yes && eq==eq", cfg, "en"));
    }

    @Test
    public void testOrShortCircuit() {
        GeneratorConfig cfg = configWith("");
        assertTrue(IfCondition.evaluate("yes || \"\"", cfg, "en"));
        assertTrue(IfCondition.evaluate("\"\" || yes", cfg, "en"));
        assertFalse(IfCondition.evaluate("\"\" || \"\"", cfg, "en"));
    }

    @Test
    public void testPrecedenceAndOverOr() {
        GeneratorConfig cfg = configWith("");
        // a || b && c  ≡  a || (b && c)
        assertTrue(IfCondition.evaluate("yes || nope && nope", cfg, "en"));
    }

    @Test
    public void testGroupingWithParens() {
        GeneratorConfig cfg = configWith("");
        // (yes || no) && no  ≡ false
        assertFalse(IfCondition.evaluate("(yes || no) && \"\"", cfg, "en"));
        // !(yes && no) ≡ true
        assertTrue(IfCondition.evaluate("!(yes && \"\")", cfg, "en"));
    }

    @Test
    public void testStartsWith() {
        Map<String, String> env = new HashMap<>();
        env.put("URL", "https://github.com/foo");
        GeneratorConfig cfg = configWith("", env);
        assertTrue(IfCondition.evaluate("startsWith(${env.URL}, https://)", cfg, "en"));
        assertFalse(IfCondition.evaluate("startsWith(${env.URL}, http://)", cfg, "en"));
    }

    @Test
    public void testEndsWith() {
        Map<String, String> env = new HashMap<>();
        env.put("VER", "1.0-SNAPSHOT");
        GeneratorConfig cfg = configWith("", env);
        assertTrue(IfCondition.evaluate("endsWith(${env.VER}, -SNAPSHOT)", cfg, "en"));
        assertFalse(IfCondition.evaluate("endsWith(${env.VER}, -RELEASE)", cfg, "en"));
    }

    @Test
    public void testFunctionsCanBeCombined() {
        Map<String, String> env = new HashMap<>();
        env.put("URL", "https://github.com/foo");
        GeneratorConfig cfg = configWith("", env);
        assertTrue(IfCondition.evaluate(
                "startsWith(${env.URL}, https://) || startsWith(${env.URL}, git@)", cfg, "en"));
    }

    @Test
    public void testPlaceholderValueWithOperatorsIsOpaque() {
        // If ${env.MSG} resolves to "a && b", the && inside MUST NOT be reinterpreted as boolean.
        Map<String, String> env = new HashMap<>();
        env.put("MSG", "a && b");
        GeneratorConfig cfg = configWith("", env);
        // Compared as opaque string: equal to literal "a && b", true
        assertTrue(IfCondition.evaluate("\"${env.MSG}\" == \"a && b\"", cfg, "en"));
        // Truthy is just non-empty
        assertTrue(IfCondition.evaluate("${env.MSG}", cfg, "en"));
    }

    @Test
    public void testAndShortCircuitDoesNotResolveRightSide() {
        AtomicInteger calls = new AtomicInteger();
        java.util.function.Function<String, String> envProvider = key -> {
            calls.incrementAndGet();
            return null;
        };
        GeneratorConfig cfg = new GeneratorConfig(new File("README.src.md"), "", null, envProvider);
        IfCondition.evaluate("\"\" && ${env.SHOULD_NOT_RESOLVE}", cfg, "en");
        assertEquals(0, calls.get(), "right side should not be resolved when left is false");
    }

    @Test
    public void testOrShortCircuitDoesNotResolveRightSide() {
        AtomicInteger calls = new AtomicInteger();
        java.util.function.Function<String, String> envProvider = key -> {
            calls.incrementAndGet();
            return null;
        };
        GeneratorConfig cfg = new GeneratorConfig(new File("README.src.md"), "", null, envProvider);
        IfCondition.evaluate("yes || ${env.SHOULD_NOT_RESOLVE}", cfg, "en");
        assertEquals(0, calls.get(), "right side should not be resolved when left is true");
    }

    @Test
    public void testUnbalancedParensThrows() {
        GeneratorConfig cfg = configWith("");
        assertThrows(IfCondition.ParseException.class, () -> IfCondition.evaluate("(yes", cfg, "en"));
        assertThrows(IfCondition.ParseException.class, () -> IfCondition.evaluate("yes)", cfg, "en"));
    }

    @Test
    public void testTrailingOperatorThrows() {
        GeneratorConfig cfg = configWith("");
        assertThrows(IfCondition.ParseException.class, () -> IfCondition.evaluate("yes &&", cfg, "en"));
        assertThrows(IfCondition.ParseException.class, () -> IfCondition.evaluate("yes ||", cfg, "en"));
    }

    @Test
    public void testUnknownFunctionFallsThroughAsBareString() {
        // "contains(...)" is not a recognised function; the parens parse as grouping/comparison
        GeneratorConfig cfg = configWith("");
        // Just verify it doesn't blow up; treats "contains(x" as a bare comparison subject etc.
        assertThrows(IfCondition.ParseException.class,
                () -> IfCondition.evaluate("contains(x, y)", cfg, "en"));
    }
}
