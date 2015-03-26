package com.supertribe.sample.financial.batch.report;

import java.util.List;
import java.util.logging.Logger;
import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.StepExecution;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

import static java.util.Arrays.asList;

@Named
public class Report extends AbstractBatchlet {
    private static final Logger LOGGER = Logger.getLogger(Report.class.getName());

    @Inject
    private JobContext context;

    @Inject
    @BatchProperty
    private Integer stepIndex;

    @Override
    public String process() throws Exception {
        final List<StepExecution> steps = BatchRuntime.getJobOperator().getStepExecutions(context.getExecutionId());
        if (steps.size() <= stepIndex) {
            throw new IllegalArgumentException("execution has only " + steps.size() + " steps");
        }
        if (stepIndex < 0) {
            for (final StepExecution e : steps) {
                doReport(e);
            }
        } else {
            doReport(steps.get(stepIndex));
        }
        return "reported";
    }

    private void doReport(final StepExecution execution) {
        LOGGER.info("Step: " + execution.getStepName());
        LOGGER.info("Execution Id: " + execution.getStepExecutionId());
        LOGGER.info("Status: " + execution.getExitStatus());
        LOGGER.info("Start: " + execution.getStartTime());
        LOGGER.info("Stop: " + execution.getEndTime());
        LOGGER.info("Metrics:");
        asList(execution.getMetrics()).stream()
                .sorted((o1, o2) -> o1.getType().name().compareTo(o2.getType().name()))
                .forEach(m -> LOGGER.info("  - " + m.getType().name() + " = " + m.getValue()));
    }
}
