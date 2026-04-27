package com.nanolaba.nrg.widgets;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.core.NRGUtil;

import java.util.Map;

/**
 * {@code ${widget:badge(type='...', ...)}} — renders shields.io-backed status badges as
 * Markdown image links.
 *
 * <p>Six pre-canned types share the widget: {@code maven-central}, {@code license},
 * {@code github-release}, {@code github-stars}, {@code github-workflow}, and
 * {@code custom}. Each type validates its own required parameters; missing or malformed
 * values are logged as errors and the widget renders an empty string.
 */
public class BadgeWidget extends DefaultWidget {

    @Override
    public String getName() {
        return "badge";
    }

    @Override
    public String getBody(WidgetTag widgetTag, GeneratorConfig config, String language) {
        Map<String, String> p = NRGUtil.parseParametersLine(widgetTag.getParameters());

        String type = p.get("type");
        if (type == null || type.isEmpty()) {
            LOG.error("badge widget: missing required 'type' parameter");
            return "";
        }
        switch (type.trim().toLowerCase()) {
            case "maven-central":
                return renderMavenCentral(p);
            case "license":
                return renderLicense(p);
            case "github-release":
                return renderGithubRelease(p);
            case "github-stars":
                return renderGithubStars(p);
            case "github-workflow":
                return renderGithubWorkflow(p);
            case "custom":
                return renderCustom(p);
            default:
                LOG.error("badge widget: unknown type '{}' (expected maven-central|license|github-release|github-stars|github-workflow|custom)", type);
                return "";
        }
    }

    private String renderMavenCentral(Map<String, String> p) {
        String coords = require(p, "coordinates", "maven-central");
        if (coords == null) return "";
        String[] parts = coords.split(":");
        if (parts.length != 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
            LOG.error("badge widget: maven-central 'coordinates' must be 'groupId:artifactId', got '{}'", coords);
            return "";
        }
        String group = parts[0].trim();
        String artifact = parts[1].trim();
        String img = "https://img.shields.io/maven-central/v/" + group + "/" + artifact + "?label=Maven%20Central";
        String link = "https://central.sonatype.com/artifact/" + group + "/" + artifact;
        return "[![Maven Central](" + img + ")](" + link + ")";
    }

    private String renderLicense(Map<String, String> p) {
        String value = require(p, "value", "license");
        if (value == null) return "";
        String encoded = shieldsEscape(value);
        String img = "https://img.shields.io/badge/License-" + encoded + "-blue.svg";
        String alt = "License: " + value;
        String url = p.get("url");
        if (url != null && !url.isEmpty()) {
            return "[![" + alt + "](" + img + ")](" + url + ")";
        }
        return "![" + alt + "](" + img + ")";
    }

    private String renderGithubRelease(Map<String, String> p) {
        String repo = requireRepo(p, "github-release");
        if (repo == null) return "";
        String img = "https://img.shields.io/github/v/release/" + repo;
        String link = "https://github.com/" + repo + "/releases/latest";
        return "[![GitHub release](" + img + ")](" + link + ")";
    }

    private String renderGithubStars(Map<String, String> p) {
        String repo = requireRepo(p, "github-stars");
        if (repo == null) return "";
        String img = "https://img.shields.io/github/stars/" + repo + "?style=social";
        String link = "https://github.com/" + repo;
        return "[![GitHub stars](" + img + ")](" + link + ")";
    }

    private String renderGithubWorkflow(Map<String, String> p) {
        String repo = requireRepo(p, "github-workflow");
        if (repo == null) return "";
        String workflow = require(p, "workflow", "github-workflow");
        if (workflow == null) return "";
        String name = p.get("name");
        if (name == null || name.isEmpty()) {
            name = stripWorkflowExtension(workflow);
        }
        String base = "https://github.com/" + repo + "/actions/workflows/" + workflow;
        String img = base + "/badge.svg";
        String branch = p.get("branch");
        if (branch != null && !branch.isEmpty()) {
            img += "?branch=" + branch;
        }
        return "[![" + name + "](" + img + ")](" + base + ")";
    }

    private static String stripWorkflowExtension(String workflow) {
        int dot = workflow.lastIndexOf('.');
        return dot > 0 ? workflow.substring(0, dot) : workflow;
    }

    private String renderCustom(Map<String, String> p) {
        String label = require(p, "label", "custom");
        String message = require(p, "message", "custom");
        String color = require(p, "color", "custom");
        if (label == null || message == null || color == null) {
            return "";
        }
        String img = "https://img.shields.io/badge/" +
                shieldsEscape(label) + "-" +
                shieldsEscape(message) + "-" +
                shieldsEscape(color) + ".svg";
        String url = p.get("url");
        if (url != null && !url.isEmpty()) {
            return "[![" + label + "](" + img + ")](" + url + ")";
        }
        return "![" + label + "](" + img + ")";
    }

    private String requireRepo(Map<String, String> p, String type) {
        String repo = require(p, "repo", type);
        if (repo == null) return null;
        if (!repo.matches("[\\w.-]+/[\\w.-]+")) {
            LOG.error("badge widget: {} 'repo' must be 'owner/name', got '{}'", type, repo);
            return null;
        }
        return repo;
    }

    private String require(Map<String, String> p, String name, String type) {
        String v = p.get(name);
        if (v == null || v.isEmpty()) {
            LOG.error("badge widget: '{}' requires non-empty parameter '{}'", type, name);
            return null;
        }
        return v;
    }

    /**
     * Encodes a label fragment for shields.io path-segment style: dashes are doubled,
     * underscores are doubled, and spaces become underscores — mirroring the rules at
     * <a href="https://shields.io/badges/static-badge">shields.io static-badge</a>.
     */
    static String shieldsEscape(String s) {
        return s.replace("-", "--").replace("_", "__").replace(" ", "_");
    }
}
