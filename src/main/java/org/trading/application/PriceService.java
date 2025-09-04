package org.trading.application;


import java.util.List;
import org.trading.domain.aggregates.AggregationPrice;

public interface PriceService {

  boolean store(List<AggregationPrice> prices) throws Exception;
}
