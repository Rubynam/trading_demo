package org.trading.presentation.scheduler;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.trading.domain.aggregates.AggregationPrice;
import org.trading.insfrastructure.constant.AggregatedSource;
import org.trading.presentation.command.AggregatedPriceCommand;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceAggregationScheduler {

  private final AggregatedPriceCommand command;

  @Scheduled(fixedRateString = "${scheduler.fixed-rate}") // Every 10 seconds
  public void fetchPriceFromSource() throws Exception {

    List<AggregationPrice> binanceData = command.execute(AggregatedSource.Binance);
    List<AggregationPrice> huobiData = command.execute(AggregatedSource.Huobi);

    binanceData.addAll(huobiData);

    //todo save
  }
}
