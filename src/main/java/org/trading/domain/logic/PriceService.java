package org.trading.domain.logic;


import java.util.List;
import org.trading.domain.aggregates.AggregationPrice;

public interface PriceService {

  boolean store(List<AggregationPrice> prices) throws Exception;

  AggregationPrice getBestPrice(String symbol);
}
