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

    public static List<Validator.Diagnostic> validate(File file, String body, List<String> declaredLanguages) {
        List<Validator.Diagnostic> out = new ArrayList<>();
        if (body == null) return out;

        FreezeBlockParser.Result parsed = FreezeBlockParser.parse(body, file);
        out.addAll(parsed.getDiagnostics());

        for (FreezeMarker m : parsed.getMarkers()) {
            if (m.getSourceLang().isPresent()) {
                String lang = m.getSourceLang().get();
                if (declaredLanguages != null && !declaredLanguages.contains(lang)) {
                    out.add(new Validator.Diagnostic(file, m.getOpenLineIndex() + 1,
                            "freeze id='" + m.getId() + "' at line " + (m.getOpenLineIndex() + 1)
                                    + ": source-lang '" + lang + "' is not declared in nrg.languages "
                                    + "(available: " + (declaredLanguages == null
                                            ? "<none>" : String.join(", ", declaredLanguages)) + ")",
                            Validator.Severity.ERROR));
                }
            }
        }
        return out;
    }
}
