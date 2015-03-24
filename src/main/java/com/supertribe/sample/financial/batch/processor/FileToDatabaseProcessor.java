package com.supertribe.sample.financial.batch.processor;

import com.supertribe.sample.financial.batch.reader.beanio.CsvInstrument;
import com.supertribe.sample.financial.batch.writer.entity.JpaInstrument;
import org.apache.batchee.extras.typed.TypedItemProcessor;
import org.modelmapper.ModelMapper;

import javax.inject.Inject;
import javax.inject.Named;

@Named("business")
public class FileToDatabaseProcessor extends TypedItemProcessor<CsvInstrument, JpaInstrument> {
    @Inject
    private ModelMapper mapper;

    @Override
    protected JpaInstrument doProcessItem(final CsvInstrument csvInstrument) {
        return mapper.map(csvInstrument, JpaInstrument.class);
    }
}
