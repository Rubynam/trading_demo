package org.trading.domain.logic;

import org.trading.insfrastructure.mapper.BinanceDepth;

public interface DepthAggregationService {

    BinanceDepth craw(String symbol, Integer limit) throws Exception;

}