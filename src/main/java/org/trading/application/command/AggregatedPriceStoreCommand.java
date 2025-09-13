package org.trading.application.command;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.trading.domain.logic.PriceService;
import org.trading.domain.aggregates.AggregationPrice;

@Service
@Slf4j
@RequiredArgsConstructor
public class AggregatedPriceStoreCommand implements Command<List<AggregationPrice>, Boolean> {

  private final PriceService priceService;

  @Transactional
  @Override
  public Boolean execute(List<AggregationPrice> input) throws Exception {
    if(input == null) throw new IllegalArgumentException("Invalid input");
    if(input.isEmpty()) return true;

    return priceService.store(input);
  }
}
