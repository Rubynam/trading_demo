package org.trading.presentation.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BestPriceResponse {

  private String symbol;
  private String bestBidPrice;
  private String bestAskPrice;
}
