package org.trading.application;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.trading.application.port.BinanceDataTransformer;
import org.trading.domain.aggregates.AggregationPrice;
import org.trading.insfrastructure.mapper.BinanceData;

@Service
@Slf4j
@RequiredArgsConstructor
public class AggregatedBinanceSourceService implements AggregationService {

  private final RestTemplate restTemplate;
  private final BinanceDataTransformer transformer;

  @Value("${source.binance.url}")
  private String binanceUrl;

  @Override
  public List<AggregationPrice> aggregate() throws Exception {
    List<BinanceData> prices = restTemplate.getForObject(binanceUrl, List.class, BinanceData.class);
    if(prices == null) throw new Exception("Invalid data");
    log.info("Binance source Prices: {}", prices);
    return prices.stream().map(transformer::transform).toList();

  }
}
