package com.supertribe.sample.financial.batch.writer;

import com.supertribe.sample.financial.batch.writer.entity.JpaInstrument;
import org.apache.batchee.extras.typed.NoStateTypedItemWriter;

import javax.annotation.PostConstruct;
import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Named
public class ThresholdComparator extends NoStateTypedItemWriter<JpaInstrument> {
    @PersistenceContext
    private EntityManager em;

    @Inject
    private JobContext context;

    @Inject
    @BatchProperty
    private Boolean propagateStatus;

    private Set<JpaInstrument.Id> all;

    private AtomicInteger counter = new AtomicInteger(0);

    @PostConstruct
    private void cacheDb() { // supposes the batch is the only one to write for its execution duration, acceptable for reference db
        // avoid em.getReference(JpaInstrument.class, instrument.getId()); in doWriteItems and N db hits
        List<JpaInstrument.Id> list = em.createQuery("Select i.id from JpaInstrument i", JpaInstrument.Id.class).getResultList();
        all = new HashSet<>(list);
    }

    @Override
    protected void doWriteItems(final List<JpaInstrument> list) {

        for (JpaInstrument i : list) {
            if (all.contains(i.getId())) {
                counter.incrementAndGet();
            }
        }
    }
}
