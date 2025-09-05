package org.trading.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.trading.insfrastructure.enumeration.TradeSide;

@Getter
@Setter
public class TradeRequest {

  @NotBlank(message = "symbol is mandatory")
  private String symbol;
  @NotNull(message = "side is mandatory")
  private TradeSide side;
  @Positive(message = "quantity must be greater than 0")
  private double quantity;
  @NotNull(message = "username is mandatory")
  private String username;
  //based on the latest price, which means that user use market price
}
