package org.trading.domain.logic.impl;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.trading.common.PairWallet;
import org.trading.domain.logic.UserWalletService;
import org.trading.insfrastructure.enumeration.TradeSide;
import org.trading.presentation.request.TradeRequest;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionExecutionService {

  private final UserBalanceValidation userBalanceValidation;
  private final UserWalletService userWalletService;


  @Transactional(propagation = Propagation.REQUIRES_NEW,isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
  public boolean executeBalance(PairWallet pairWallet, TradeRequest input, BigDecimal price)
      throws Exception {
    BigDecimal amount = price.multiply(BigDecimal.valueOf(input.getQuantity()));
    final TradeSide side = input.getSide();

    if(!userBalanceValidation.validate(pairWallet,amount, input)){
      log.warn("Insufficient balance username={} amount {} action {} symbol {}",input.getUsername(),amount,input.getSide(),input.getSymbol());
      return false;
    }
    switch (side) {
      case BUY: {
        userWalletService.deduct(pairWallet.getQuoteWallet(), amount);
        userWalletService.add(pairWallet.getBaseWallet(), BigDecimal.valueOf(input.getQuantity()));
        break;
      }
      case SELL: {
        userWalletService.deduct(pairWallet.getBaseWallet(), BigDecimal.valueOf(input.getQuantity()));
        userWalletService.add(pairWallet.getQuoteWallet(), amount);
        break;
      }
      default:
        log.warn("Invalid side {}",side);
        throw new IllegalArgumentException("Invalid side");
    }

    return true;
  }
}
