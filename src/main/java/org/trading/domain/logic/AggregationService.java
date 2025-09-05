package org.trading.domain.logic;

import java.util.List;
import org.trading.domain.aggregates.AggregationPrice;

public interface AggregationService {

  List<AggregationPrice> aggregate() throws Exception;
}
