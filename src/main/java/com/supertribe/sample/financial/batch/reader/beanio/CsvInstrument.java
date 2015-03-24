package com.supertribe.sample.financial.batch.reader.beanio;

import org.beanio.annotation.Field;
import org.beanio.annotation.Record;

@Record
public class CsvInstrument {
    @Field(at = 77)
    private double numberOfDecimal;

    @Field(at = 78)
    private String unitOfQuotation;

    public String getUnitOfQuotation() {
        return unitOfQuotation;
    }

    public void setUnitOfQuotation(final String unitOfQuotation) {
        this.unitOfQuotation = unitOfQuotation;
    }

    public double getNumberOfDecimal() {
        return numberOfDecimal;
    }

    public void setNumberOfDecimal(final double numberOfDecimal) {
        this.numberOfDecimal = numberOfDecimal;
    }
}
