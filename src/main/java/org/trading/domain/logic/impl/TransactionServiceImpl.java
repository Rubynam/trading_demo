package org.trading.domain.logic.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.trading.domain.enumeration.TransactionStatus;
import org.trading.domain.logic.TransactionService;
import org.trading.insfrastructure.entities.TradeTransaction;
import org.trading.domain.enumeration.TradeSide;
import org.trading.insfrastructure.repository.TradeTransactionRepository;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

  private final TradeTransactionRepository tradeTransactionRepository;

  @Override
  public void store(String username, String symbol, BigDecimal price, BigDecimal quantity,
      TradeSide side, TransactionStatus status) throws Exception {
    TradeTransaction tradeTransaction = TradeTransaction.builder()
        .username(username)
        .symbol(symbol)
        .price(price)
        .quantity(quantity)
        .tradeType(side)
        .status(status.name())
        .timestamp(LocalDateTime.now())
        .build();

    tradeTransactionRepository.save(tradeTransaction);
  }

  @Override
  public Page<TradeTransaction> findAllBy(String username, Pageable pageable) {
    return tradeTransactionRepository.findAllByUsernameOrderByTimestampDesc(username,pageable);
  }
}
