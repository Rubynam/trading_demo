package org.trading.domain.logic;

import org.trading.domain.model.KLineParameters;
import org.trading.insfrastructure.mapper.BinanceKLine;

import java.util.List;

public interface KLineAggregationService {

    List<BinanceKLine> craw(KLineParameters kLineParameters) throws Exception;

}
