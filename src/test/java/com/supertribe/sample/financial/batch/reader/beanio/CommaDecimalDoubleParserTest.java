package com.supertribe.sample.financial.batch.reader.beanio;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CommaDecimalDoubleParserTest {
    @Test
    public void convert() {
        assertEquals(1234.456, new CommaDecimalDoubleParser().createNumber("1,234.456").doubleValue(), 0.);
    }
}
