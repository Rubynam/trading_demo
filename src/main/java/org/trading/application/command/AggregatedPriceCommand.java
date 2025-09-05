package org.trading.application.command;

import java.util.LinkedList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.trading.application.AggregatedBinanceSourceService;
import org.trading.application.AggregatedHuobiSourceService;
import org.trading.domain.aggregates.AggregationPrice;
import org.trading.constant.AggregatedSource;

@Service
@RequiredArgsConstructor
public class AggregatedPriceCommand implements Command<AggregatedSource, List<AggregationPrice>> {

  private final AggregatedBinanceSourceService binanceService;
  private final AggregatedHuobiSourceService huobiService;

  @Override
  public List<AggregationPrice> execute(AggregatedSource input) throws Exception {
    var data = switch (input) {
      case Binance -> binanceService.aggregate();
      case Huobi -> huobiService.aggregate();
    };
    return new LinkedList<>(data);
  }
}
