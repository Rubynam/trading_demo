package org.trading.domain.logic.impl;

import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.trading.domain.logic.AmountValidation;
import org.trading.insfrastructure.entities.UserWallet;

@Service
@Slf4j
public class UserBalanceValidation implements AmountValidation<UserWallet> {

  @Override
  public boolean validate(UserWallet input, BigDecimal amount) {
    return input.getBalance().compareTo(amount) >= 0;
  }
}
