package org.trading.domain.logic;

import org.trading.insfrastructure.mapper.BinanceTicker;

public interface TickerAggregationService {

    BinanceTicker craw(String symbol) throws Exception;

}