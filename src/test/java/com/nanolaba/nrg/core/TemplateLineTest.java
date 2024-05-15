package com.nanolaba.nrg.core;

import com.nanolaba.nrg.widgets.NRGWidget;
import com.nanolaba.nrg.widgets.WidgetTag;
import org.junit.jupiter.api.Test;

import java.util.function.BiFunction;
import java.util.function.Function;

import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_LANGUAGES;
import static org.junit.jupiter.api.Assertions.*;

class TemplateLineTest {

    private TemplateLine line(String line) {
        return new TemplateLine(new GeneratorConfig("<!--" + PROPERTY_LANGUAGES + "=ru-->"), line);
    }

    @Test
    public void testLineVisibility() {

        assertTrue(line("").isLineVisible("ru"));
        assertTrue(line(" ").isLineVisible("ru"));
        assertTrue(line("test <!--ru-->").isLineVisible("ru"));
        assertTrue(line("test <!-- ru -->").isLineVisible("ru"));
        assertTrue(line("test <!-- ru --> test").isLineVisible("ru"));
        assertTrue(line("test <!-- ru --><!--en-->").isLineVisible("ru"));
        assertTrue(line("<!--ru-->").isLineVisible("ru"));
        assertTrue(line("<!--ru--> test").isLineVisible("ru"));
        assertTrue(line("<!-- ru -->").isLineVisible("ru"));
        assertTrue(line("<!-- ru --> test").isLineVisible("ru"));
        assertTrue(line("<!--  ru  --> test").isLineVisible("ru"));

        assertFalse(line("<!--en-->").isLineVisible("ru"));
        assertFalse(line("<!--en--> test").isLineVisible("ru"));
        assertFalse(line("test <!--en-->").isLineVisible("ru"));
        assertFalse(line("test <!-- en-->").isLineVisible("ru"));
        assertFalse(line("test <!-- en -->").isLineVisible("ru"));
        assertFalse(line("test  <!-- en -->").isLineVisible("ru"));

        assertFalse(line("<!--nrg.someproperty-->").isLineVisible("ru"));
        assertFalse(line("<!--nrg.some property-->").isLineVisible("ru"));
        assertFalse(line("<!--nrg.some=property-->").isLineVisible("ru"));
        assertFalse(line("<!--nrg.some = property-->").isLineVisible("ru"));
        assertFalse(line("<!--  nrg.some  =  property  -->").isLineVisible("ru"));
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
        assertEquals("", action.apply("<!--nrg.test-->"));
        assertEquals("", action.apply("<!--nrg.test -->"));
        assertEquals("", action.apply("<!--nrg.test = test-->"));
        assertEquals("", action.apply("<!-- nrg.test = test --><!--ru-->"));
        assertEquals("123", action.apply("1<!-- nrg.test = test -->2<!--ru-->3"));
        assertEquals("123", action.apply("1<!-- nrg.test  =  test  -->2<!--ru-->3"));
    }

    @Test
    public void testGetWidgetTag() {
        Function<String, WidgetTag> action = s -> line(s).getWidgetTag();

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
            return l.renderWidgets(lang);
        };


        assertEquals("test widget body null", action.apply("${nrg.widget:test}", "ru"));
        assertEquals("test widget body null", action.apply("${nrg.widget:test}", "ru"));
        assertEquals("test widget body ", action.apply("${ nrg.widget:test() }", "ru"));
        assertEquals("test widget body AAA", action.apply("${ nrg.widget:test(AAA) }", "ru"));
        assertEquals("test widget body AAA=123", action.apply("${ nrg.widget:test(AAA=123) }", "ru"));
        assertEquals("test widget body AAA=123, BBB=234", action.apply("${ nrg.widget:test(AAA=123, BBB=234) }", "ru"));
    }
}