package org.trading.domain.logic;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.trading.domain.enumeration.TransactionStatus;
import org.trading.insfrastructure.entities.TradeTransaction;
import org.trading.domain.enumeration.TradeSide;

public interface TransactionService {

  void store(String username, String symbol, BigDecimal price, BigDecimal quantity, TradeSide side, TransactionStatus status) throws Exception;

  Page<TradeTransaction> findAllBy(String username, Pageable pageable);
}
