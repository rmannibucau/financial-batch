package com.supertribe.sample.financial.batch.reader;

import javax.batch.api.chunk.AbstractItemReader;
import javax.inject.Named;

// TODO: use batchee?
@Named("file")
public class FileReader extends AbstractItemReader {
    @Override
    public Object readItem() throws Exception {
        return null;
    }
}
