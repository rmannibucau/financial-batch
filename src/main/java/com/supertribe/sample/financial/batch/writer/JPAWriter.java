package com.supertribe.sample.financial.batch.writer;

import com.supertribe.sample.financial.batch.writer.entity.JpaInstrument;
import org.apache.batchee.extras.typed.NoStateTypedItemWriter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Named("jpa")
public class JPAWriter extends NoStateTypedItemWriter<JpaInstrument> {
    @PersistenceContext
    private EntityManager em;

    @Inject
    private JobContext context;

    @Inject
    @BatchProperty
    private Boolean propagateStatus;

    private Map<JpaInstrument.Id, Item<JpaInstrument>> all;

    @PostConstruct
    private void cacheDb() { // supposes the batch is the only one to write for its execution duration, acceptable for reference db
        // avoid em.getReference(JpaInstrument.class, instrument.getId()); in doWriteItems and N db hits
        all = em.createNamedQuery("JpaInstrument.findAll", JpaInstrument.class)
                .getResultList().stream()
                .map(i -> new Item<>(i, null))
                .collect(toMap(i -> i.value.getId(), identity()));
    }

    @Override
    protected void doWriteItems(final List<JpaInstrument> list) {
        list.stream().forEach(instrument -> {
            if (Optional.ofNullable(all.get(instrument.getId())).isPresent()) {
                em.merge(instrument);
                all.put(instrument.getId(), new Item<>(instrument, false));
            } else {
                em.persist(instrument);
                all.put(instrument.getId(), new Item<>(instrument, true));
            }
        });
        if (propagateStatus != null && propagateStatus) {
            context.setTransientUserData(all); // allow next step to reuse it if needed
        }
    }

    public static class Item<T> {
        private final T value;
        private Boolean isNew;

        public Item(final T value, final Boolean isNew) {
            this.value = value;
            this.isNew = isNew;
        }

        public T getValue() {
            return value;
        }

        public Boolean isNew() {
            return isNew;
        }
    }
}
