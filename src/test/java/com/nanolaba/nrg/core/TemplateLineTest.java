package com.nanolaba.nrg.core;

import com.nanolaba.nrg.widgets.NRGWidget;
import com.nanolaba.nrg.widgets.WidgetTag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_LANGUAGES;
import static org.junit.jupiter.api.Assertions.*;

class TemplateLineTest {

    private TemplateLine line(String line) {
        return line(line, "ru");
    }

    private TemplateLine line(String line, String... languages) {
        String configBody = "<!--@" + PROPERTY_LANGUAGES + "=" + String.join(", ", languages) + "-->" + line;
        return new TemplateLine(new GeneratorConfig(new File("README.src.md"), configBody), line, 0);
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
        Function<String, WidgetTag> action = s -> line(s).getWidgetTag(s);

        assertNull(action.apply(""));
        assertNull(action.apply("123"));
        assertNull(action.apply("123<!--test-->"));
        assertNull(action.apply("123${nrg.widget}"));
        assertNull(action.apply("${nrg:widget:languages}"));

        assertEquals("languages", action.apply("${nrg.widget:languages}").getName());
        assertEquals("languages", action.apply("${ nrg.widget:languages }").getName());
        assertEquals("languages", action.apply(" ${ nrg.widget:languages } ").getName());
        assertEquals("languages", action.apply("qwe ${ nrg.widget:languages } 123").getName());

        assertNull(action.apply("${nrg.widget:languages}").getParameters());
        assertEquals("", action.apply("${nrg.widget:languages()}").getParameters());
        assertEquals("parameters", action.apply("${nrg.widget:languages(parameters)}").getParameters());
        assertEquals("a b c", action.apply("${nrg.widget:languages(a b c)}").getParameters());
    }

    @Test
    public void testRenderWidget() {
        BiFunction<String, String, String> action = (s, lang) -> {
            TemplateLine l = line(s);
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

        assertEquals("test widget body null", action.apply("${nrg.widget:test}", "ru"));
        assertEquals("test widget body null", action.apply("${nrg.widget:test}", "ru"));
        assertEquals("test widget body ", action.apply("${ nrg.widget:test() }", "ru"));
        assertEquals("test widget body AAA", action.apply("${ nrg.widget:test(AAA) }", "ru"));
        assertEquals("test widget body AAA=123", action.apply("${ nrg.widget:test(AAA=123) }", "ru"));
        assertEquals("test widget body AAA=123, BBB=234", action.apply("${ nrg.widget:test(AAA=123, BBB=234) }", "ru"));

        assertEquals("test widget body 123", action.apply("${ nrg.widget:test(${ru:'123'}) }", "ru"));
    }

    @Test
    public void testReadProperties() {
        TemplateLine line = line("");
        assertEquals(0, line.getProperties().size());

        line = line("<!--@AAA=BBB-->");
        assertEquals(1, line.getProperties().size());
        assertEquals("BBB", line.getProperties().getProperty("AAA"));

        line = line("<!--@AAA-->");
        assertEquals(1, line.getProperties().size());
        assertEquals("", line.getProperties().getProperty("AAA"));

        line = line("<!--@AAA=BBB-->");
        assertEquals(1, line.getProperties().size());
        assertEquals("BBB", line.getProperties().getProperty("AAA"));

        line = line("<!--@AAA=BBB--><!--@AAA.BBB=CCC.DDD-->");
        assertEquals(2, line.getProperties().size());
        assertEquals("BBB", line.getProperties().getProperty("AAA"));
        assertEquals("CCC.DDD", line.getProperties().getProperty("AAA.BBB"));

        line = line("<!-- @AAA = BBB --><!--@ AAA.BBB =  CCC.DDD  -->");
        assertEquals(2, line.getProperties().size());
        assertEquals("BBB", line.getProperties().getProperty("AAA"));
        assertEquals("CCC.DDD", line.getProperties().getProperty("AAA.BBB"));
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
        assertEquals("", line("${en:'Table of contents'}", "en", "ru").generateLine("ru"));
        assertEquals("Содержание", line("${ru:'Содержание'}", "en", "ru").generateLine("ru"));
        assertEquals("Содержание", line("${en:'Table of contents', ru:'Содержание'}", "en", "ru").generateLine("ru"));
        assertEquals("Table of contents", line("${en:'Table of contents', ru:'Содержание'}", "en", "ru").generateLine("en"));
    }
}