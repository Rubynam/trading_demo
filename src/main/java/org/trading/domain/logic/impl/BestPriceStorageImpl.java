package org.trading.domain.logic.impl;

import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.trading.domain.aggregates.AggregationPrice;
import org.trading.domain.logic.BestPriceStorage;
import org.trading.insfrastructure.entities.BestAggregatedPrice;
import org.trading.insfrastructure.repository.BestAggregatedPriceRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class BestPriceStorageImpl implements BestPriceStorage {

  private final BestAggregatedPriceRepository bestAggregatedPriceRepository;

  @Override
  @Transactional
  public List<BestAggregatedPrice> save(List<BestAggregatedPrice> prices) {
    //so we need to filter currentcy pair before.
    return bestAggregatedPriceRepository.saveAll(prices);
  }

  @Override
  public AggregationPrice get(String symbol) {
    return null;
  }
}
