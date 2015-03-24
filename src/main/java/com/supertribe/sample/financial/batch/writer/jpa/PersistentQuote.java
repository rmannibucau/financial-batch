package com.supertribe.sample.financial.batch.writer.jpa;

import javax.persistence.Entity;

@Entity
public class PersistentQuote {
    private String id;

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }
}
