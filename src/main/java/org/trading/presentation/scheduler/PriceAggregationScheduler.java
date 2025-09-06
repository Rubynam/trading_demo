package org.trading.presentation.scheduler;

import java.util.LinkedList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.trading.domain.aggregates.AggregationPrice;
import org.trading.domain.enumeration.AggregatedSource;
import org.trading.application.command.AggregatedPriceCommand;
import org.trading.application.command.AggregatedPriceStoreCommand;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceAggregationScheduler {

  private final AggregatedPriceCommand command;
  private final AggregatedPriceStoreCommand storeCommand;

  @Scheduled(fixedRateString = "${scheduler.fixed-rate}") // Every 10 seconds
  public void fetchPriceFromSource() throws Exception {
    final List<AggregationPrice> mergedData = new LinkedList<>();
    final List<AggregationPrice> binanceData = command.execute(AggregatedSource.Binance);
    final List<AggregationPrice> huobiData = command.execute(AggregatedSource.Huobi);

    mergedData.addAll(binanceData);
    mergedData.addAll(huobiData);

    storeCommand.execute(mergedData);
  }
}
