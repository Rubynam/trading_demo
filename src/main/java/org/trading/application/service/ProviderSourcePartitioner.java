package org.trading.application.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.trading.domain.enumeration.AggregatedSource;

@Slf4j
@Service
public class ProviderSourcePartitioner implements PartitionStrategy {

    @Override
    public int partition(AggregatedSource source) {

        // Map provider to partition
        return switch (source){
            case Binance -> 0;
            case Huobi -> 1;
            default -> throw new IllegalArgumentException("Unknow provider");
        };
    }
}
