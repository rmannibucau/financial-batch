package com.supertribe.sample.financial.batch.writer;

import java.util.List;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

// TODO
@Named("jpa")
public class JPAWriter extends AbstractItemWriter {
    @PersistenceContext
    private EntityManager em;

    @Override
    public void writeItems(final List<Object> items) throws Exception {

    }
}
