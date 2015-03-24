package com.supertribe.sample.financial.batch.processor;

import javax.batch.api.chunk.ItemProcessor;
import javax.inject.Named;

// TODO
@Named("business")
public class FileToDatabaseProcessor implements ItemProcessor {
    public Object processItem(final Object item) throws Exception {
        return item;
    }
}
