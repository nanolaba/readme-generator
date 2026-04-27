package com.nanolaba.nrg.core.freeze;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable value describing one parsed {@code <!--nrg.freeze ... -->} … {@code <!--/nrg.freeze-->}
 * pair found in a body of text.
 *
 * <p>Carries the parsed {@code id} (required, never null), the optional {@code source-lang}
 * attribute, the zero-based line indices of the open and close markers, and the verbatim
 * marker lines so that they can be written back to output preserving original whitespace.
 * Content lines between open and close are not stored here — callers re-extract them from
 * the body using the indices when needed.
 */
public final class FreezeMarker {

    private final String id;
    private final String sourceLang;
    private final int openLineIndex;
    private final int closeLineIndex;
    private final String openLine;
    private final String closeLine;

    public FreezeMarker(String id, String sourceLang,
                        int openLineIndex, int closeLineIndex,
                        String openLine, String closeLine) {
        this.id = Objects.requireNonNull(id, "id");
        this.sourceLang = sourceLang;
        this.openLineIndex = openLineIndex;
        this.closeLineIndex = closeLineIndex;
        this.openLine = openLine;
        this.closeLine = closeLine;
    }

    public String getId() {
        return id;
    }

    public Optional<String> getSourceLang() {
        return Optional.ofNullable(sourceLang);
    }

    public int getOpenLineIndex() {
        return openLineIndex;
    }

    public int getCloseLineIndex() {
        return closeLineIndex;
    }

    public String getOpenLine() {
        return openLine;
    }

    public String getCloseLine() {
        return closeLine;
    }
}
