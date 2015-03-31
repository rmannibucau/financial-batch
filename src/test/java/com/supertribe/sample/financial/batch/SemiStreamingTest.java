package com.supertribe.sample.financial.batch;

import com.supertribe.sample.financial.batch.writer.entity.JpaInstrument;
import org.apache.batchee.test.StepLauncher;
import org.apache.openejb.junit.ApplicationComposer;
import org.apache.openejb.testing.*;
import org.apache.openejb.testng.PropertiesBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.Resource;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;
import java.io.File;

import static javax.batch.runtime.BatchStatus.COMPLETED;
import static javax.batch.runtime.BatchStatus.FAILED;
import static org.apache.batchee.test.StepBuilder.extractFromXml;
import static org.apache.openejb.loader.JarLocation.jarLocation;
import static org.junit.Assert.assertEquals;

@Default
@Classes(cdi = true)
@Descriptors(@Descriptor(name = "persistence.xml", path = "META-INF/persistence.xml"))
@ContainerProperties(@ContainerProperties.Property(name = "Default JDBC Database.LogSql", value = "true"))
// debug in logs
@RunWith(ApplicationComposer.class)
public class SemiStreamingTest {
    private static final File CLASSES = jarLocation(SemiStreamingTest.class);
    private static final File INPUT_CSV = new File(CLASSES, "inputData.csv");
    private static final File TARGET = CLASSES.getParentFile();

    @PersistenceContext
    private EntityManager em;

    @Resource
    private UserTransaction ut;

    @Before
    public void populate() throws Exception {
        insert("AT00000AMAG3", "EUR", "XFRA");
        insert("AT000000STR1", "EUR", "XFRA");
        insert("ARP125991090", "EUR", "XFRA");
        insert("ANN4327C1220", "EUR", "XFRA");
    }

    private void insert(String isin, String currency, String mic) throws Exception {
        ut.begin();
        JpaInstrument i = new JpaInstrument();
        JpaInstrument.Id id = new JpaInstrument.Id();

        id.setCurrency(currency);
        id.setIsin(isin);
        id.setMic(mic);
        i.setId(id);
        em.persist(i);
        ut.commit();
    }

    @After
    public void reset() throws Exception {
        ut.begin();
        em.createQuery("delete from JpaInstrument").executeUpdate();
        ut.commit();
    }

    @Test
    public void shouldComputeThresholdAndFailBecauseItsTooHigh() throws Exception {

        insert("SOMEOLDISIN1", "EUR", "XFRA"); // will be deleted
        insert("SOMEOLDISIN2", "EUR", "XFRA"); // will be deleted

        final JobExecution execution = StepLauncher.exec(
                extractFromXml("semi-streaming", "pre-process"),
                new PropertiesBuilder()
                        .p("downloadCache", INPUT_CSV.getAbsolutePath())
                        .build()).jobExecution();
        assertEquals(FAILED, execution.getBatchStatus());
        assertEquals(6, em.createQuery("select count(e) from JpaInstrument e", Number.class).getSingleResult().intValue());
    }

    @Test
    public void shouldComputeThresholdAndSucceedBecauseItsLowEnough() throws Exception {

        final StepExecution execution = StepLauncher.execute(
                extractFromXml("semi-streaming", "pre-process"),
                new PropertiesBuilder()
                        .p("downloadCache", INPUT_CSV.getAbsolutePath())
                        .build());
        assertEquals(COMPLETED, execution.getBatchStatus());
        assertEquals(4, em.createQuery("select count(e) from JpaInstrument e", Number.class).getSingleResult().intValue());
    }
}
