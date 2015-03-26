package com.supertribe.sample.financial.batch;

import org.apache.batchee.test.StepLauncher;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.LocaleUtils;
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
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import javax.annotation.Resource;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static javax.batch.runtime.BatchStatus.COMPLETED;
import static org.apache.batchee.test.StepBuilder.extractFromXml;
import static org.apache.openejb.loader.JarLocation.jarLocation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Default
@Classes(cdi = true)
@Descriptors(@Descriptor(name = "persistence.xml", path = "META-INF/persistence.xml"))
@ContainerProperties(@ContainerProperties.Property(name = "Default JDBC Database.LogSql", value = "true")) // debug in logs
@RunWith(ApplicationComposer.class)
public class BatchTest {
    private static final File CLASSES = jarLocation(BatchTest.class);
    private static final File INPUT_CSV = new File(CLASSES, "inputData.csv");
    private static final File TARGET = CLASSES.getParentFile();

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
    public void download() throws IOException {
        final File target = new File(TARGET, "BatchTest#download.csv");
        FileUtils.deleteQuietly(target);
        assertFalse(target.exists());

        final StepExecution execution = StepLauncher.execute(
                extractFromXml("file-to-database", "download"),
                new PropertiesBuilder()
                        .p("inputURL", INPUT_CSV.toURI().toASCIIString())
                        .p("downloadCache", target.getAbsolutePath())
                        .build());
        assertEquals(COMPLETED, execution.getBatchStatus());
        assertTrue(target.exists());

        assertEquals(FileUtils.readFileToString(INPUT_CSV), FileUtils.readFileToString(target));
        FileUtils.deleteQuietly(target);
    }

    @Test
    public void chunkStep() {
        final StepExecution execution = StepLauncher.execute(
                extractFromXml("file-to-database","process"),
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

        /* would execute the whole batch
        waitForEnd(BatchRuntime.getJobOperator().start(
                "file-to-database",
                new PropertiesBuilder()
                        .p("input", new File(jarLocation(BatchTest.class), "inputData.csv").getAbsolutePath())
                .build()));
        */
        /* explicitly execute one step
        StepExecution execution = StepLauncher.execute(newStep()
            .chunk()
                .reader()
                    .ref(BeanIOReader.class.getName())
                    .property("file", new File(jarLocation(BatchTest.class), "inputData.csv").getAbsolutePath())
                    .property("streamName", "readerCSV")
                    .property("configuration", "beanio.xml")
                    .property("skippedHeaderLines", "5")
                .up()
                .processor()
                    .ref(ModelMapperItemProcessor.class.getName())
                    .property("matchingStrategy", "LOOSE")
                    .property("destinationType", JpaInstrument.class.getName())
                .up()
                .writer()
                    .ref("jpa")
                .up()
            .up()
        .create())
         */
    }
}
