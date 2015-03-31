package com.supertribe.sample.financial.batch;

import com.supertribe.sample.financial.batch.writer.entity.JpaInstrument;
import org.apache.batchee.test.StepLauncher;
import org.apache.commons.io.FileUtils;
import org.apache.openejb.junit.ApplicationComposer;
import org.apache.openejb.testing.*;
import org.apache.openejb.testng.PropertiesBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.Resource;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static javax.batch.runtime.BatchStatus.COMPLETED;
import static org.apache.batchee.test.StepBuilder.extractFromXml;
import static org.apache.openejb.loader.JarLocation.jarLocation;
import static org.junit.Assert.*;

@Default
@Classes(cdi = true)
@Descriptors(@Descriptor(name = "persistence.xml", path = "META-INF/persistence.xml"))
@ContainerProperties(@ContainerProperties.Property(name = "Default JDBC Database.LogSql", value = "true")) // debug in logs
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
        ut.begin();
        try {
            insert("AT00000AMAG3", "EUR", "XFRA");
            insert("AT000000STR1", "EUR", "XFRA");
            insert("ARP125991090", "EUR", "XFRA");
            insert("ANN4327C1220", "EUR", "XFRA");
            insert("SOMEOLDISIN1", "EUR", "XFRA"); // will be deleted
            insert("SOMEOLDISIN2", "EUR", "XFRA"); // will be deleted
        } catch (Exception e) {
            System.out.println("Error populating test db");
            e.printStackTrace();
        } finally {
            ut.commit();
        }
    }

    private void insert(String isin, String currency, String mic) {
        JpaInstrument i = new JpaInstrument();
        JpaInstrument.Id id = new JpaInstrument.Id();

        id.setCurrency(currency);
        id.setIsin(isin);
        id.setMic(mic);
        i.setId(id);
        em.persist(i);
    }

    @After
    public void reset() throws Exception {
        ut.begin();
        em.createQuery("delete from JpaInstrument").executeUpdate();
        ut.commit();
    }

    @Ignore
    @Test
    public void shouldComputeThresholdAndFailBecauseItsTooHigh() {
        final StepExecution execution = StepLauncher.execute(
                extractFromXml("semi-streaming","pre-process"),
                new PropertiesBuilder()
                    .p("downloadCache", INPUT_CSV.getAbsolutePath())
                .build());

        assertEquals(COMPLETED, execution.getBatchStatus());
        assertEquals(
                new HashMap<Metric.MetricType, Long>() {{
                    put(Metric.MetricType.READ_COUNT, 15L);
                    put(Metric.MetricType.WRITE_COUNT, 15L);
                    put(Metric.MetricType.COMMIT_COUNT, 2L);
                    put(Metric.MetricType.ROLLBACK_COUNT, 0L);
                    put(Metric.MetricType.PROCESS_SKIP_COUNT, 0L);
                    put(Metric.MetricType.FILTER_COUNT, 0L);
                    put(Metric.MetricType.WRITE_SKIP_COUNT, 0L);
                    put(Metric.MetricType.READ_SKIP_COUNT, 0L);
                }},
                asList(execution.getMetrics()).stream().collect(toMap(Metric::getType, Metric::getValue)));
        assertEquals(15, em.createQuery("select count(e) from JpaInstrument e", Number.class).getSingleResult().intValue());

    }
}
