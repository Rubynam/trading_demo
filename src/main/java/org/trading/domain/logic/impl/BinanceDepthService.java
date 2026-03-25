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
import org.trading.domain.logic.DepthAggregationService;
import org.trading.insfrastructure.mapper.BinanceDepth;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BinanceDepthService implements DepthAggregationService {

    private final RestTemplate restTemplate;

    @Value("${source.binance.depth}")
    private String binanceDepthUrl;

    @Override
    public BinanceDepth craw(String symbol, Integer limit) throws Exception {
        String finalUrl = UriComponentsBuilder.fromUriString(binanceDepthUrl)
                .queryParam("symbol", symbol)
                .queryParamIfPresent("limit", Optional.ofNullable(limit))
                .toUriString();

        HttpEntity<?> requestEntity = new HttpEntity<>(new HttpHeaders());
        var httpResponse = restTemplate.exchange(
                finalUrl,
                HttpMethod.GET,
                requestEntity,
                BinanceDepth.class
        );

        if (httpResponse.getStatusCode().is2xxSuccessful()) {
            return httpResponse.getBody();
        }
        throw new Exception("Binance Depth request failed");
    }
}