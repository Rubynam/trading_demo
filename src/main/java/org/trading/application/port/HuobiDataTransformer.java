package org.trading.application.port;

import org.springframework.stereotype.Service;
import org.trading.domain.aggregates.AggregationPrice;
import org.trading.insfrastructure.mapper.HuobiData;
import org.trading.insfrastructure.mapper.HuobiData.Tickers;

@Service
public class HuobiDataTransformer implements Transformer<HuobiData.Tickers, AggregationPrice> {

  @Override
  public AggregationPrice transform(HuobiData.Tickers input) throws IllegalArgumentException {
    if(input == null || input.getSymbol() == null || input.getBid() == null || input.getAsk() == null)
      throw new IllegalArgumentException("Invalid input");
    if (input.getAsk().doubleValue() < 0 || input.getBid().doubleValue() < 0)
      throw new IllegalArgumentException("Invalid input");

    return AggregationPrice.builder()
        .symbol(input.getSymbol())
        .bidPrice(input.getBid())
        .askPrice(input.getAsk())
        .askPrice(input.getAsk())
        .build();
  }

  @Override
  public Tickers reverseTransform(AggregationPrice output) throws IllegalArgumentException {
    //todo
    return null;
  }
}
