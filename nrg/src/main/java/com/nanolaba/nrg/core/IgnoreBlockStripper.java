package com.nanolaba.nrg.core;

import com.nanolaba.logging.LOG;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class IgnoreBlockStripper {

    private static final Pattern INLINE = Pattern.compile("(?<!\\\\)<!--\\s*nrg\\.ignore\\s*-->");
    private static final Pattern BEGIN = Pattern.compile("(?<!\\\\)<!--\\s*nrg\\.ignore\\.begin\\s*-->");
    private static final Pattern END = Pattern.compile("(?<!\\\\)<!--\\s*nrg\\.ignore\\.end\\s*-->");

    private IgnoreBlockStripper() {/**/}

    public static String strip(String body) {
        if (body == null || body.isEmpty()) {
            return body;
        }

        String[] lines = body.split("\\R", -1);

        List<String> out = new ArrayList<>(lines.length);
        boolean inBlock = false;
        int blockStartLine = -1;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            if (inBlock) {
                if (END.matcher(line).find()) {
                    inBlock = false;
                    blockStartLine = -1;
                }
                continue;
            }

            if (BEGIN.matcher(line).find()) {
                inBlock = true;
                blockStartLine = i + 1;
                continue;
            }

            if (END.matcher(line).find()) {
                LOG.error("nrg.ignore.end without matching nrg.ignore.begin at line {}", i + 1);
                continue;
            }

            if (INLINE.matcher(line).find()) {
                continue;
            }

            out.add(line);
        }

        if (inBlock) {
            LOG.error("Unclosed nrg.ignore.begin at line {} (missing nrg.ignore.end)", blockStartLine);
        }

        return String.join("\n", out);
    }
}
