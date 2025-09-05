package org.trading.domain.logic.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.trading.application.port.BestAggregatedPriceTransformer;
import org.trading.domain.aggregates.AggregationPrice;
import org.trading.domain.logic.BestPriceStorage;
import org.trading.domain.logic.PriceService;
import org.trading.insfrastructure.entities.BestAggregatedPrice;
import org.trading.presentation.response.BestPriceDto;

@Service
@Slf4j
@RequiredArgsConstructor
public class PriceServiceImpl implements PriceService {

  private final BestAggregatedPriceTransformer transformer;
  private final BestPriceStorage bestPriceStorage;

  @Override
  public boolean store(List<AggregationPrice> prices) throws Exception {
    try{
      bestPriceStorage.save(prices.stream().map(transformer::transform).toList());
      return true;
    }catch(Exception e){
      log.error("Error while storing prices",e);
      return false;
    }
  }

  @Override
  public BestPriceDto getBestPrice(String symbol) {
    BestAggregatedPrice bestAggregatedPrice = bestPriceStorage.findTopBySymbolOrderByTimestampDesc(symbol);
    if(bestAggregatedPrice == null) return BestPriceDto.builder().symbol(symbol).build();

    AggregationPrice aggregationPrice =  transformer.reverseTransform(bestAggregatedPrice);

    return BestPriceDto.builder()
        .symbol(aggregationPrice.getSymbol())
        .bestAskPrice(String.valueOf(aggregationPrice.getAskPrice()))
        .bestBidPrice(String.valueOf(aggregationPrice.getBidPrice()))
        .build();
  }
}
