package com.supertribe.sample.financial.batch.writer;

import com.supertribe.sample.financial.batch.writer.entity.JpaInstrument;
import org.apache.batchee.extras.typed.NoStateTypedItemWriter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Named("jpa")
public class JPAWriter extends NoStateTypedItemWriter<JpaInstrument> {
    @PersistenceContext
    private EntityManager em;

    private Map<JpaInstrument.Id, JpaInstrument> all;

    @PostConstruct
    private void cacheDb() { // supposes the batch is the only one to write for its execution duration, acceptable for reference db
        // avoid em.getReference(JpaInstrument.class, instrument.getId()); in doWriteItems and N db hits
        all = em.createNamedQuery("JpaInstrument.findAll", JpaInstrument.class)
                .getResultList().stream()
                .collect(toMap(JpaInstrument::getId, identity()));
    }

    @Override
    protected void doWriteItems(final List<JpaInstrument> list) {
        list.stream().forEach(instrument -> {
            if (Optional.ofNullable(all.get(instrument.getId())).isPresent()) {
                em.merge(instrument);
                all.put(instrument.getId(), instrument);
            } else {
                em.persist(instrument);
            }
        });
    }
}
