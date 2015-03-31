package com.supertribe.sample.financial.batch.writer;

public class ThresholdInfo {
    private int beansAlreadyPresent;
    private int total;

    public ThresholdInfo(int beansAlreadyPresent, int total) {
        this.beansAlreadyPresent = beansAlreadyPresent;
        this.total = total;
    }

    @Override
    public String toString() {
        return "ThresholdInfo{" +
                "beansAlreadyPresent=" + beansAlreadyPresent +
                ", total=" + total +
                '}';
    }

    public int getBeansAlreadyPresent() {
        return beansAlreadyPresent;
    }

    public int getTotal() {
        return total;
    }
}
