package org.trading.insfrastructure.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record BinanceDepth(
        @JsonProperty("lastUpdateId")
        Long lastUpdateId,
        List<List<String>> bids,
        List<List<String>> asks
) {
    /**
     * Helper record to represent a single order book level (price and quantity)
     */
    public record OrderBookLevel(
            String price,
            String quantity
    ) {
        public static OrderBookLevel fromList(List<String> data) {
            if (data == null || data.size() < 2) {
                throw new IllegalArgumentException("Invalid order book data");
            }
            return new OrderBookLevel(data.get(0), data.get(1));
        }
    }

    /**
     * Get parsed bid levels
     */
    public List<OrderBookLevel> getBidLevels() {
        return bids.stream()
                .map(OrderBookLevel::fromList)
                .toList();
    }

    /**
     * Get parsed ask levels
     */
    public List<OrderBookLevel> getAskLevels() {
        return asks.stream()
                .map(OrderBookLevel::fromList)
                .toList();
    }
}