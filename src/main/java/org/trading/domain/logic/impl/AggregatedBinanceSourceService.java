package org.trading.domain.logic.impl;

import java.util.LinkedList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.trading.application.port.BinanceDataTransformer;
import org.trading.domain.aggregates.AggregationPrice;
import org.trading.domain.logic.AggregationService;
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
    var responseEntity = restTemplate.exchange(
        binanceUrl,
        HttpMethod.GET,
        null,
        new ParameterizedTypeReference<List<BinanceData>>() {}
    );
    List<BinanceData> prices = responseEntity.getBody();

    if(prices == null) throw new Exception("Invalid data");
    log.debug("Binance source url {} Prices: {}",binanceUrl, prices);
    List<AggregationPrice> result = new LinkedList<>();

    prices.forEach(price -> result.add(transformer.transform(price)));
    return result;
  }
}
