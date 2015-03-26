package com.supertribe.sample.financial.batch.io;

import org.apache.commons.io.FileUtils;

import java.io.File;
import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.inject.Inject;
import javax.inject.Named;

@Named("clean")
public class CleanUpCache extends AbstractBatchlet {
    @Inject
    @BatchProperty
    private String path;

    @Override
    public String process() throws Exception {
        FileUtils.forceDelete(new File(path));
        return path;
    }
}
