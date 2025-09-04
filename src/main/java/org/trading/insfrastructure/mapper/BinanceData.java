package org.trading.insfrastructure.mapper;

import lombok.Getter;
import lombok.Setter;

/**
 {
 "symbol": "ETHBTC",
 "bidPrice": "0.03977000",
 "bidQty": "22.48500000",
 "askPrice": "0.03978000",
 "askQty": "9.79880000"
 },
 */
@Getter
@Setter
public class BinanceData {

  private String symbol;
  private String bidPrice;
  private String bidQty;
  private String askPrice;
  private String askQty;
}
