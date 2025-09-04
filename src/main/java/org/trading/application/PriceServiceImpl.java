package org.trading.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.trading.application.port.BestAggregatedPriceTransformer;
import org.trading.domain.aggregates.AggregationPrice;
import org.trading.domain.logic.BestPriceStorage;

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
}
