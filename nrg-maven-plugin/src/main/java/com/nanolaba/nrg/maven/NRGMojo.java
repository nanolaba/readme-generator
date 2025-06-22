package com.nanolaba.nrg.maven;

import com.nanolaba.nrg.NRG;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "create-files", defaultPhase = LifecyclePhase.COMPILE)
public class NRGMojo extends AbstractMojo {

    private String[] files = new String[]{"README.src.md"};

    @Parameter(property = "charset")
    private String charset = "UTF-8";

    @Parameter(property = "file")
    public void setFile(String[] files) {
        this.files = files;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (files == null || files.length == 0) {
            getLog().warn("No template files specified");
        } else {
            for (String file : files) {
                NRG.main("-f", file, "--charset", charset);
            }
        }
    }
}
