package com.supertribe.sample.financial.batch.writer.domain;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class JpaInstrument {
    @Id
    private String id;

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }
}
