package org.trading.domain.logic;

import java.util.List;
import org.trading.insfrastructure.entities.BestAggregatedPrice;

public interface BestPriceStorage {

  List<BestAggregatedPrice> save(List<BestAggregatedPrice> prices);

  BestAggregatedPrice findTopBySymbolOrderByTimestampDesc(String symbol);
}
