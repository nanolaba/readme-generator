package com.nanolaba.nrg.widgets;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.core.NRGUtil;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class ExecWidget extends DefaultWidget {

    static final long DEFAULT_TIMEOUT_SECONDS = 30L;
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final CommandRunner runner;

    public ExecWidget() {
        this(new ProcessBuilderRunner());
    }

    ExecWidget(CommandRunner runner) {
        this.runner = runner;
    }

    @Override
    public String getName() {
        return "exec";
    }

    @Override
    public String getBody(WidgetTag widgetTag, GeneratorConfig config, String language) {
        Map<String, String> params = NRGUtil.parseParametersLine(widgetTag.getParameters());

        String cmd = params.get("cmd");
        if (cmd == null || cmd.trim().isEmpty()) {
            LOG.error("exec widget: missing required 'cmd' parameter");
            return "";
        }
        if (!config.isExecAllowed()) {
            LOG.error("exec widget: execution is disabled; pass --allow-exec (or set <allowExec>true</allowExec> in the Maven plugin) to enable");
            return "";
        }

        List<String> argv = Arrays.asList(WHITESPACE.split(cmd.trim()));

        String trimValue = params.getOrDefault("trim", "true");
        if (!"true".equals(trimValue) && !"false".equals(trimValue)) {
            LOG.error("exec widget: invalid 'trim' value '{}' (expected true|false)", trimValue);
            return "";
        }
        boolean trim = "true".equals(trimValue);

        long timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
        String timeoutRaw = params.get("timeout");
        if (timeoutRaw != null) {
            try {
                timeoutSeconds = Long.parseLong(timeoutRaw.trim());
            } catch (NumberFormatException e) {
                LOG.error("exec widget: invalid 'timeout' value '{}' (expected positive integer seconds)", timeoutRaw);
                return "";
            }
            if (timeoutSeconds <= 0) {
                LOG.error("exec widget: invalid 'timeout' value '{}' (expected positive integer seconds)", timeoutRaw);
                return "";
            }
        }

        File cwd = resolveCwd(config, params.get("cwd"));
        if (cwd == null) {
            return "";
        }

        ExecResult result;
        try {
            result = runner.run(argv, cwd, timeoutSeconds);
        } catch (IOException e) {
            LOG.error(e, () -> "exec widget: failed to run command " + argv);
            return "";
        }

        if (result.timedOut) {
            LOG.warn("exec widget: command {} timed out after {} seconds", argv, timeoutSeconds);
            return "";
        }
        if (result.exitCode != 0) {
            LOG.error("exec widget: command failed with exit code {}: {} (stderr: {})",
                    result.exitCode, argv, snippet(result.stderr));
            return "";
        }

        String stdout = result.stdout == null ? "" : result.stdout;
        if (trim) {
            stdout = stripTrailing(stdout);
        }

        if (params.containsKey("codeblock")) {
            String lang = params.get("codeblock");
            return "```" + (lang == null ? "" : lang) + System.lineSeparator() +
                    stdout + System.lineSeparator() + "```";
        }
        return stdout;
    }

    private static File resolveCwd(GeneratorConfig config, String raw) {
        File base = config.getSourceFile().getAbsoluteFile().getParentFile();
        if (raw == null || raw.isEmpty()) {
            return base;
        }
        File requested = new File(raw);
        File resolved = requested.isAbsolute() ? requested : new File(base, raw);
        if (!resolved.isDirectory()) {
            LOG.error("exec widget: cwd not found: {}", resolved.getAbsolutePath());
            return null;
        }
        return resolved;
    }

    private static String stripTrailing(String s) {
        int end = s.length();
        while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) {
            end--;
        }
        return s.substring(0, end);
    }

    private static String snippet(String s) {
        if (s == null) return "";
        String one = s.replace("\r\n", " ").replace('\n', ' ').trim();
        return one.length() > 200 ? one.substring(0, 200) + "…" : one;
    }

    public interface CommandRunner {
        ExecResult run(List<String> argv, File cwd, long timeoutSeconds) throws IOException;
    }

    public static final class ExecResult {
        public final int exitCode;
        public final String stdout;
        public final String stderr;
        public final boolean timedOut;

        private ExecResult(int exitCode, String stdout, String stderr, boolean timedOut) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
            this.timedOut = timedOut;
        }

        public static ExecResult ok(String stdout, String stderr) {
            return new ExecResult(0, stdout, stderr, false);
        }

        public static ExecResult failed(int exitCode, String stdout, String stderr) {
            return new ExecResult(exitCode, stdout, stderr, false);
        }

        public static ExecResult timeout() {
            return new ExecResult(-1, "", "", true);
        }
    }

    private static final class ProcessBuilderRunner implements CommandRunner {
        @Override
        public ExecResult run(List<String> argv, File cwd, long timeoutSeconds) throws IOException {
            ProcessBuilder pb = new ProcessBuilder(argv);
            pb.directory(cwd);
            pb.redirectErrorStream(false);
            Process p = pb.start();
            try {
                boolean finished = p.waitFor(timeoutSeconds, TimeUnit.SECONDS);
                if (!finished) {
                    p.destroyForcibly();
                    return ExecResult.timeout();
                }
                String out = readAll(p.getInputStream());
                String err = readAll(p.getErrorStream());
                int exit = p.exitValue();
                return exit == 0 ? ExecResult.ok(out, err) : ExecResult.failed(exit, out, err);
            } catch (InterruptedException e) {
                p.destroyForcibly();
                Thread.currentThread().interrupt();
                throw new IOException("interrupted while waiting for command", e);
            }
        }

        private static String readAll(InputStream in) throws IOException {
            try {
                return IOUtils.toString(in, StandardCharsets.UTF_8);
            } finally {
                in.close();
            }
        }
    }
}
