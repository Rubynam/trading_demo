package org.trading.presentation.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.trading.constant.TransactionStatus;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeResponse {

  private String symbol;
  private String quantity;
  private String price;
  private String side;
  private TransactionStatus status;
}
