package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.DefaultNRGTest;
import com.nanolaba.nrg.core.Generator;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MathWidgetTest extends DefaultNRGTest {

    @Test
    public void testMissingExprLogsErrorAndEmitsNothing() {
        Generator generator = new Generator(new File("README.src.md"),
                "before\n${widget:math()}\nafter");

        String body = generator.getResult("en").getContent().toString();
        assertTrue(body.contains("before"));
        assertTrue(body.contains("after"));
        assertFalse(body.contains("$"), body);
        assertTrue(getErrAndClear().contains("math widget: "));
    }

    @Test
    public void testInlineNativeDefault() {
        Generator generator = new Generator(new File("README.src.md"),
                "pi is ${widget:math(expr='\\\\pi')} radians");

        String body = generator.getResult("en").getContent().toString();
        assertTrue(body.contains("pi is $\\pi$ radians"), body);
    }

    @Test
    public void testBlockNative() {
        Generator generator = new Generator(new File("README.src.md"),
                "${widget:math(expr='x^2', display='block')}");

        String body = generator.getResult("en").getContent().toString();
        assertTrue(body.contains("$$x^2$$"), body);
    }

    @Test
    public void testBackslashEscapeInExpr() {
        Generator generator = new Generator(new File("README.src.md"),
                "${widget:math(expr='\\\\sum_{i=0}^{n} x_i')}");

        String body = generator.getResult("en").getContent().toString();
        assertTrue(body.contains("$\\sum_{i=0}^{n} x_i$"), body);
    }

    @Test
    public void testInvalidDisplayLogsErrorAndEmitsNothing() {
        Generator generator = new Generator(new File("README.src.md"),
                "before\n${widget:math(expr='x', display='oops')}\nafter");

        String body = generator.getResult("en").getContent().toString();
        assertTrue(body.contains("before"));
        assertTrue(body.contains("after"));
        assertFalse(body.contains("$x$"), body);
        assertTrue(getErrAndClear().contains("math widget: invalid display"));
    }

    @Test
    public void testInvalidRendererLogsErrorAndEmitsNothing() {
        Generator generator = new Generator(new File("README.src.md"),
                "${widget:math(expr='x', renderer='mathml')}");

        generator.getResult("en").getContent().toString();
        assertTrue(getErrAndClear().contains("math widget: invalid renderer"));
    }

    @Test
    public void testSvgInlineUsesCodecogsByDefault() {
        Generator generator = new Generator(new File("README.src.md"),
                "${widget:math(expr='\\\\pi', renderer='svg')}");

        String body = generator.getResult("en").getContent().toString();
        assertTrue(body.contains("![\\pi](https://latex.codecogs.com/svg.image?%5Cpi)"), body);
    }

    @Test
    public void testSvgBlockPrependsDisplaystyle() {
        Generator generator = new Generator(new File("README.src.md"),
                "${widget:math(expr='x^2', renderer='svg', display='block')}");

        String body = generator.getResult("en").getContent().toString();
        assertTrue(body.contains("https://latex.codecogs.com/svg.image?%5Cdisplaystyle+x%5E2"), body);
        assertTrue(body.contains("![x^2]"), body);
    }

    @Test
    public void testSvgCustomService() {
        Generator generator = new Generator(new File("README.src.md"),
                "${widget:math(expr='x', renderer='svg', service='https://render.example/?f=')}");

        String body = generator.getResult("en").getContent().toString();
        assertTrue(body.contains("![x](https://render.example/?f=x)"), body);
    }

    @Test
    public void testSvgAltOverride() {
        Generator generator = new Generator(new File("README.src.md"),
                "${widget:math(expr='\\\\Phi', renderer='svg', alt='Phi')}");

        String body = generator.getResult("en").getContent().toString();
        assertTrue(body.contains("![Phi](https://latex.codecogs.com/svg.image?%5CPhi)"), body);
    }

    @Test
    public void testLatexWithBracesPassesThrough() {
        Generator generator = new Generator(new File("README.src.md"),
                "${widget:math(expr='\\\\Phi_{\\\\text{org}}')}");

        String body = generator.getResult("en").getContent().toString();
        assertTrue(body.contains("$\\Phi_{\\text{org}}$"), body);
    }

    @Test
    public void testLanguageSubstitutionInAlt() {
        Generator generator = new Generator(new File("README.src.md"),
                "<!--@nrg.languages=en,ru-->\n" +
                        "${widget:math(expr='x', renderer='svg', alt=\"${en:'x axis', ru:'ось X'}\")}");

        String en = generator.getResult("en").getContent().toString();
        String ru = generator.getResult("ru").getContent().toString();
        assertTrue(en.contains("![x axis]"), en);
        assertTrue(ru.contains("![ось X]"), ru);
    }
}
