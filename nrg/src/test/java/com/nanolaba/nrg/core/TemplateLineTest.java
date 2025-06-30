package com.nanolaba.nrg.core;

import com.nanolaba.nrg.DefaultNRGTest;
import com.nanolaba.nrg.widgets.NRGWidget;
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
        return new TemplateLine(new GeneratorConfig(new File("README.src.md"), configBody), line, 0);
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
    public void testRenderWidget() {
        BiFunction<String, String, String> action = (s, lang) -> {
            TemplateLine l = line(s, "ru", "en");
            l.getConfig().getWidgets().add(new NRGWidget() {
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

        assertEquals("${widget:test}", action.apply("\\${widget:test}", "ru"));
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
        assertEquals("${en:'Table of contents', ru:'Содержание'}", line("\\${en:'Table of contents', ru:'Содержание'}", "en", "ru").generateLine("en"));
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
        assertEquals("${A}", line("<!--@A=B-->\\${A}").generateLine("ru"));
    }

    @Test
    public void testEscapeCharacters2() {
        TemplateLine line = line("<!--@AA=BB--><!--@CC=\\${AA}-->${CC}");

        assertEquals("BB", line.getProperty("AA", "ru"));
        assertEquals("\\${AA}", line.getProperty("CC", "ru"));
        assertEquals("${AA}", line.generateLine("ru"));
    }

    @Test
    public void testEscapeCharacters3() {
        assertEquals("<!--@A=B-->", line("<!--\\@A=B-->").generateLine("ru"));
    }

    @Test
    public void testEscapeCharacters4() {
        assertEquals("AA<!--ru-->", line("AA<\\!--ru-->").generateLine("ru"));
    }
}