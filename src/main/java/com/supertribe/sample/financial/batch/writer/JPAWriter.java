package com.supertribe.sample.financial.batch.writer;

import com.supertribe.sample.financial.batch.writer.entity.JpaInstrument;
import org.apache.batchee.extras.typed.NoStateTypedItemWriter;

import java.util.List;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Named("jpa")
public class JPAWriter extends NoStateTypedItemWriter<JpaInstrument> {
    @PersistenceContext
    private EntityManager em;

    @Override
    protected void doWriteItems(final List<JpaInstrument> list) {
        list.stream().forEach(em::persist);
    }
}
