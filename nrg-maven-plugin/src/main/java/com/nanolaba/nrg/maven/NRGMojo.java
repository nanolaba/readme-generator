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

@Mojo(name = "create-files", defaultPhase = LifecyclePhase.COMPILE)
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

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (files == null || files.length == 0) {
            getLog().warn("No template files specified");
            return;
        }

        String widgetsArg = validateAndJoinWidgets();

        boolean anyCheckFailure = false;
        boolean anyValidateFailure = false;
        for (String file : files) {
            List<String> args = new ArrayList<>();
            args.add("-f");
            args.add(file);
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
            int code = NRG.run(args.toArray(new String[0]));
            if (validate && code != 0) {
                anyValidateFailure = true;
            } else if (check && code != 0) {
                anyCheckFailure = true;
            }
        }

        if (anyValidateFailure) {
            throw new MojoExecutionException(
                    "Template validation reported errors (see diagnostics above). " +
                            "Fix the source templates or remove <validate>.");
        }
        if (anyCheckFailure) {
            throw new MojoExecutionException(
                    "Generated output differs from files on disk (see diff above). " +
                            "Regenerate README files by running the plugin without <check>.");
        }
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
