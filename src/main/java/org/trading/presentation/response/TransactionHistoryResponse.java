package org.trading.presentation.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionHistoryResponse {

  private String symbol;
  private String quantity;
  private String price;
  private String side;
  private LocalDateTime timestamp;
  private String status;
  private Long transactionId;
  private String username;
}
