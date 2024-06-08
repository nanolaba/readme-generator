package com.nanolaba.nrg.core;

import java.io.File;

public class NoHeadCommentGenerator extends Generator {
    public NoHeadCommentGenerator(File sourceFile, String source) {
        super(sourceFile, source);
    }

    @Override
    protected String generateHeadComment(String language) {
        return "";
    }
}
