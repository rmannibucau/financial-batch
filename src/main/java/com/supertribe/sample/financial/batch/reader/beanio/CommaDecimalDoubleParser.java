package com.supertribe.sample.financial.batch.reader.beanio;

import org.beanio.types.DoubleTypeHandler;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;

public class CommaDecimalDoubleParser extends DoubleTypeHandler {
    private final DecimalFormat df;

    public CommaDecimalDoubleParser() {
        final DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        symbols.setGroupingSeparator(',');

        df = new DecimalFormat();
        df.setDecimalFormatSymbols(symbols);
    }

    @Override
    protected Double createNumber(final String text) throws NumberFormatException {
        try {
            return df.parse(text).doubleValue();
        } catch (final ParseException e) {
            throw new NumberFormatException(text);
        }
    }
}
