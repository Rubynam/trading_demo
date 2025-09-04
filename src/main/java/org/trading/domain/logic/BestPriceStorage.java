package org.trading.domain.logic;

import java.util.List;
import org.trading.domain.aggregates.AggregationPrice;
import org.trading.insfrastructure.entities.BestAggregatedPrice;

public interface BestPriceStorage {

  List<BestAggregatedPrice> save(List<BestAggregatedPrice> prices);

  AggregationPrice get(String symbol);
}
