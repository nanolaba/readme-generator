package com.nanolaba.nrg.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class NoHeadCommentGenerator extends Generator {

    public NoHeadCommentGenerator(File sourceFile, Charset charset) throws IOException {
        super(sourceFile, charset);
    }

    public NoHeadCommentGenerator(File sourceFile, String source) {
        super(sourceFile, source);
    }

    @Override
    protected String generateHeadComment(String language) {
        return "";
    }
}
