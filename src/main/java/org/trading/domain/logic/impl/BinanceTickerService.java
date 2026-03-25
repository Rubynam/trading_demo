package org.trading.domain.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.trading.domain.logic.TickerAggregationService;
import org.trading.insfrastructure.mapper.BinanceTicker;

import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class BinanceTickerService implements TickerAggregationService {

    private final RestTemplate restTemplate;

    @Value("${source.binance.ticker}")
    private String binanceTickerUrl;

    @Override
    public BinanceTicker craw(String symbol, String interval) throws Exception {
        if(Objects.isNull(interval)){
            return null;
        }
        String finalUrl = UriComponentsBuilder.fromUriString(binanceTickerUrl)
                .path(interval)
                .queryParam("symbol", symbol)
                .toUriString();

        HttpEntity<?> requestEntity = new HttpEntity<>(new HttpHeaders());
        var httpResponse = restTemplate.exchange(
                finalUrl,
                HttpMethod.GET,
                requestEntity,
                BinanceTicker.class
        );

        if (httpResponse.getStatusCode().is2xxSuccessful()) {
            return httpResponse.getBody();
        }
        throw new Exception("Binance Ticker request failed");
    }
}