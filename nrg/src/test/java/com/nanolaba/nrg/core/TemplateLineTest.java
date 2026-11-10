package com.nanolaba.nrg.core;

import com.nanolaba.nrg.DefaultNRGTest;
import com.nanolaba.nrg.widgets.DefaultWidget;
import com.nanolaba.nrg.widgets.WidgetTag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_LANGUAGES;
import static org.junit.jupiter.api.Assertions.*;

class TemplateLineTest extends DefaultNRGTest {

    private TemplateLine line(String line) {
        return line(line, "ru");
    }

    private TemplateLine line(String line, String... languages) {
        String configBody = "<!--@" + PROPERTY_LANGUAGES + "=" + String.join(", ", languages) + "-->" + line;
        return new TemplateLine(new GeneratorConfig(new File("README.src.md"), configBody, null), line, 0);
    }

    @Test
    public void testLineCreation() {
        TemplateLine line = line("test");
        assertEquals("test", line.getLine());
    }

    @Test
    public void testLineVisibility() {

        assertTrue(line("").isLineVisible("ru"));
        assertTrue(line(" ").isLineVisible("ru"));
        assertTrue(line("test <!--ru-->").isLineVisible("ru"));
        assertTrue(line("test <!-- ru -->").isLineVisible("ru"));
        assertTrue(line("test <!-- ru --> test").isLineVisible("ru"));
        assertTrue(line("test <!-- ru --><!--en-->", "ru", "en").isLineVisible("ru"));
        assertTrue(line("<!--ru-->").isLineVisible("ru"));
        assertTrue(line("<!--ru--> test").isLineVisible("ru"));
        assertTrue(line("<!-- ru -->").isLineVisible("ru"));
        assertTrue(line("<!-- ru --> test").isLineVisible("ru"));
        assertTrue(line("<!--  ru  --> test").isLineVisible("ru"));
        assertTrue(line("test  <!-- fr -->", "ru", "en").isLineVisible("ru"));
        assertTrue(line("test  <!-- fr -->", "ru", "en").isLineVisible("en"));

        assertFalse(line("<!--en-->", "ru", "en").isLineVisible("ru"));
        assertFalse(line("<!--en--> test", "ru", "en").isLineVisible("ru"));
        assertFalse(line("test <!--en-->", "ru", "en").isLineVisible("ru"));
        assertFalse(line("test <!-- en-->", "ru", "en").isLineVisible("ru"));
        assertFalse(line("test <!-- en -->", "ru", "en").isLineVisible("ru"));
        assertFalse(line("test  <!-- en -->", "ru", "en").isLineVisible("ru"));

        assertFalse(line("<!--@nrg.someproperty-->").isLineVisible("ru"));
        assertFalse(line("<!--@nrg.some property-->").isLineVisible("ru"));
        assertFalse(line("<!--@nrg.some=property-->").isLineVisible("ru"));
        assertFalse(line("<!--@nrg.some = property-->").isLineVisible("ru"));
        assertFalse(line("<!--  @nrg.some  =  property  -->").isLineVisible("ru"));

        // Property definitions with nothing readable around them: still not a visible line.
        assertFalse(line("<!--@a=1--><!--@b=2-->").isLineVisible("ru"));
        assertFalse(line("- <!--@a=1--> ").isLineVisible("ru"));
        assertFalse(line("<!--@a=1--><!--ru-->").isLineVisible("ru"));

        // Text next to a definition makes the line visible. The old check dropped all three:
        // text between two comments, text followed by a plain comment, and any non-ASCII text
        // (\W is ASCII-only in Java, so Cyrillic counted as "no word characters").
        assertTrue(line("<!--@a=1--> text <!--@b=2-->").isLineVisible("ru"));
        assertTrue(line("<!--@a=1--> text <!-- plain -->").isLineVisible("ru"));
        assertTrue(line("<!--@a=1--> Привет").isLineVisible("ru"));
    }

    @Test
    public void testRemoveNrgDataFromText() {
        Function<String, String> action = s -> line(s).removeNrgDataFromText(s);

        assertEquals("", action.apply(""));
        assertEquals(" ", action.apply(" "));
        assertEquals(" ", action.apply(" <!--ru-->"));
        assertEquals(" ", action.apply("<!--ru--> "));
        assertEquals("123 ", action.apply("123<!--ru--> "));
        assertEquals("123<!--en--> ", action.apply("123<!--en--> "));
        assertEquals("", action.apply("<!--@nrg.test-->"));
        assertEquals("", action.apply("<!--@nrg.test -->"));
        assertEquals("", action.apply("<!--@nrg.test = test-->"));
        assertEquals("", action.apply("<!-- @nrg.test = test --><!--ru-->"));
        assertEquals("123", action.apply("1<!-- @nrg.test = test -->2<!--ru-->3"));
        assertEquals("123", action.apply("1<!-- @nrg.test  =  test  -->2<!--ru-->3"));

        // A property comment ends at its own "-->", not at the last one on the line.
        assertEquals("A  KEEP <!-- plain --> tail", action.apply("A <!--@p=1--> KEEP <!-- plain --> tail"));
        assertEquals("1 KEEP 2", action.apply("1<!--@p=1--> KEEP <!--@q=2-->2"));
    }

    @Test
    public void testTextNextToPropertyDefinitionsSurvivesGeneration() {
        assertEquals("A  KEEP <!-- plain --> tail",
                line("A <!--@p=1--> KEEP <!-- plain --> tail").generateLine("ru"));
        assertEquals(" KEEP ", line("<!--@p=1--> KEEP <!--@q=2-->").generateLine("ru"));
        assertEquals(" Привет, ${p}".replace("${p}", "1"), line("<!--@p=1--> Привет, ${p}").generateLine("ru"));
        assertNull(line("<!--@p=1--><!--@q=2-->").generateLine("ru"));
    }

    @Test
    public void testGetWidgetTag() {
        Function<String, WidgetTag> action = s -> {
            List<WidgetTag> tags = line(s).getWidgetTags(s);
            return tags.isEmpty() ? null : tags.get(0);
        };

        assertNull(action.apply(""));
        assertNull(action.apply("123"));
        assertNull(action.apply("123<!--test-->"));
        assertNull(action.apply("123${widget}"));
        assertNull(action.apply("${nrg:widget:languages}"));

        assertEquals("languages", action.apply("${widget:languages}").getName());
        assertEquals("languages", action.apply("${ widget:languages }").getName());
        assertEquals("languages", action.apply(" ${ widget:languages } ").getName());
        assertEquals("languages", action.apply("qwe ${ widget:languages } 123").getName());

        assertNull(action.apply("${widget:languages}").getParameters());
        assertEquals("", action.apply("${widget:languages()}").getParameters());
        assertEquals("parameters", action.apply("${widget:languages(parameters)}").getParameters());
        assertEquals("a b c", action.apply("${widget:languages(a b c)}").getParameters());
    }

    @Test
    public void testGetWidgetTagWithParenthesesInQuotedParameters() {
        Function<String, List<WidgetTag>> tags = s -> line(s).getWidgetTags(s);

        // Regression for #55: a ')' inside a quoted value must not end the parameter list.
        assertEquals("cond='endsWith(1.1, -SNAPSHOT)'",
                tags.apply("${widget:if(cond='endsWith(1.1, -SNAPSHOT)')}").get(0).getParameters());
        assertEquals("expr=\"f(x) = (a + b)\", display='block'",
                tags.apply("x ${widget:math(expr=\"f(x) = (a + b)\", display='block')} y").get(0).getParameters());
        assertEquals("text='a) b'", tags.apply("${widget:todo(text='a) b')}").get(0).getParameters());

        // The other quote character inside a value is plain text, apostrophes included.
        assertEquals("text=\"it's (done)\"",
                tags.apply("${widget:todo(text=\"it's (done)\")}").get(0).getParameters());

        // Two tags on one line stay two tags.
        List<WidgetTag> two = tags.apply("${widget:a(x='(1)')} and ${widget:b(y='(2)')}");
        assertEquals(2, two.size());
        assertEquals("x='(1)'", two.get(0).getParameters());
        assertEquals("y='(2)'", two.get(1).getParameters());

        // A quote without a partner is not a delimiter: such tags were recognised before
        // quotes became significant and must stay recognised.
        assertEquals("text='it's'", tags.apply("${widget:todo(text='it's')}").get(0).getParameters());

        // An unquoted ')' still ends the parameter list.
        assertEquals("a", tags.apply("${widget:test(a)} b)}").get(0).getParameters());
    }

    @Test
    public void testLongTextInLanguageConstructDoesNotOverflowTheStack() {
        // About two thousand characters used to be enough for a StackOverflowError: the old
        // pattern looped over an alternation, and java.util.regex recurses once per iteration.
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 20_000; i++) {
            longText.append("word ");
        }
        String text = longText.toString();

        assertEquals(text, line("${en:'x', ru:'" + text + "'}", "en", "ru").generateLine("ru"));
        assertEquals(text, line("${ru:\"" + text + "\", en:'x'}", "en", "ru").generateLine("ru"));
        assertEquals("x", line("${en:'x', ru:'" + text + "'}", "en", "ru").generateLine("en"));

        // Doubled quotes are the other loop in the pattern.
        StringBuilder quotes = new StringBuilder();
        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < 20_000; i++) {
            quotes.append("it''s ");
            expected.append("it's ");
        }
        assertEquals(expected.toString(), line("${ru:'" + quotes + "'}", "en", "ru").generateLine("ru"));
    }

    @Test
    public void testMultiLineWidgetBodyKeepsIndentationOfTheWidgetLine() {
        // Regression for #57: the tag is replaced in place, so without help only the first
        // line of a multi-line body sits at the indentation of the widget line.
        BiFunction<String, String, String> action = (s, widgetBody) -> {
            TemplateLine l = line(s, "ru", "en");
            l.getConfig().getWidgets().add(new DefaultWidget() {
                @Override
                public String getName() {
                    return "test";
                }

                @Override
                public String getBody(WidgetTag tag, GeneratorConfig config, String language) {
                    return widgetBody;
                }
            });
            return l.generateLine("ru");
        };

        assertEquals("   a\n   b\n   c", action.apply("   ${widget:test}", "a\nb\nc"));
        assertEquals("\t a\r\n\t b", action.apply("\t ${widget:test}", "a\r\nb"));
        assertEquals("  a\n  b tail", action.apply("  ${widget:test} tail", "a\nb"));

        // Blank lines stay blank, and a trailing line break gets nothing appended.
        assertEquals("  a\n\n  b\n", action.apply("  ${widget:test}", "a\n\nb\n"));
        assertEquals("  a\r\n\r\n  b\r\n", action.apply("  ${widget:test}", "a\r\n\r\nb\r\n"));

        // Nothing to repeat: no indentation, a single-line body, or text before the tag.
        assertEquals("a\nb", action.apply("${widget:test}", "a\nb"));
        assertEquals("   a", action.apply("   ${widget:test}", "a"));
        assertEquals("- a\nb", action.apply("- ${widget:test}", "a\nb"));
        assertEquals("  x a\nb", action.apply("  x ${widget:test}", "a\nb"));

        // Only the first tag on a line starts at the indentation.
        assertEquals("  a\n  b a\nb", action.apply("  ${widget:test} ${widget:test}", "a\nb"));
    }

    @Test
    public void testRenderWidget() {
        BiFunction<String, String, String> action = (s, lang) -> {
            TemplateLine l = line(s, "ru", "en");
            l.getConfig().getWidgets().add(new DefaultWidget() {
                @Override
                public String getName() {
                    return "test";
                }

                @Override
                public String getBody(WidgetTag tag, GeneratorConfig config, String language) {
                    return "test widget body " + tag.getParameters();
                }
            });
            return l.generateLine(lang);
        };

        assertEquals("\\${widget:test}", action.apply("\\${widget:test}", "ru"));
        assertEquals("test widget body null", action.apply("${widget:test}", "ru"));
        assertEquals("test widget body null", action.apply("${widget:test}", "ru"));
        assertEquals("test widget body ", action.apply("${ widget:test() }", "ru"));
        assertEquals("test widget body AAA", action.apply("${ widget:test(AAA) }", "ru"));
        assertEquals("test widget body AAA=123", action.apply("${ widget:test(AAA=123) }", "ru"));
        assertEquals("test widget body AAA=123, BBB=234", action.apply("${ widget:test(AAA=123, BBB=234) }", "ru"));

        assertEquals("test widget body 123", action.apply("${ widget:test(${ru:'123'}) }", "ru"));
        assertEquals("test widget body 123", action.apply("${ widget:test(${ru:\"123\"}) }", "ru"));
        assertEquals("test widget body 123'", action.apply("${ widget:test(${ru:\"123'\"}) }", "ru"));
        assertEquals("test widget body '123'", action.apply("${ widget:test(${ru:\"'123'\"}) }", "ru"));
        assertEquals("test widget body 123\"", action.apply("${ widget:test(${ru:'123\"'}) }", "ru"));
        assertEquals("test widget body \"123\"", action.apply("${ widget:test(${ru:'\"123\"'}) }", "ru"));
        assertEquals("test widget body \"123\"", action.apply("${ widget:test(${ru:'\"123\"'}) }", "ru"));
        assertEquals("test widget body \"123\"", action.apply("${ widget:test(${ru:'\"123\"', en:'aa'}) }", "ru"));
        assertEquals("test widget body \"123\"", action.apply("${ widget:test(${ru:'\"123\"', en:\"aa\"}) }", "ru"));
        assertEquals("test widget body \"123\"", action.apply("${ widget:test(${ru:'\"123\"', en:\"'aa\"}) }", "ru"));

        assertEquals("test widget body \"123:\"", action.apply("${ widget:test(${ru:'\"123:\"', en:\"'aa\"}) }", "ru"));
        assertEquals("test widget body \"123:\"", action.apply("${ widget:test(${ru:'\"123:\"', en:\"'aa\"}) }<!--ru-->", "ru"));

        assertNull(action.apply("${ widget:test(${ru:'\"123:\"', en:\"'aa\"}) }<!--en-->", "ru"));

        assertEquals("test widget body \"", action.apply("${ widget:test(${ru:'\"'}) }", "ru"));
        assertEquals("test widget body '", action.apply("${ widget:test(${ru:''''}) }", "ru"));
        assertEquals("test widget body '", action.apply("${ widget:test(${ru:\"'\"}) }", "ru"));
        assertEquals("test widget body \"", action.apply("${ widget:test(${ru:\"\"\"\"}) }", "ru"));

        assertEquals("${widget:unknownWidget}", action.apply("${widget:unknownWidget}", "ru"));

        assertTrue(getOutAndClear().endsWith("Unknown widget name: unknownWidget" + RN));
    }

    @Test
    public void testReadProperties() {

        Properties p = line("").readProperties("ru");
        assertPropertiesSize(0, p);

        p = line("<!--@AAA=BBB-->").readProperties("ru");
        assertPropertiesSize(1, p);
        assertEquals("BBB", p.getProperty("AAA"));

        p = line("<!--@AAA-->").readProperties("ru");
        assertPropertiesSize(1, p);
        assertEquals("", p.getProperty("AAA"));

        p = line("<!--@AAA=BBB-->").readProperties("ru");
        assertPropertiesSize(1, p);
        assertEquals("BBB", p.getProperty("AAA"));

        p = line("<!--@AAA=BBB--><!--@AAA.BBB=CCC.DDD-->").readProperties("ru");
        assertPropertiesSize(2, p);
        assertEquals("BBB", p.getProperty("AAA"));
        assertEquals("CCC.DDD", p.getProperty("AAA.BBB"));

        p = line("<!-- @AAA = BBB --><!--@ AAA.BBB =  CCC.DDD  -->").readProperties("ru");
        assertPropertiesSize(2, p);
        assertEquals("BBB", p.getProperty("AAA"));
        assertEquals("CCC.DDD", p.getProperty("AAA.BBB"));

        p = line("<!--@AAA=${ru:'BBB', en:'CCC'}-->", "en", "ru").readProperties("ru");
        assertPropertiesSize(1, p);
        assertEquals("BBB", p.getProperty("AAA"));
    }

    private void assertPropertiesSize(int expected, Properties p) {
        int defaultPropertiesCount = 1;
        assertEquals(defaultPropertiesCount + expected, p.size());
    }

    @Test
    public void testRenderProperties() {
        assertEquals("ru", line("${" + PROPERTY_LANGUAGES + "}").generateLine("ru"));
        assertEquals("BB", line("<!--@ AA=BB -->${AA}").generateLine("ru"));
        assertEquals("<!--comment-->BB", line("<!--comment--><!--@ AA=BB -->${AA}").generateLine("ru"));
        assertEquals("${A_A}", line("<!--@ A.A=BB -->${A_A}").generateLine("ru"));
    }

    @Test
    public void testRenderLanguageProperties() {
        assertEquals("${en:'Table of contents'}", line("${en:'Table of contents'}", "ru").generateLine("ru"));
        assertEquals("${en:'Table, of contents'}", line("${en:'Table, of contents'}", "ru").generateLine("ru"));
        assertEquals("${en:\"Table of contents\"}", line("${en:\"Table of contents\"}", "ru").generateLine("ru"));
        assertEquals("", line("${en:'Table of contents'}", "en", "ru").generateLine("ru"));
        assertEquals("", line("${en:\"Table of contents\"}", "en", "ru").generateLine("ru"));
        assertEquals("", line("${en:'Table of contents', ru:''}", "en", "ru").generateLine("ru"));
        assertEquals("", line("${en:\"Table of contents\"}", "en", "ru").generateLine("ru"));
        assertEquals("Содержание", line("${ru:'Содержание'}", "en", "ru").generateLine("ru"));
        assertEquals("Со,держание", line("${ru:'Со,держание'}", "en", "ru").generateLine("ru"));
        assertEquals("Сод:ержание", line("${ru:'Сод:ержание'}", "en", "ru").generateLine("ru"));
        assertEquals("Содержание", line("${ru:\"Содержание\"}", "en", "ru").generateLine("ru"));
        assertEquals("'Содержание", line("${ru:\"'Содержание\"}", "en", "ru").generateLine("ru"));
        assertEquals("'Содержание'", line("${ru:\"'Содержание'\"}", "en", "ru").generateLine("ru"));
        assertEquals("Содержание", line("${en:'Table of contents', ru:'Содержание'}", "en", "ru").generateLine("ru"));
        assertEquals("Содержание", line("${en:'Table of contents', ru:\"Содержание\"}", "en", "ru").generateLine("ru"));
        assertEquals("Table of contents", line("${en:'Table of contents', ru:'Содержание'}", "en", "ru").generateLine("en"));
        assertEquals("Table of contents", line("${en:\"Table of contents\", ru:'Содержание'}", "en", "ru").generateLine("en"));
        assertEquals("Table of contents", line("${en:\"Table of contents\", ru:\"Содержание\"}", "en", "ru").generateLine("en"));
        assertEquals("\\${en:'Table of contents', ru:'Содержание'}", line("\\${en:'Table of contents', ru:'Содержание'}", "en", "ru").generateLine("en"));
    }

    @Test
    public void testRenderLanguagePropertiesWithNestedPropertyReference() {
        // Single nested property reference inside both language variants (issue #28).
        assertEquals("Project Sample info",
                line("<!--@projectName=Sample-->${en:'Project ${projectName} info', ru:'Проект ${projectName}'}",
                        "en", "ru").generateLine("en"));
        assertEquals("Проект Sample",
                line("<!--@projectName=Sample-->${en:'Project ${projectName} info', ru:'Проект ${projectName}'}",
                        "en", "ru").generateLine("ru"));
    }

    @Test
    public void testRenderLanguagePropertiesWithMultipleNestedPropertyReferences() {
        assertEquals("A X B Y C",
                line("<!--@var=X--><!--@var2=Y-->${en:'A ${var} B ${var2} C', ru:'А ${var} Б ${var2} В'}",
                        "en", "ru").generateLine("en"));
        assertEquals("А X Б Y В",
                line("<!--@var=X--><!--@var2=Y-->${en:'A ${var} B ${var2} C', ru:'А ${var} Б ${var2} В'}",
                        "en", "ru").generateLine("ru"));
    }

    @Test
    public void testRenderLanguagePropertiesWithNestedEnvDefault() {
        // ${env.X:default} is resolved before language substitution; the nested ref must not break parsing.
        assertEquals("Build dev",
                line("${en:'Build ${env.NRG_TEST_UNDEFINED_VAR_28:dev}', ru:'Сборка ${env.NRG_TEST_UNDEFINED_VAR_28:dev}'}",
                        "en", "ru").generateLine("en"));
    }

    @Test
    public void testRenderLanguagePropertiesWithEscapedNestedReference() {
        // Backslash-escaped ${...} inside the language quotes must remain literal in the output.
        assertEquals("Project \\${projectName} stays",
                line("<!--@projectName=Sample-->${en:'Project \\${projectName} stays', ru:'Проект \\${projectName} остаётся'}",
                        "en", "ru").generateLine("en"));
    }

    @Test
    public void testRenderManyWidgetsInOneLine() {
        String yyyyMM = new SimpleDateFormat("yyyyMM").format(new Date());
        assertEquals(yyyyMM, line("${widget:date(pattern='yyyy')}${widget:date(pattern='MM')}", "en", "ru").generateLine("en"));
    }

    @Test
    public void testRenderManyLanguagePropertiesInOneLine() {
        assertEquals("${en:'A'}${en:'B'}", line("${en:'A'}${en:'B'}", "ru").generateLine("ru"));
        assertEquals("", line("${en:'A'}${en:'B'}", "en", "ru").generateLine("ru"));
        assertEquals("AB", line("${en:'A'}${en:'B'}", "en", "ru").generateLine("en"));
        assertEquals("AB", line("${en:'A', ru:'Z'}${en:'B', ru:'X'}", "en", "ru").generateLine("en"));
    }

    @Test
    public void testRenderManyPropertiesInOneLine() {
        assertEquals("BBBB", line("<!--@ AA=BB -->${AA}${AA}").generateLine("ru"));
        assertEquals("BBDD", line("<!--@ AA=BB --><!--@ CC=DD -->${AA}${CC}").generateLine("ru"));
    }

    @Test
    public void testRenderCombinedProperty() {
        assertEquals("BB", line("<!--@AA=BB --><!--@CC=${AA}-->${CC}").generateLine("ru"));
        assertEquals("123", line("<!--@A=123 --><!--@B=${A}--><!--@C=${B}-->${C}").generateLine("ru"));
    }

    @Test
    public void testEscapeCharacters() {
        assertEquals("\\${A}", line("<!--@A=B-->\\${A}").generateLine("ru"));
    }

    @Test
    public void testEscapeCharacters2() {
        TemplateLine line = line("<!--@AA=BB--><!--@CC=\\${AA}-->${CC}");

        assertEquals("BB", line.getProperty("AA", "ru"));
        assertEquals("\\${AA}", line.getProperty("CC", "ru"));
        assertEquals("\\${AA}", line.generateLine("ru"));
    }

    @Test
    public void testEscapeCharacters3() {
        assertEquals("<!--\\@A=B-->", line("<!--\\@A=B-->").generateLine("ru"));
    }

    @Test
    public void testEscapeCharacters4() {
        assertEquals("AA<\\!--ru-->", line("AA<\\!--ru-->").generateLine("ru"));
    }

    @Test
    public void testEscapeReplacementHappensAtGeneratorLevel() {
        // Regression: per-line generateLine no longer unescapes; that's the Generator post-pass's job.
        String body = String.join("\n",
                "<!--@nrg.languages=en-->",
                "\\${A}",
                "<!--\\@key=value-->",
                "<\\!--ru-->",
                "");
        com.nanolaba.nrg.core.Generator g = new com.nanolaba.nrg.core.Generator(
                new java.io.File("README.src.md"), body, null);
        String out = g.getResult("en").getContent().toString();
        assertTrue(out.contains("${A}"), "expected ${A} after generator-level unescape, got: " + out);
        assertTrue(out.contains("<!--@key=value-->"), "expected <!--@key=value--> after unescape");
        assertTrue(out.contains("<!--ru-->"), "expected <!--ru--> after unescape");
    }

    @Test
    public void testEscapeRule1StripsAnyLiteralDollar() {
        // Regression for #50: rule 1 strips the backslash from any \$, not only \${...}.
        // Authors who want a literal \$ in the output (e.g. [\$] in a markdown link) must
        // double the backslash: \\$ → \$.
        String body = String.join("\n",
                "<!--@nrg.languages=en-->",
                "DOLLAR=[\\$]",
                "DOUBLE=[\\\\$]",
                "");
        com.nanolaba.nrg.core.Generator g = new com.nanolaba.nrg.core.Generator(
                new java.io.File("README.src.md"), body, null);
        String out = g.getResult("en").getContent().toString();
        assertTrue(out.contains("DOLLAR=[$]"),
                "expected [$] (rule 1 strips any \\$, even outside \\${...}), got: " + out);
        assertTrue(out.contains("DOUBLE=[\\$]"),
                "expected [\\$] (\\\\$ keeps a literal \\$ in the output), got: " + out);
    }
}