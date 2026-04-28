package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.DefaultNRGTest;
import com.nanolaba.nrg.core.Generator;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class BadgeWidgetTest extends DefaultNRGTest {

    private String render(String widgetCall) {
        Generator g = new Generator(new File("README.src.md"), widgetCall);
        return g.getResult("en").getContent().toString();
    }

    @Test
    public void testMavenCentral() {
        String body = render("${widget:badge(type='maven-central', coordinates='com.nanolaba:readme-generator')}");
        assertTrue(body.contains("[![Maven Central]"), body);
        assertTrue(body.contains("https://img.shields.io/maven-central/v/com.nanolaba/readme-generator?label=Maven%20Central"));
        assertTrue(body.contains("https://central.sonatype.com/artifact/com.nanolaba/readme-generator"));
    }

    @Test
    public void testMavenCentralInvalidCoordinates() {
        String body = render("${widget:badge(type='maven-central', coordinates='com.nanolaba')}");
        assertFalse(body.contains("shields.io"));
        assertTrue(getErrAndClear().contains("must be 'groupId:artifactId'"));
    }

    @Test
    public void testMavenCentralMissingCoordinates() {
        String body = render("${widget:badge(type='maven-central')}");
        assertFalse(body.contains("shields.io"));
        assertTrue(getErrAndClear().contains("requires non-empty parameter 'coordinates'"));
    }

    @Test
    public void testLicenseWithoutUrl() {
        String body = render("${widget:badge(type='license', value='Apache-2.0')}");
        assertTrue(body.contains("![License: Apache-2.0]"));
        assertTrue(body.contains("https://img.shields.io/badge/License-Apache--2.0-blue.svg"));
        assertFalse(body.contains("[!["), "non-clickable license must not wrap in []");
    }

    @Test
    public void testLicenseWithUrl() {
        String body = render("${widget:badge(type='license', value='Apache-2.0', url='https://apache.org/licenses/LICENSE-2.0')}");
        assertTrue(body.contains("[![License: Apache-2.0]"));
        assertTrue(body.contains("](https://apache.org/licenses/LICENSE-2.0)"));
    }

    @Test
    public void testGithubRelease() {
        String body = render("${widget:badge(type='github-release', repo='nanolaba/readme-generator')}");
        assertTrue(body.contains("[![GitHub release]"));
        assertTrue(body.contains("https://img.shields.io/github/v/release/nanolaba/readme-generator"));
        assertTrue(body.contains("https://github.com/nanolaba/readme-generator/releases/latest"));
    }

    @Test
    public void testGithubStars() {
        String body = render("${widget:badge(type='github-stars', repo='nanolaba/readme-generator')}");
        assertTrue(body.contains("[![GitHub stars]"));
        assertTrue(body.contains("https://img.shields.io/github/stars/nanolaba/readme-generator?style=social"));
        assertTrue(body.contains("](https://github.com/nanolaba/readme-generator)"));
    }

    @Test
    public void testGithubInvalidRepo() {
        String body = render("${widget:badge(type='github-release', repo='not-a-slash-path')}");
        assertFalse(body.contains("shields.io"));
        assertTrue(getErrAndClear().contains("must be 'owner/name'"));
    }

    @Test
    public void testCustomWithoutUrl() {
        String body = render("${widget:badge(type='custom', label='docs', message='up to date', color='brightgreen')}");
        assertTrue(body.contains("![docs]"));
        assertTrue(body.contains("https://img.shields.io/badge/docs-up_to_date-brightgreen.svg"));
        assertFalse(body.contains("[!["));
    }

    @Test
    public void testCustomWithUrl() {
        String body = render("${widget:badge(type='custom', label='CI', message='passing', color='green', url='https://ci.example.com')}");
        assertTrue(body.contains("[![CI]"));
        assertTrue(body.contains("](https://ci.example.com)"));
    }

    @Test
    public void testCustomMissingRequiredParam() {
        String body = render("${widget:badge(type='custom', label='X', message='Y')}");
        assertFalse(body.contains("shields.io"));
        assertTrue(getErrAndClear().contains("'custom' requires non-empty parameter 'color'"));
    }

    @Test
    public void testUnknownTypeLogsError() {
        String body = render("${widget:badge(type='foobar', coordinates='x:y')}");
        assertFalse(body.contains("shields.io"));
        assertTrue(getErrAndClear().contains("unknown type 'foobar'"));
    }

    @Test
    public void testMissingTypeLogsError() {
        String body = render("${widget:badge(coordinates='x:y')}");
        assertFalse(body.contains("shields.io"));
        assertTrue(getErrAndClear().contains("missing required 'type' parameter"));
    }

    @Test
    public void testGithubWorkflowWithExplicitName() {
        String body = render("${widget:badge(type='github-workflow', repo='nanolaba/readme-generator', workflow='ci.yml', name='CI')}");
        assertTrue(body.contains(
                "[![CI](https://github.com/nanolaba/readme-generator/actions/workflows/ci.yml/badge.svg)]" +
                        "(https://github.com/nanolaba/readme-generator/actions/workflows/ci.yml)"), body);
    }

    @Test
    public void testGithubWorkflowDefaultsNameToFilenameWithoutExtension() {
        String body = render("${widget:badge(type='github-workflow', repo='nanolaba/readme-generator', workflow='ci.yml')}");
        assertTrue(body.contains("[![ci]"), body);
    }

    @Test
    public void testGithubWorkflowWithBranchQuery() {
        String body = render("${widget:badge(type='github-workflow', repo='nanolaba/readme-generator', workflow='ci.yml', branch='main', name='CI')}");
        assertTrue(body.contains("actions/workflows/ci.yml/badge.svg?branch=main"), body);
    }

    @Test
    public void testGithubWorkflowMissingWorkflow() {
        String body = render("${widget:badge(type='github-workflow', repo='nanolaba/readme-generator')}");
        assertFalse(body.contains("shields.io") || body.contains("badge.svg"));
        assertTrue(getErrAndClear().contains("requires non-empty parameter 'workflow'"));
    }

    @Test
    public void testGithubWorkflowInvalidRepo() {
        String body = render("${widget:badge(type='github-workflow', repo='bad', workflow='ci.yml')}");
        assertFalse(body.contains("badge.svg"));
        assertTrue(getErrAndClear().contains("must be 'owner/name'"));
    }

    @Test
    public void testShieldsEscapeUnit() {
        assertEquals("Apache--2.0", BadgeWidget.shieldsEscape("Apache-2.0"));
        assertEquals("foo__bar", BadgeWidget.shieldsEscape("foo_bar"));
        assertEquals("up_to_date", BadgeWidget.shieldsEscape("up to date"));
        assertEquals("a--b__c_d", BadgeWidget.shieldsEscape("a-b_c d"));
    }

    // ---------------------------------------------------------------------
    // alt= parameter (issue #52): every type accepts an optional alt= that
    // overrides the type-specific default. Empty alt='' falls back to default.
    // ---------------------------------------------------------------------

    @Test
    public void testMavenCentralAltOverride() {
        String body = render("${widget:badge(type='maven-central', coordinates='com.nanolaba:readme-generator', alt='NRG on Maven Central')}");
        assertTrue(body.contains("[![NRG on Maven Central]"), body);
        assertFalse(body.contains("[![Maven Central]"), body);
    }

    @Test
    public void testLicenseAltOverride() {
        String body = render("${widget:badge(type='license', value='Apache-2.0', alt='NRG license: Apache 2.0')}");
        assertTrue(body.contains("![NRG license: Apache 2.0]"), body);
        assertFalse(body.contains("![License: Apache-2.0]"), body);
    }

    @Test
    public void testGithubReleaseAltOverride() {
        String body = render("${widget:badge(type='github-release', repo='nanolaba/readme-generator', alt='Latest NRG release')}");
        assertTrue(body.contains("[![Latest NRG release]"), body);
        assertFalse(body.contains("[![GitHub release]"), body);
    }

    @Test
    public void testGithubStarsAltOverride() {
        String body = render("${widget:badge(type='github-stars', repo='nanolaba/readme-generator', alt='Star NRG on GitHub')}");
        assertTrue(body.contains("[![Star NRG on GitHub]"), body);
        assertFalse(body.contains("[![GitHub stars]"), body);
    }

    @Test
    public void testGithubWorkflowAltOverride() {
        // alt= overrides only the markdown alt; name= still controls what defaults
        // would have produced, so this case proves alt= wins over name=.
        String body = render("${widget:badge(type='github-workflow', repo='nanolaba/readme-generator', workflow='ci.yml', name='CI', alt='NRG continuous integration build status')}");
        assertTrue(body.contains("[![NRG continuous integration build status](https://github.com/nanolaba/readme-generator/actions/workflows/ci.yml/badge.svg)]"), body);
        assertFalse(body.contains("[![CI]"), body);
    }

    @Test
    public void testCustomAltOverride() {
        String body = render("${widget:badge(type='custom', label='CI', message='passing', color='green', url='https://ci.example.com', alt='NRG CI status: passing')}");
        assertTrue(body.contains("[![NRG CI status: passing]"), body);
        assertFalse(body.contains("[![CI]"), body);
    }

    @Test
    public void testEmptyAltFallsBackToDefault() {
        // Empty alt='' is treated as not provided, so the default is used. This keeps
        // round-tripping a template through a value-substitution pipeline safe — an
        // unset variable resolving to '' should not break the badge.
        String body = render("${widget:badge(type='maven-central', coordinates='com.nanolaba:readme-generator', alt='')}");
        assertTrue(body.contains("[![Maven Central]"), body);
    }
}
