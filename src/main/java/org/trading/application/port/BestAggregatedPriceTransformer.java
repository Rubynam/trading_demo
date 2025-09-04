package org.trading.application.port;

import org.springframework.stereotype.Service;
import org.trading.domain.aggregates.AggregationPrice;
import org.trading.insfrastructure.entities.BestAggregatedPrice;

@Service
public class BestAggregatedPriceTransformer implements Transformer<AggregationPrice, BestAggregatedPrice> {

  @Override
  public BestAggregatedPrice transform(AggregationPrice input) throws IllegalArgumentException {
    if (input == null) throw new IllegalArgumentException("Invalid input");
    BestAggregatedPrice bestAggregatedPrice = new BestAggregatedPrice();
    bestAggregatedPrice.setSymbol(input.getSymbol());
    bestAggregatedPrice.setBidPrice(input.getBidPrice());
    bestAggregatedPrice.setAskPrice(input.getAskPrice());
    return bestAggregatedPrice;
  }
}
