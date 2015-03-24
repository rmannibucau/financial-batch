package com.supertribe.sample.financial.batch;

import org.apache.openejb.junit.ApplicationComposer;
import org.apache.openejb.testing.Classes;
import org.apache.openejb.testing.ContainerProperties;
import org.apache.openejb.testing.Default;
import org.apache.openejb.testing.Descriptor;
import org.apache.openejb.testing.Descriptors;
import org.apache.openejb.testng.PropertiesBuilder;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import javax.annotation.Resource;
import javax.batch.runtime.BatchRuntime;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;

import static org.apache.batchee.util.Batches.waitForEnd;
import static org.apache.openejb.loader.JarLocation.jarLocation;
import static org.junit.Assert.assertEquals;

@Default
@Classes(cdi = true)
@Descriptors(@Descriptor(name = "persistence.xml", path = "META-INF/persistence.xml"))
@ContainerProperties(@ContainerProperties.Property(name = "Default JDBC Database.LogSql", value = "true")) // debug in logs
@RunWith(ApplicationComposer.class)
public class BatchTest {
    @PersistenceContext
    private EntityManager em;

    @Resource
    private UserTransaction ut;

    @After
    public void reset() throws Exception {
        ut.begin();
        em.createQuery("delete from JpaInstrument").executeUpdate();
        ut.commit();
    }

    @Test
    public void execute() {
        waitForEnd(BatchRuntime.getJobOperator().start(
                "file-to-database",
                new PropertiesBuilder()
                        .p("input", new File(jarLocation(BatchTest.class), "inputData.csv").getAbsolutePath())
                .build()));
        assertEquals(15, em.createQuery("select count(e) from JpaInstrument e", Number.class).getSingleResult().intValue());
    }
}
