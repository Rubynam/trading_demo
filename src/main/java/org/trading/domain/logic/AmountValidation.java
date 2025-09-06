package org.trading.domain.logic;

import java.math.BigDecimal;
import org.trading.presentation.request.TradeRequest;

public interface AmountValidation<I> {

  boolean validate(I input, BigDecimal amount, TradeRequest tradeSide);
}
