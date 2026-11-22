package com.nanolaba.nrg.core.freeze;

import com.nanolaba.nrg.core.Validator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Template-side validation pass for freeze blocks. Re-uses {@link FreezeBlockParser} to
 * surface authoring mistakes (missing or duplicate ids, unbalanced markers, nesting,
 * unknown attributes), then layers on a higher-level check that {@code source-lang}
 * names a language declared in {@code nrg.languages} for the file under inspection.
 */
public final class FreezeValidator {

    private FreezeValidator() {
    }

    /**
     * A {@code null} body yields no diagnostics (the parser treats it as empty). A {@code null}
     * {@code declaredLanguages} skips the {@code source-lang} check altogether: with nothing
     * declared there is nothing to check against.
     */
    public static List<Validator.Diagnostic> validate(File file, String body, List<String> declaredLanguages) {
        FreezeBlockParser.Result parsed = FreezeBlockParser.parse(body, file);
        List<Validator.Diagnostic> out = new ArrayList<>(parsed.getDiagnostics());

        for (FreezeMarker m : parsed.getMarkers()) {
            if (m.getSourceLang().isPresent()) {
                String lang = m.getSourceLang().get();
                if (declaredLanguages != null && !declaredLanguages.contains(lang)) {
                    out.add(new Validator.Diagnostic(file, m.getOpenLineIndex() + 1, Validator.Severity.ERROR,
                            "freeze-block", "freeze id='" + m.getId() + "' at line " + (m.getOpenLineIndex() + 1)
                                    + ": source-lang '" + lang + "' is not declared in nrg.languages "
                                    + "(available: " + String.join(", ", declaredLanguages) + ")",
                            null));
                }
            }
        }
        return out;
    }
}
