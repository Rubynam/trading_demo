package org.trading.domain.aggregates;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Domain model for 24hr ticker price change statistics
 * {
 *   "symbol": "BTCUSDT",
 *   "priceChange": "-1930.21000000",
 *   "priceChangePercent": "-2.734",
 *   "lastPrice": "68658.81000000",
 *   "bidPrice": "68658.80000000",
 *   "askPrice": "68658.81000000",
 *   "volume": "15609.80328000"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TickerData {

    private String symbol;
    private BigDecimal priceChange;
    private BigDecimal priceChangePercent;
    private BigDecimal weightedAvgPrice;
    private BigDecimal prevClosePrice;
    private BigDecimal lastPrice;
    private BigDecimal lastQty;
    private BigDecimal bidPrice;
    private BigDecimal bidQty;
    private BigDecimal askPrice;
    private BigDecimal askQty;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal volume;
    private BigDecimal quoteVolume;
    private Long openTime;
    private Long closeTime;
    private Long firstId;
    private Long lastId;
    private Integer count;

}