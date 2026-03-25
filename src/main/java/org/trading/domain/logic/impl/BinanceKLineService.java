package org.trading.domain.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.trading.domain.logic.KLineAggregationService;
import org.trading.domain.model.KLineParameters;
import org.trading.insfrastructure.mapper.BinanceKLine;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BinanceKLineService implements KLineAggregationService {

    private final RestTemplate restTemplate;

    @Value("${source.binance.kline-url}")
    private String binanceKLineUrl;


    @Override
    public List<BinanceKLine> craw(KLineParameters parameters) throws Exception {
        String finalUrl = UriComponentsBuilder.fromUriString(binanceKLineUrl)
                .queryParam("symbol", parameters.symbol())       // VD: "BTCUSDT"
                .queryParam("interval", parameters.interval())   // VD: "15m"
                .queryParamIfPresent("limit", Optional.ofNullable(  parameters.limit()))         // VD: 10
                .toUriString();

        HttpEntity<?> requestEntity = new HttpEntity<>(new HttpHeaders());
        var httpResponse =  restTemplate.exchange(finalUrl, HttpMethod.GET, requestEntity, new ParameterizedTypeReference<List<BinanceKLine>>() {});
        if(httpResponse.getStatusCode().is2xxSuccessful()){
            return httpResponse.getBody();
        }
        log.error("Binance Kline request failed with status code {} url {}", httpResponse.getStatusCode(),finalUrl);
        throw new Exception("Binance Kline request failed");
    }
}
