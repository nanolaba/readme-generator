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
    public void testNullConditionIsFalse() {
        assertFalse(IfCondition.evaluate(null, configWith(""), "en"));
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
    public void testEqualityTrimsResolvedPlaceholderValues() {
        Map<String, String> env = new HashMap<>();
        env.put("X", " abc ");
        GeneratorConfig cfg = configWith("", env);
        assertTrue(IfCondition.evaluate("${env.X}==abc", cfg, "en"));
        assertTrue(IfCondition.evaluate("abc==${env.X}", cfg, "en"));
        assertFalse(IfCondition.evaluate("${env.X}!=abc", cfg, "en"));
    }

    @Test
    public void testQuotedLiteralPreservesWhitespace() {
        Map<String, String> env = new HashMap<>();
        env.put("X", " abc ");
        GeneratorConfig cfg = configWith("", env);
        assertTrue(IfCondition.evaluate("\"${env.X}\" == \" abc \"", cfg, "en"));
    }

    @Test
    public void testOneQuotedSideDisablesTrimming() {
        Map<String, String> env = new HashMap<>();
        env.put("X", " abc ");
        GeneratorConfig cfg = configWith("", env);
        assertFalse(IfCondition.evaluate("\" abc\" == abc", cfg, "en"));
        assertFalse(IfCondition.evaluate("abc == \" abc\"", cfg, "en"));
        assertFalse(IfCondition.evaluate("\"${env.X}\" == abc", cfg, "en"));
        assertFalse(IfCondition.evaluate("${env.X} == \"abc\"", cfg, "en"));
        assertTrue(IfCondition.evaluate("${env.X} == \" abc \"", cfg, "en"));
    }

    @Test
    public void testOperatorsNeedNoSurroundingWhitespace() {
        GeneratorConfig cfg = configWith("");
        assertFalse(IfCondition.evaluate("yes&&\"\"", cfg, "en"));
        assertTrue(IfCondition.evaluate("a==a&&b!=c", cfg, "en"));
        assertTrue(IfCondition.evaluate("\"\"||!\"\"", cfg, "en"));
        assertTrue(IfCondition.evaluate("!(a!=a)", cfg, "en"));
        assertTrue(IfCondition.evaluate("startsWith(abc,a)&&endsWith(abc,c)", cfg, "en"));
    }

    @Test
    public void testSingleOperatorCharactersStayInsideBareStrings() {
        GeneratorConfig cfg = configWith("");
        assertTrue(IfCondition.evaluate("a&b == a&b", cfg, "en"));
        assertTrue(IfCondition.evaluate("a|b == a|b", cfg, "en"));
        assertTrue(IfCondition.evaluate("a=b == a=b", cfg, "en"));
        assertTrue(IfCondition.evaluate("a$b == a$b", cfg, "en"));
        assertFalse(IfCondition.evaluate("a&b == a", cfg, "en"));
    }

    @Test
    public void testQuotedStringWithDoubledQuotes() {
        GeneratorConfig cfg = configWith("");
        assertTrue(IfCondition.evaluate("'a''b' == \"a'b\"", cfg, "en"));
        assertTrue(IfCondition.evaluate("\"say \"\"hi\"\"\" == 'say \"hi\"'", cfg, "en"));
        assertTrue(IfCondition.evaluate("'' == \"\"", cfg, "en"));
    }

    @Test
    public void testUnterminatedQuotedStringThrows() {
        GeneratorConfig cfg = configWith("");
        IfCondition.ParseException single = assertThrows(IfCondition.ParseException.class,
                () -> IfCondition.evaluate("a == 'abc", cfg, "en"));
        assertEquals("Unterminated quoted string starting at position 5", single.getMessage());
        assertThrows(IfCondition.ParseException.class, () -> IfCondition.evaluate("\"abc", cfg, "en"));
        assertThrows(IfCondition.ParseException.class, () -> IfCondition.evaluate("'abc''", cfg, "en"));
    }

    @Test
    public void testPlaceholderWithNestedBraces() {
        GeneratorConfig cfg = configWith("<!--@nrg.languages=en,ru-->");
        assertTrue(IfCondition.evaluate("${en:'{a}', ru:'{b}'} == {a}", cfg, "en"));
        assertTrue(IfCondition.evaluate("${en:'{a}', ru:'{b}'} == {b}", cfg, "ru"));
    }

    @Test
    public void testUnterminatedPlaceholderThrows() {
        GeneratorConfig cfg = configWith("");
        IfCondition.ParseException e = assertThrows(IfCondition.ParseException.class,
                () -> IfCondition.evaluate("a == ${env.X", cfg, "en"));
        assertEquals("Unterminated placeholder starting at position 5", e.getMessage());
        assertThrows(IfCondition.ParseException.class, () -> IfCondition.evaluate("${a{b}", cfg, "en"));
        assertThrows(IfCondition.ParseException.class, () -> IfCondition.evaluate("${", cfg, "en"));
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
    public void testFunctionNameMayBeFollowedByWhitespace() {
        GeneratorConfig cfg = configWith("");
        assertTrue(IfCondition.evaluate("startsWith (abc, a)", cfg, "en"));
        assertTrue(IfCondition.evaluate("endsWith\t(abc, c)", cfg, "en"));
        assertFalse(IfCondition.evaluate("startsWith  (abc, c)", cfg, "en"));
    }

    @Test
    public void testFunctionNameWithoutArgumentListIsABareString() {
        GeneratorConfig cfg = configWith("");
        assertTrue(IfCondition.evaluate("startsWith", cfg, "en"));
        assertTrue(IfCondition.evaluate("endsWith ", cfg, "en"));
        assertTrue(IfCondition.evaluate("endsWith == endsWith", cfg, "en"));
        assertTrue(IfCondition.evaluate("a startsWith b == a startsWith b", cfg, "en"));
        assertThrows(IfCondition.ParseException.class,
                () -> IfCondition.evaluate("startsWithX(abc, a)", cfg, "en"));
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

    private static String repeat(String s, int n) {
        StringBuilder b = new StringBuilder(s.length() * n);
        for (int i = 0; i < n; i++) {
            b.append(s);
        }
        return b.toString();
    }

    @Test
    public void testDeepNestingIsAParseErrorNotAStackOverflow() {
        GeneratorConfig cfg = configWith("");

        IfCondition.ParseException parens = assertThrows(IfCondition.ParseException.class,
                () -> IfCondition.evaluate(repeat("(", 20_000) + "yes" + repeat(")", 20_000), cfg, "en"));
        assertTrue(parens.getMessage().contains("nested too deeply"), parens.getMessage());

        assertThrows(IfCondition.ParseException.class,
                () -> IfCondition.evaluate(repeat("!", 20_000) + "yes", cfg, "en"));
        assertThrows(IfCondition.ParseException.class,
                () -> IfCondition.evaluate(repeat("(", 20_000), cfg, "en"));
    }

    @Test
    public void testNestingWithinTheLimitStillEvaluates() {
        GeneratorConfig cfg = configWith("");

        assertTrue(IfCondition.evaluate(repeat("(", 60) + "yes" + repeat(")", 60), cfg, "en"));
        assertTrue(IfCondition.evaluate(repeat("!", 60) + "yes", cfg, "en"));
        // Sequential groups do not add depth, and a long flat chain must not recurse per
        // operator when it is evaluated.
        assertTrue(IfCondition.evaluate("(yes)" + repeat(" && (yes)", 50_000), cfg, "en"));
        assertFalse(IfCondition.evaluate("\"\"" + repeat(" || \"\"", 50_000), cfg, "en"));
        assertTrue(IfCondition.evaluate("\"\"" + repeat(" || \"\"", 50_000) + " || yes", cfg, "en"));
    }

    @Test
    public void testNestingLimitIsSixtyFourLevels() {
        GeneratorConfig cfg = configWith("");

        assertTrue(IfCondition.evaluate(repeat("(", 64) + "yes" + repeat(")", 64), cfg, "en"));
        assertThrows(IfCondition.ParseException.class,
                () -> IfCondition.evaluate(repeat("(", 65) + "yes" + repeat(")", 65), cfg, "en"));
        assertTrue(IfCondition.evaluate(repeat("!", 64) + "yes", cfg, "en"));
        assertThrows(IfCondition.ParseException.class,
                () -> IfCondition.evaluate(repeat("!", 65) + "yes", cfg, "en"));
    }

    @Test
    public void testSequentialNegationsDoNotAddDepth() {
        GeneratorConfig cfg = configWith("");
        assertTrue(IfCondition.evaluate("!\"\"" + repeat(" && !\"\"", 100), cfg, "en"));
    }
}
