package org.trading.insfrastructure.mapper;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({
        "openTime", "open", "high", "low", "close", "volume",
        "closeTime", "quoteAssetVolume", "numberOfTrades",
        "takerBuyBaseAssetVolume", "takerBuyQuoteAssetVolume", "ignore"
})
public record BinanceKLine(
        Long openTime,
        String open, //
        String high,
        String low,
        String close,
        String volume,
        Long closeTime,
        String quoteAssetVolume,
        Integer numberOfTrades,
        String takerBuyBaseAssetVolume,
        String takerBuyQuoteAssetVolume,
        String ignore
) {}
