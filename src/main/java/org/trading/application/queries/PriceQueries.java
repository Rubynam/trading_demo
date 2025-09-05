package org.trading.application.queries;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.trading.domain.aggregates.AggregationPrice;
import org.trading.domain.logic.PriceService;
import org.trading.presentation.response.BestPriceResponse;

@Service
@RequiredArgsConstructor
public class PriceQueries {

  private final PriceService priceService;

  public BestPriceResponse getBestPriceBy(String symbol) {

    AggregationPrice aggregationPrice = priceService.getBestPrice(symbol);
    if(aggregationPrice == null) return BestPriceResponse.builder().symbol(symbol).build();

    return BestPriceResponse.builder()
        .symbol(aggregationPrice.getSymbol())
        .bestAskPrice(String.valueOf(aggregationPrice.getAskPrice()))
        .bestBidPrice(String.valueOf(aggregationPrice.getBidPrice()))
        .build();
  }
}
