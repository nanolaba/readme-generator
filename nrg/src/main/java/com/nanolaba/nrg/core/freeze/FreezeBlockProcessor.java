package com.nanolaba.nrg.core.freeze;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.core.OutputFileNameResolver;
import com.nanolaba.nrg.core.Validator;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Post-pass on the root generator's already-rendered output that resolves
 * {@code <!--nrg.freeze id="..."-->} … {@code <!--/nrg.freeze-->} blocks against the
 * on-disk output file.
 *
 * <p>For every freeze block found in the rendered output, looks up its raw on-disk
 * content via {@link DiskFreezeIndex}: when {@code source-lang} is set, the index is
 * built from the output file of that language; otherwise from the language being
 * generated. On hit, the block's body in the rendered output is replaced with the disk
 * content; on miss (file absent or id not found) the rendered placeholder is kept and a
 * one-time warning is emitted. Errors discovered in the rendered output (e.g. unbalanced
 * markers — which can only happen if the user wrote them that way in the template,
 * since post-pass runs after rendering) are surfaced as a {@link RuntimeException} so
 * the generator caller can fail the run.
 */
public final class FreezeBlockProcessor {

    private FreezeBlockProcessor() {
    }

    public static String resolve(String renderedOutput, GeneratorConfig config, String language) {
        if (renderedOutput == null || renderedOutput.isEmpty()) {
            return renderedOutput;
        }
        FreezeBlockParser.Result parsed = FreezeBlockParser.parse(renderedOutput, config.getSourceFile());
        if (hasError(parsed.getDiagnostics())) {
            throw new IllegalStateException(
                    "freeze: invalid markers in rendered output: " + Validator.format(parsed.getDiagnostics()));
        }
        if (parsed.getMarkers().isEmpty()) {
            return renderedOutput;
        }

        String[] lines = renderedOutput.split("\\R", -1);
        StringBuilder out = new StringBuilder(renderedOutput.length());

        int cursor = 0;
        for (FreezeMarker m : parsed.getMarkers()) {
            for (int i = cursor; i < m.getOpenLineIndex(); i++) {
                out.append(lines[i]).append('\n');
            }
            out.append(m.getOpenLine()).append('\n');

            File targetFile = resolveTargetFile(m, config, language);
            DiskFreezeIndex idx = config.getDiskFreezeIndex(targetFile);
            Optional<String> diskContent = idx.lookup(m.getId());
            if (diskContent.isPresent()) {
                out.append(diskContent.get());
            } else {
                if (config.getWarnedMissingFreezeIds().add(m.getId())) {
                    LOG.warn("freeze id='{}' not found in {}; using template placeholder",
                            m.getId(), targetFile == null ? "<unresolved>" : targetFile.getName());
                }
                for (int i = m.getOpenLineIndex() + 1; i < m.getCloseLineIndex(); i++) {
                    out.append(lines[i]).append('\n');
                }
            }

            out.append(m.getCloseLine()).append('\n');
            cursor = m.getCloseLineIndex() + 1;
        }
        // Tail
        for (int i = cursor; i < lines.length; i++) {
            out.append(lines[i]);
            if (i < lines.length - 1) out.append('\n');
        }

        // Preserve original trailing newline if input had one.
        if (renderedOutput.endsWith("\n") && out.length() > 0 && out.charAt(out.length() - 1) != '\n') {
            out.append('\n');
        }
        return out.toString();
    }

    private static File resolveTargetFile(FreezeMarker m, GeneratorConfig config, String language) {
        String lang = m.getSourceLang().orElse(language);
        return OutputFileNameResolver.resolve(
                config.getRootSourceFile(), config.getDefaultLanguage(), lang, config.getProperties());
    }

    private static boolean hasError(List<Validator.Diagnostic> diagnostics) {
        for (Validator.Diagnostic d : diagnostics) {
            if (d.isError()) return true;
        }
        return false;
    }
}
