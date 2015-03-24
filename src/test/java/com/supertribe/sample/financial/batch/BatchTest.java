package com.supertribe.sample.financial.batch;

import org.apache.openejb.junit.ApplicationComposer;
import org.apache.openejb.testing.Classes;
import org.apache.openejb.testing.Default;
import org.apache.openejb.testing.Descriptor;
import org.apache.openejb.testing.Descriptors;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Properties;
import javax.batch.runtime.BatchRuntime;

import static org.apache.batchee.util.Batches.waitForEnd;

@Default
@Classes(cdi = true)
@Descriptors(@Descriptor(name = "persistence.xml", path = "META-INF/persistence.xml"))
@RunWith(ApplicationComposer.class)
public class BatchTest {
    @Test
    public void execute() {
        waitForEnd(BatchRuntime.getJobOperator().start("file-to-database", new Properties()));
    }
}
