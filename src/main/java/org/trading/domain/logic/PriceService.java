package org.trading.domain.logic;


import java.util.List;
import org.trading.domain.aggregates.AggregationPrice;
import org.trading.presentation.response.BestPriceDto;

public interface PriceService {

  boolean store(List<AggregationPrice> prices) throws Exception;

  BestPriceDto getBestPrice(String symbol);
}
