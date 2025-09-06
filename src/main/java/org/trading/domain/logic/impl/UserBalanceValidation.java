package org.trading.domain.logic.impl;

import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.trading.domain.aggregates.PairWallet;
import org.trading.domain.logic.AmountValidation;
import org.trading.presentation.request.TradeRequest;

@Service
@Slf4j
public class UserBalanceValidation implements AmountValidation<PairWallet> {

  @Override
  public boolean validate(PairWallet pairWallet, BigDecimal amount, TradeRequest input) {
    final var tradeSide = input.getSide();
    return switch (tradeSide){
      case BUY -> pairWallet.getQuoteWallet().getBalance().compareTo(amount) >= 0;
      case SELL -> pairWallet.getBaseWallet().getBalance().compareTo(
          BigDecimal.valueOf(input.getQuantity())) >= 0;
    };
  }
}
