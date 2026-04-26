package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.DefaultNRGTest;
import com.nanolaba.nrg.core.Generator;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class AssetWidgetTest extends DefaultNRGTest {

    @Test
    public void testResolvesPerLanguageAsset() {
        Generator generator = new Generator(new File("README.src.md"),
                "<!--@nrg.languages=en,zh,ja-->\n" +
                        "<!--@asset.screenshot.en=./public/show-en.png-->\n" +
                        "<!--@asset.screenshot.zh=./public/show-zh.png-->\n" +
                        "<!--@asset.screenshot.ja=./public/show-ja.png-->\n" +
                        "<img src=\"${widget:asset(name='screenshot')}\" />\n");

        String en = generator.getResult("en").getContent().toString();
        String zh = generator.getResult("zh").getContent().toString();
        String ja = generator.getResult("ja").getContent().toString();

        assertTrue(en.contains("<img src=\"./public/show-en.png\" />"), en);
        assertTrue(zh.contains("<img src=\"./public/show-zh.png\" />"), zh);
        assertTrue(ja.contains("<img src=\"./public/show-ja.png\" />"), ja);
    }

    @Test
    public void testFallsBackToBareAssetWhenNoLanguageOverride() {
        Generator generator = new Generator(new File("README.src.md"),
                "<!--@nrg.languages=en,zh-->\n" +
                        "<!--@asset.logo=./public/logo.png-->\n" +
                        "<!--@asset.logo.zh=./public/logo-zh.png-->\n" +
                        "<img src=\"${widget:asset(name='logo')}\" />\n");

        String en = generator.getResult("en").getContent().toString();
        String zh = generator.getResult("zh").getContent().toString();

        assertTrue(en.contains("<img src=\"./public/logo.png\" />"), en);
        assertTrue(zh.contains("<img src=\"./public/logo-zh.png\" />"), zh);
    }

    @Test
    public void testFallsBackToBareAssetForAllLanguagesWhenNoOverrides() {
        Generator generator = new Generator(new File("README.src.md"),
                "<!--@nrg.languages=en,ru-->\n" +
                        "<!--@asset.banner=./public/banner.png-->\n" +
                        "![banner](${widget:asset(name='banner')})\n");

        String en = generator.getResult("en").getContent().toString();
        String ru = generator.getResult("ru").getContent().toString();

        assertTrue(en.contains("![banner](./public/banner.png)"), en);
        assertTrue(ru.contains("![banner](./public/banner.png)"), ru);
    }

    @Test
    public void testMissingNameLogsError() {
        Generator generator = new Generator(new File("README.src.md"),
                "${widget:asset()}\n");

        generator.getResult("en").getContent().toString();
        assertTrue(getErrAndClear().contains("asset widget: missing required 'name' parameter"));
    }

    @Test
    public void testUnresolvedAssetLogsError() {
        Generator generator = new Generator(new File("README.src.md"),
                "${widget:asset(name='nope')}\n");

        String body = generator.getResult("en").getContent().toString();
        assertFalse(body.contains("nope"), body);
        assertTrue(getErrAndClear().contains("asset widget: no value for asset 'nope'"));
    }
}
