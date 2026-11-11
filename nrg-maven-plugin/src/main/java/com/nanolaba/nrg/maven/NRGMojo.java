package com.nanolaba.nrg.maven;

import com.nanolaba.nrg.NRG;
import com.nanolaba.nrg.widgets.NRGWidget;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.List;

/**
 * Maven goal {@code nrg:create-files} — wraps the {@link NRG} CLI so README templates can
 * be regenerated as part of a Maven build (default phase: {@code compile}).
 *
 * <p>The Mojo is intentionally a thin shim: parameter values are translated to the
 * matching CLI flags and passed through {@link NRG#run(String...)}. The only logic that
 * lives here is up-front validation of the {@code <widgets>} list (existence, supertype,
 * no-arg constructor) so a misconfigured POM fails fast with a {@code MojoExecutionException}
 * instead of an obscure CLI error per file. {@code <check>} and {@code <validate>} failures
 * across all template files are aggregated into a single failure at the end so authors see
 * every diagnostic before the build halts.
 *
 * <p>Each {@code <file>} entry may be a literal path or a {@code glob:}-style pattern; the
 * plugin forwards them to the CLI as positional arguments and lets {@link NRG} resolve
 * them in one batch (see {@code com.nanolaba.nrg.core.SourceFileResolver}). Set
 * {@code <failFast>true</failFast>} to abort on the first non-zero result instead of
 * aggregating diagnostics across all matched files.
 *
 * <p>{@code inheritByDefault = false}: README generation is a root-of-repo concern, so
 * child modules of an aggregator project do <strong>not</strong> re-run the goal in their
 * own working directory (where a {@code README.src.md} typically does not exist and would
 * either log warnings or fail). A child module that genuinely needs to run NRG must opt
 * in explicitly via {@code <inherited>true</inherited>} in its own POM.
 */
@Mojo(name = "create-files", defaultPhase = LifecyclePhase.COMPILE, inheritByDefault = false)
public class NRGMojo extends AbstractMojo {

    private String[] files = new String[]{"README.src.md"};

    @Parameter(property = "charset")
    private String charset = "UTF-8";

    @Parameter(property = "logLevel")
    private String logLevel;

    @Parameter
    private List<String> widgets;

    @Parameter(property = "check", defaultValue = "false")
    private boolean check;

    @Parameter(property = "allowExec", defaultValue = "false")
    private boolean allowExec;

    @Parameter(property = "validate", defaultValue = "false")
    private boolean validate;

    @Parameter(property = "failFast", defaultValue = "false")
    private boolean failFast;

    @Parameter(property = "fileNamePattern")
    private String fileNamePattern;

    @Parameter(property = "defaultLanguageFileNamePattern")
    private String defaultLanguageFileNamePattern;

    @Parameter(property = "lineEnding")
    private String lineEnding;

    @Parameter(property = "noHeader", defaultValue = "false")
    private boolean noHeader;

    @Parameter(property = "headerText")
    private String headerText;

    @Parameter
    private List<String> checkPaths;

    @Parameter(property = "root")
    private String root;

    /**
     * Output style passed to {@code --format}: {@code text} (default), {@code json} (one JSON
     * report on stdout) or {@code github} (GitHub Actions workflow annotations).
     */
    @Parameter(property = "format")
    private String format;

    /** {@code --sandbox}: no environment variables, no remote imports; rejects {@code <allowExec>}. */
    @Parameter(property = "sandbox", defaultValue = "false")
    private boolean sandbox;

    /** {@code --no-env}: every {@code ${env.NAME}} renders its fallback or empty. */
    @Parameter(property = "noEnv", defaultValue = "false")
    private boolean noEnv;

    /** {@code --env-allow}: the only environment variables templates may read (comma-separated, {@code PREFIX_*}). */
    @Parameter(property = "envAllow")
    private String envAllow;

    /** {@code --offline}: refuse remote imports whatever the template says. */
    @Parameter(property = "offline", defaultValue = "false")
    private boolean offline;

    public void setFileNamePattern(String fileNamePattern) {
        this.fileNamePattern = fileNamePattern;
    }

    public void setDefaultLanguageFileNamePattern(String defaultLanguageFileNamePattern) {
        this.defaultLanguageFileNamePattern = defaultLanguageFileNamePattern;
    }

    public void setLineEnding(String lineEnding) {
        this.lineEnding = lineEnding;
    }

    public void setNoHeader(boolean noHeader) {
        this.noHeader = noHeader;
    }

    public void setHeaderText(String headerText) {
        this.headerText = headerText;
    }

    public void setCheckPaths(List<String> checkPaths) {
        this.checkPaths = checkPaths;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    @Parameter(property = "file")
    public void setFile(String[] files) {
        this.files = files;
    }

    public void setWidgets(List<String> widgets) {
        this.widgets = widgets;
    }

    public void setCheck(boolean check) {
        this.check = check;
    }

    public void setAllowExec(boolean allowExec) {
        this.allowExec = allowExec;
    }

    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setSandbox(boolean sandbox) {
        this.sandbox = sandbox;
    }

    public void setEnvAllow(String envAllow) {
        this.envAllow = envAllow;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (files == null || files.length == 0) {
            getLog().warn("No template files specified");
            return;
        }

        String widgetsArg = validateAndJoinWidgets();

        boolean hasCheckPathsFilter = false;
        if (checkPaths != null) {
            for (String p : checkPaths) {
                if (p != null && !p.isEmpty()) {
                    hasCheckPathsFilter = true;
                    break;
                }
            }
        }
        if (hasCheckPathsFilter && !check) {
            throw new MojoExecutionException("<checkPaths> requires <check>true</check>");
        }

        List<String> args = new ArrayList<>();
        args.add("--charset");
        args.add(charset);
        if (logLevel != null && !logLevel.isEmpty()) {
            args.add("--log-level");
            args.add(logLevel);
        }
        if (widgetsArg != null) {
            args.add("--widgets");
            args.add(widgetsArg);
        }
        if (validate) {
            args.add("--validate");
        } else if (check) {
            args.add("--check");
        }
        if (allowExec) {
            args.add("--allow-exec");
        }
        if (failFast) {
            args.add("--fail-fast");
        }
        if (fileNamePattern != null && !fileNamePattern.isEmpty()) {
            args.add("--file-name-pattern");
            args.add(fileNamePattern);
        }
        if (defaultLanguageFileNamePattern != null && !defaultLanguageFileNamePattern.isEmpty()) {
            args.add("--default-language-file-name-pattern");
            args.add(defaultLanguageFileNamePattern);
        }
        if (lineEnding != null && !lineEnding.isEmpty()) {
            args.add("--line-ending");
            args.add(lineEnding);
        }
        if (noHeader) {
            args.add("--no-header");
        }
        if (headerText != null && !headerText.isEmpty()) {
            args.add("--header-text");
            args.add(headerText);
        }
        if (root != null && !root.isEmpty()) {
            args.add("--root");
            args.add(root);
        }
        if (format != null && !format.isEmpty()) {
            args.add("--format");
            args.add(format);
        }
        if (sandbox) {
            args.add("--sandbox");
        }
        if (noEnv) {
            args.add("--no-env");
        }
        if (envAllow != null && !envAllow.isEmpty()) {
            args.add("--env-allow");
            args.add(envAllow);
        }
        if (offline) {
            args.add("--offline");
        }
        if (checkPaths != null) {
            for (String p : checkPaths) {
                if (p != null && !p.isEmpty()) {
                    args.add("--check-paths");
                    args.add(p);
                }
            }
        }
        // Positional file arguments must follow all flags (commons-cli stops parsing options at
        // the first non-option token).
        for (String f : files) {
            if (f != null && !f.isEmpty()) args.add(f);
        }

        int code = NRG.run(args.toArray(new String[0]));
        if (code == NRG.EXIT_OK) {
            return;
        }
        if (code == NRG.EXIT_USAGE) {
            throw new MojoExecutionException(
                    "Invalid plugin configuration or missing source files (see errors above).");
        }
        if (validate) {
            throw new MojoExecutionException(
                    "Template validation reported errors (see diagnostics above). " +
                            "Fix the source templates or remove <validate>.");
        }
        if (check && code == NRG.EXIT_DRIFT) {
            throw new MojoExecutionException(
                    "Generated output differs from files on disk (see diff above). " +
                            "Regenerate README files by running the plugin without <check>.");
        }
        throw new MojoExecutionException("Template generation failed (see errors above).");
    }

    private String validateAndJoinWidgets() throws MojoExecutionException {
        if (widgets == null || widgets.isEmpty()) {
            return null;
        }
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        StringBuilder joined = new StringBuilder();
        for (String rawFqcn : widgets) {
            if (rawFqcn == null) continue;
            String fqcn = rawFqcn.trim();
            if (fqcn.isEmpty()) continue;

            Class<?> cls;
            try {
                cls = Class.forName(fqcn, false, classLoader);
            } catch (ClassNotFoundException e) {
                throw new MojoExecutionException(
                        "Widget class not found: '" + fqcn + "'. " +
                                "Add the artifact containing this class as a <dependency> of the nrg-maven-plugin declaration.");
            }
            if (!NRGWidget.class.isAssignableFrom(cls)) {
                throw new MojoExecutionException(
                        "Widget class '" + fqcn + "' does not implement " + NRGWidget.class.getName());
            }
            try {
                cls.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                throw new MojoExecutionException(
                        "Widget class '" + fqcn + "' must declare a public no-argument constructor");
            }

            if (joined.length() > 0) {
                joined.append(',');
            }
            joined.append(fqcn);
        }
        return joined.length() == 0 ? null : joined.toString();
    }
}
