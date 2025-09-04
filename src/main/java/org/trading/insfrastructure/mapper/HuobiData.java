package org.trading.insfrastructure.mapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HuobiData {
  private List<Tickers> data = new ArrayList<>(0);

  /**
   {
   "symbol": "sylousdt",
   "open": 0.0004623,
   "high": 0.0004691,
   "low": 0.000456,
   "close": 0.0004661,
   "amount": 155493612.9151785,
   "vol": 71869.51427875923,
   "count": 1928,
   "bid": 0.0004615,
   "bidSize": 46832.1758,
   "ask": 0.0004692,
   "askSize": 54119.3046
   },
   */
  @Getter
  @Setter
  public static class Tickers {
    private String symbol;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal amount;
    private BigDecimal vol;
    private int count;
    private BigDecimal bid;
    private BigDecimal bidSize;
    private BigDecimal ask;
    private BigDecimal askSize;
  }
}
