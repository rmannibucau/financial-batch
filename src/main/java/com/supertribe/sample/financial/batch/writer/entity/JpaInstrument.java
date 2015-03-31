package com.supertribe.sample.financial.batch.writer.entity;

import java.util.Date;
import javax.persistence.*;

import static javax.persistence.TemporalType.DATE;

@Entity
@NamedQueries({
        @NamedQuery(name = "JpaInstrument.findAll", query = "select i from JpaInstrument i"),
        @NamedQuery(name = "JpaInstrument.findAll.Pk", query = "Select i.id from JpaInstrument i")
})
public class JpaInstrument {
    @EmbeddedId
    private Id id;

    @Temporal(DATE)
    private Date dateLastUpdate;

    private String instrument;
    private String wkn;
    private String mnemonic;
    private double roundLot;
    private double minTradableUnit;
    private int setId;
    private double numberOfDecimal;
    private String unitOfQuotation;
    private double closingPricePreviousDay;
    private String tradingCalendar;

    public Id getId() {
        return id;
    }

    public void setId(final Id id) {
        this.id = id;
    }

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

    public double getRoundLot() {
        return roundLot;
    }

    public void setRoundLot(final double roundLot) {
        this.roundLot = roundLot;
    }

    public double getMinTradableUnit() {
        return minTradableUnit;
    }

    public void setMinTradableUnit(final double minTradableUnit) {
        this.minTradableUnit = minTradableUnit;
    }

    public int getSetId() {
        return setId;
    }

    public void setSetId(final int setId) {
        this.setId = setId;
    }

    public double getNumberOfDecimal() {
        return numberOfDecimal;
    }

    public void setNumberOfDecimal(final double numberOfDecimal) {
        this.numberOfDecimal = numberOfDecimal;
    }

    public String getUnitOfQuotation() {
        return unitOfQuotation;
    }

    public void setUnitOfQuotation(final String unitOfQuotation) {
        this.unitOfQuotation = unitOfQuotation;
    }

    public double getClosingPricePreviousDay() {
        return closingPricePreviousDay;
    }

    public void setClosingPricePreviousDay(final double closingPricePreviousDay) {
        this.closingPricePreviousDay = closingPricePreviousDay;
    }

    public String getTradingCalendar() {
        return tradingCalendar;
    }

    public void setTradingCalendar(final String tradingCalendar) {
        this.tradingCalendar = tradingCalendar;
    }

    @Override
    public String toString() {
        return "JpaInstrument{" +
                "id=" + id +
                '}';
    }

    @Embeddable
    public static class Id {
        private String isin;
        private String mic;
        private String currency;

        public String getIsin() {
            return isin;
        }

        public void setIsin(final String isin) {
            this.isin = isin;
        }

        public String getMic() {
            return mic;
        }

        public void setMic(final String mic) {
            this.mic = mic;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(final String currency) {
            this.currency = currency;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || !Id.class.isInstance(o)) {
                return false;
            }
            final Id id = Id.class.cast(o);
            return isin.equals(id.isin) && mic.equals(id.mic) && currency.equals(id.currency);
        }

        @Override
        public int hashCode() {
            int result = isin.hashCode();
            result = 31 * result + mic.hashCode();
            result = 31 * result + currency.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Id{" +
                    "isin='" + isin + '\'' +
                    ", mic='" + mic + '\'' +
                    ", currency='" + currency + '\'' +
                    '}';
        }
    }
}
