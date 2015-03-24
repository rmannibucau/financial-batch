package com.supertribe.sample.financial.batch.reader.beanio;

import org.beanio.annotation.Field;
import org.beanio.annotation.Record;

import java.util.Date;

@Record
public class CsvInstrument {
    @Field(at = 0, format = "dd.MM.yyyy")
    private Date dateLastUpdate;

    @Field(at = 1)
    private String instrument;

    @Field(at = 2)
    private String isin;

    @Field(at = 4)
    private String wkn;

    @Field(at = 5)
    private String mnemonic;

    @Field(at = 6)
    private String mic;

    @Field(at = 16)
    private double roundLot;

    @Field(at = 16)
    private double minTradableUnit;

    @Field(at = 34)
    private int setId;

    @Field(at = 77)
    private double numberOfDecimal;

    @Field(at = 78)
    private String unitOfQuotation;

    @Field(at = 82)
    private double closingPricePreviousDay;

    @Field(at = 95)
    private String tradingCalendar;

    @Field(at = 98)
    private String settlementCurrency;

    public Date getDateLastUpdate() {
        return dateLastUpdate;
    }

    public void setDateLastUpdate(final Date dateLastUpdate) {
        this.dateLastUpdate = dateLastUpdate;
    }

    public String getInstrument() {
        return instrument;
    }

    public void setInstrument(final String instrument) {
        this.instrument = instrument;
    }

    public String getIsin() {
        return isin;
    }

    public void setIsin(final String isin) {
        this.isin = isin;
    }

    public String getWkn() {
        return wkn;
    }

    public void setWkn(final String wkn) {
        this.wkn = wkn;
    }

    public String getMnemonic() {
        return mnemonic;
    }

    public void setMnemonic(final String mnemonic) {
        this.mnemonic = mnemonic;
    }

    public String getMic() {
        return mic;
    }

    public void setMic(String mic) {
        this.mic = mic;
    }

    public double getRoundLot() {
        return roundLot;
    }

    public void setRoundLot(double roundLot) {
        this.roundLot = roundLot;
    }

    public double getMinTradableUnit() {
        return minTradableUnit;
    }

    public void setMinTradableUnit(double minTradableUnit) {
        this.minTradableUnit = minTradableUnit;
    }

    public int getSetId() {
        return setId;
    }

    public void setSetId(int setId) {
        this.setId = setId;
    }

    public double getNumberOfDecimal() {
        return numberOfDecimal;
    }

    public void setNumberOfDecimal(double numberOfDecimal) {
        this.numberOfDecimal = numberOfDecimal;
    }

    public String getUnitOfQuotation() {
        return unitOfQuotation;
    }

    public void setUnitOfQuotation(String unitOfQuotation) {
        this.unitOfQuotation = unitOfQuotation;
    }

    public double getClosingPricePreviousDay() {
        return closingPricePreviousDay;
    }

    public void setClosingPricePreviousDay(double closingPricePreviousDay) {
        this.closingPricePreviousDay = closingPricePreviousDay;
    }

    public String getTradingCalendar() {
        return tradingCalendar;
    }

    public void setTradingCalendar(String tradingCalendar) {
        this.tradingCalendar = tradingCalendar;
    }

    public String getSettlementCurrency() {
        return settlementCurrency;
    }

    public void setSettlementCurrency(String settlementCurrency) {
        this.settlementCurrency = settlementCurrency;
    }
}
