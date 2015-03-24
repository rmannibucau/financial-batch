package com.supertribe.sample.financial.batch.processor;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class MapperProducer {
    private ModelMapper mapper;

    @PostConstruct
    private void init() {
        mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE); // for jpda id matching
    }

    @Produces
    public ModelMapper jpaMapper() {
        return mapper;
    }
}
