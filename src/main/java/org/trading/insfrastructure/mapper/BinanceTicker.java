package org.trading.insfrastructure.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BinanceTicker(
        String symbol,
        @JsonProperty("priceChange")
        String priceChange,
        @JsonProperty("priceChangePercent")
        String priceChangePercent,
        @JsonProperty("weightedAvgPrice")
        String weightedAvgPrice,
        @JsonProperty("prevClosePrice")
        String prevClosePrice,
        @JsonProperty("lastPrice")
        String lastPrice,
        @JsonProperty("lastQty")
        String lastQty,
        @JsonProperty("bidPrice")
        String bidPrice,
        @JsonProperty("bidQty")
        String bidQty,
        @JsonProperty("askPrice")
        String askPrice,
        @JsonProperty("askQty")
        String askQty,
        @JsonProperty("openPrice")
        String openPrice,
        @JsonProperty("highPrice")
        String highPrice,
        @JsonProperty("lowPrice")
        String lowPrice,
        String volume,
        @JsonProperty("quoteVolume")
        String quoteVolume,
        @JsonProperty("openTime")
        Long openTime,
        @JsonProperty("closeTime")
        Long closeTime,
        @JsonProperty("firstId")
        Long firstId,
        @JsonProperty("lastId")
        Long lastId,
        Integer count
) {}