package org.trading.domain.aggregates;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AggregationPrice {

  private BigDecimal bidPrice;
  private BigDecimal askPrice;
  private String symbol;
}
