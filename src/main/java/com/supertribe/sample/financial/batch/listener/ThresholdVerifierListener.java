package com.supertribe.sample.financial.batch.listener;

import com.supertribe.sample.financial.batch.writer.ThresholdInfo;

import javax.batch.api.BatchProperty;
import javax.batch.api.listener.AbstractStepListener;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ThresholdVerifierListener extends AbstractStepListener {
    @Inject
    private JobContext context;

    @Inject
    @BatchProperty
    private Double maxThreshold;

    @Inject
    private StepContext stepContext;

    @Override
    public void afterStep() throws Exception {

        ThresholdInfo ti = ThresholdInfo.class.cast(context.getTransientUserData());

        double threshold = 1.0*(ti.getTotal() - ti.getBeansAlreadyPresent() ) / ti.getTotal();

        if (threshold > maxThreshold) {
            stepContext.setExitStatus("safetyThresholdExceeded");
        }
    }
}
