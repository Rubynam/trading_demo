package org.trading.domain.logic.impl;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.trading.domain.logic.TransactionService;
import org.trading.insfrastructure.entities.TradeTransaction;
import org.trading.insfrastructure.enumeration.TradeSide;
import org.trading.insfrastructure.repository.TradeTransactionRepository;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

  private final TradeTransactionRepository tradeTransactionRepository;

  @Override
  public boolean store(String username, String symbol, BigDecimal price, BigDecimal quantity,
      TradeSide side) throws Exception {
    TradeTransaction tradeTransaction = TradeTransaction.builder()
        .username(username)
        .symbol(symbol)
        .price(price)
        .quantity(quantity)
        .tradeType(side)
        .build();

    tradeTransactionRepository.save(tradeTransaction);
    return true;
  }
}
