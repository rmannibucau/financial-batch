package com.supertribe.sample.financial.batch.writer;

import javax.batch.api.AbstractBatchlet;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class CleanTransientData extends AbstractBatchlet {
    @Inject
    private JobContext context;

    @Override
    public String process() throws Exception {
        context.setTransientUserData(null);
        return "nullified";
    }
}
