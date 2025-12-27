package org.trading.application.service;


import org.trading.domain.enumeration.AggregatedSource;

public interface PartitionStrategy {

    int partition(AggregatedSource source);
}
