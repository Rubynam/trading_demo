package org.trading.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.trading.application.port.HuobiDataTransformer;
import org.trading.domain.aggregates.AggregationPrice;
import org.trading.insfrastructure.mapper.HuobiData;

@Service
@RequiredArgsConstructor
@Slf4j
public class AggregatedHuobiSourceService implements AggregationService {

  private final RestTemplate restTemplate;
  private final HuobiDataTransformer transformer;

  @Value("${source.huobi.url}")
  private String huobiUrl;

  @Override
  public List<AggregationPrice> aggregate() throws Exception {
    HuobiData prices = restTemplate.getForObject(huobiUrl, HuobiData.class);
    if(prices == null) throw new Exception("Invalid data");

    log.debug("Huobi source url {} Prices: {}",huobiUrl, prices);
    return prices.getData().stream().map(transformer::transform).toList();

  }
}
