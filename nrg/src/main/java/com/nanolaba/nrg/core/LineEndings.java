package com.nanolaba.nrg.core;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Detects and applies line-ending conventions when NRG writes its output.
 *
 * <p>Without intervention, the generator emits {@link System#lineSeparator()} on every line
 * — LF on Linux/macOS, CRLF on Windows. When an existing on-disk README was written with a
 * different convention (typical when contributors edit on different OSes), the very first
 * regeneration churns every line. This class keeps the output's line separator aligned with
 * what's already on disk (default {@link Mode#AUTO}) or pins it to a fixed convention when
 * the caller passes {@link Mode#LF_ONLY} / {@link Mode#CRLF_ONLY}.
 *
 * <p>Conversion always normalises through LF first ({@code \r\n} → {@code \n}, then
 * {@code \n} → target), so a previously-mixed file is safely rewritten to the chosen
 * convention without leaving stray CRs.
 */
public final class LineEndings {

    public static final String LF = "\n";
    public static final String CRLF = "\r\n";

    public enum Mode {
        AUTO, LF_ONLY, CRLF_ONLY;

        public static Mode parse(String raw) {
            if (raw == null || raw.isEmpty() || "auto".equalsIgnoreCase(raw)) return AUTO;
            if ("lf".equalsIgnoreCase(raw)) return LF_ONLY;
            if ("crlf".equalsIgnoreCase(raw)) return CRLF_ONLY;
            throw new IllegalArgumentException(
                    "Invalid line-ending mode: '" + raw + "' (expected auto|lf|crlf)");
        }
    }

    private LineEndings() {/**/}

    /**
     * Detect the dominant line ending of {@code content}: {@link #CRLF}, {@link #LF},
     * or {@code null} when {@code content} contains no newline at all.
     *
     * <p>A file with both CRLF and bare-LF lines (mixed) resolves to LF — it matches Git's
     * usual normalisation and yields a deterministic rewrite without surprising contributors
     * who left bare LF lines in an otherwise-CRLF file.
     */
    public static String detect(String content) {
        if (content == null || content.isEmpty()) return null;
        int crlf = 0;
        int lfOnly = 0;
        int len = content.length();
        for (int i = 0; i < len; i++) {
            if (content.charAt(i) == '\n') {
                if (i > 0 && content.charAt(i - 1) == '\r') crlf++;
                else lfOnly++;
            }
        }
        if (crlf == 0 && lfOnly == 0) return null;
        if (crlf > 0 && lfOnly == 0) return CRLF;
        return LF;
    }

    /**
     * Read {@code file} with {@code charset} and return its detected line ending, or
     * {@code null} when the file is empty or contains no newline.
     */
    public static String detectFromFile(File file, Charset charset) throws IOException {
        return detect(FileUtils.readFileToString(file, charset));
    }

    /**
     * Convert every line ending in {@code content} to {@code ending}. {@code null}
     * {@code ending} is a no-op (caller has nothing to enforce).
     */
    public static String applyTo(CharSequence content, String ending) {
        if (content == null || content.length() == 0 || ending == null) {
            return content == null ? null : content.toString();
        }
        String normalized = content.toString().replace(CRLF, LF);
        if (LF.equals(ending)) return normalized;
        if (CRLF.equals(ending)) return normalized.replace(LF, CRLF);
        return normalized;
    }

    /**
     * Resolve the line ending to use for {@code targetFile} given {@code mode}:
     * <ul>
     *   <li>{@link Mode#LF_ONLY} → {@link #LF};</li>
     *   <li>{@link Mode#CRLF_ONLY} → {@link #CRLF};</li>
     *   <li>{@link Mode#AUTO} with an existing target file → detected ending (mixed → LF);</li>
     *   <li>{@link Mode#AUTO} with a missing or empty target → {@link System#lineSeparator()}.</li>
     * </ul>
     */
    public static String resolve(Mode mode, File targetFile, Charset charset) throws IOException {
        if (mode == Mode.LF_ONLY) return LF;
        if (mode == Mode.CRLF_ONLY) return CRLF;
        if (targetFile != null && targetFile.isFile()) {
            String detected = detectFromFile(targetFile, charset);
            if (detected != null) return detected;
        }
        return System.lineSeparator();
    }
}
