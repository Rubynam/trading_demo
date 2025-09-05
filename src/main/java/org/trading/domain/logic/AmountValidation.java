package org.trading.domain.logic;

import java.math.BigDecimal;

public interface AmountValidation<I> {

  boolean validate(I input, BigDecimal amount);
}
