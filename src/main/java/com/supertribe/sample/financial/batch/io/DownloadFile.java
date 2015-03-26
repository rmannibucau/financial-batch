package com.supertribe.sample.financial.batch.io;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.URL;
import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.inject.Inject;
import javax.inject.Named;

@Named("download")
public class DownloadFile extends AbstractBatchlet {
    @Inject
    @BatchProperty
    private String inputURL;

    @Inject
    @BatchProperty
    private String output;

    @Override
    public String process() throws Exception { // stupid impl but enough for us
        validate();
        final File out = new File(output);
        if (out.isFile()) { // don't re-download if already here
            return output;
        }
        FileUtils.forceMkdir(out.getParentFile());
        FileUtils.copyURLToFile(new URL(inputURL), out);
        return output;
    }

    private void validate() {
        if (inputURL == null) {
            throw new IllegalArgumentException("input not provided");
        }
        if (output == null) {
            throw new IllegalArgumentException("output not provided");
        }
    }
}
