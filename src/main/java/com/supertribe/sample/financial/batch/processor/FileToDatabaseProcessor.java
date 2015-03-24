package com.supertribe.sample.financial.batch.processor;

import com.supertribe.sample.financial.batch.reader.beanio.CsvInstrument;
import com.supertribe.sample.financial.batch.writer.domain.JpaInstrument;
import org.apache.batchee.extras.typed.TypedItemProcessor;

import java.util.UUID;
import javax.inject.Named;

@Named("business")
public class FileToDatabaseProcessor extends TypedItemProcessor<CsvInstrument, JpaInstrument> {
    @Override
    protected JpaInstrument doProcessItem(final CsvInstrument csvInstrument) {
        // TODO: find a mapper? ModelMapper is not bad for business code we could @Produces it
        final JpaInstrument jpa = new JpaInstrument();
        jpa.setId(UUID.randomUUID().toString());
        return jpa;
    }
}
